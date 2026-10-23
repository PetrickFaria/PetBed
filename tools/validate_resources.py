#!/usr/bin/env python3
"""Fast offline checks for PetBed's resource metadata; does not replace a Forge build."""
import json
from pathlib import Path
import re
import struct

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"

for path in sorted(RES.rglob("*.json")):
    json.loads(path.read_text(encoding="utf-8"))

required = [
    RES / "META-INF/mods.toml",
    RES / "assets/petbed/blockstates/pet_bed.json",
    RES / "assets/petbed/models/block/pet_bed.json",
    RES / "assets/petbed/models/item/pet_bed.json",
    RES / "assets/petbed/textures/block/woven_wood.png",
    RES / "assets/petbed/textures/block/pad.png",
    RES / "data/petbed/recipes/pet_bed.json",
    RES / "data/petbed/loot_tables/blocks/pet_bed.json",
    RES / "petbed.mixins.json",
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit("Missing required resources: " + ", ".join(missing))

mod_toml = (RES / "META-INF/mods.toml").read_text(encoding="utf-8")
if 'modId="${mod_id}"' not in mod_toml or 'versionRange="[1.19.2,1.20)"' not in mod_toml:
    raise SystemExit("mods.toml is missing PetBed's expanded mod ID or target Minecraft range")

blockstate = json.loads((RES / "assets/petbed/blockstates/pet_bed.json").read_text())
variants = blockstate.get("variants", {})
expected_facing = {f"facing={direction}" for direction in ("north", "south", "east", "west")}
if set(variants) != expected_facing:
    raise SystemExit("PetBed blockstate must define all four facing variants")
for variant in variants.values():
    model_id = variant["model"]
    model_path = RES / "assets" / model_id.split(":")[0] / "models" / (model_id.split(":")[1] + ".json")
    model = json.loads(model_path.read_text())
    for value in model.get("textures", {}).values():
        if value.startswith("#"):
            continue
        ns, texture = value.split(":", 1)
        texture_path = RES / "assets" / ns / "textures" / (texture + ".png")
        if not texture_path.is_file():
            raise SystemExit(f"Model texture not found: {texture_path.relative_to(ROOT)}")

for path in required:
    if path.suffix == ".png":
        data = path.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n":
            raise SystemExit(f"Invalid PNG signature: {path.relative_to(ROOT)}")
        width, height = struct.unpack(">II", data[16:24])
        if width != 16 or height != 16:
            raise SystemExit(f"Expected 16x16 pixel art texture: {path.relative_to(ROOT)}")

recipe = json.loads((RES / "data/petbed/recipes/pet_bed.json").read_text())
if recipe.get("result", {}).get("item") != "petbed:pet_bed":
    raise SystemExit("PetBed crafting recipe does not produce petbed:pet_bed")

mixin_config = json.loads((RES / "petbed.mixins.json").read_text())
if "com.petrick.petbed.mixin" != mixin_config.get("package") or not {"FollowOwnerGoalMixin", "RestingPetCollisionMixin"}.issubset(set(mixin_config.get("mixins", []))):
    raise SystemExit("Mixin config does not register the linked-pet follow and resting collision guards")
follow_mixin = ROOT / "src/main/java/com/petrick/petbed/mixin/FollowOwnerGoalMixin.java"
if not follow_mixin.is_file():
    raise SystemExit("FollowOwnerGoalMixin source is missing")
follow_source = follow_mixin.read_text(encoding="utf-8")
for method in ("canUse", "canContinueToUse", "tick"):
    if f'method = "{method}"' not in follow_source:
        raise SystemExit(f"FollowOwnerGoalMixin is missing its {method} guard")
if "setNoAi" in follow_source:
    raise SystemExit("The linked-pet guard must not disable the animal's full AI")

rest_goal = ROOT / "src/main/java/com/petrick/petbed/PetRestGoal.java"
collision_mixin = ROOT / "src/main/java/com/petrick/petbed/mixin/RestingPetCollisionMixin.java"
if not rest_goal.is_file() or not collision_mixin.is_file():
    raise SystemExit("Pet rest pathfinding or temporary collision guard source is missing")
rest_source = rest_goal.read_text(encoding="utf-8")
for marker in ("createPath(candidate, 0)", "path.canReach()", "moveTo(candidate.path(), 1.0D)", "atApproach()", "STUCK_AFTER_TICKS", "RESTING_KEY"):
    if marker not in rest_source:
        raise SystemExit(f"Pet rest behavior is missing required path/stability check: {marker}")
if "noPhysics" in rest_source or "teleportTo" in rest_source:
    raise SystemExit("Pet return/rest logic must retain block collision and avoid teleporting")
for marker in ("if (retryDelay > 0 && !night)", "canAlignFromCurrentPosition()", "navigate(failedApproach)", "isNearApproach()", "level.noCollision(pet, destination)", "if (retryDelay > 0) { retryDelay--; return; }", "blockDx + blockDz != 1", "!onRestSurface() && PetBedPets.isOrderedToSit(pet)"):
    if marker not in rest_source:
        raise SystemExit(f"Pet night return is missing its state/path/arrival guard: {marker}")
collision_source = collision_mixin.read_text(encoding="utf-8")
if 'method = "isPushable"' not in collision_source or "isRestingLinkedPet" not in collision_source:
    raise SystemExit("Entity collision guard must be conditional on linked-pet rest state")

area_source = (ROOT / "src/main/java/com/petrick/petbed/block/PetBedArea.java").read_text(encoding="utf-8")
for size in ("15", "30", "45", "50", "75"):
    if size not in area_source:
        raise SystemExit(f"Movement area option {size} is missing")
if "DEFAULT_SIZE = 50" not in area_source:
    raise SystemExit("New beds must use a valid 50×50 default area")
network_source = (ROOT / "src/main/java/com/petrick/petbed/network/PetBedNetwork.java").read_text(encoding="utf-8")
for marker in ("ControlMessage", "setAreaSize", "setMode"):
    if marker not in network_source:
        raise SystemExit(f"Server validated network operation {marker} is missing")
for path in (ROOT / "src/main/java/com/petrick/petbed/client/PetControlScreen.java", ROOT / "src/main/java/com/petrick/petbed/menu/PetControlMenu.java"):
    if not path.is_file():
        raise SystemExit(f"Pet control interface source missing: {path.name}")
link_source = (ROOT / "src/main/java/com/petrick/petbed/block/PetLinkService.java").read_text(encoding="utf-8")
if "PetBedPets.setOrderedToSit(pet, false)" not in link_source or "PetBedPets.setOrderedToSit(entity, restoreSitting)" not in link_source:
    raise SystemExit("Linking must release sit while linked and restore the original sit order on unlink")

print("PetBed static checks passed (resources, four facings, recipe, mixin guard, area choices, mode controls).")
