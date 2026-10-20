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
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit("Missing required resources: " + ", ".join(missing))

mod_toml = (RES / "META-INF/mods.toml").read_text(encoding="utf-8")
if 'modId="${mod_id}"' not in mod_toml or 'versionRange="[1.19.2,1.20)"' not in mod_toml:
    raise SystemExit("mods.toml is missing PetBed's expanded mod ID or target Minecraft range")

blockstate = json.loads((RES / "assets/petbed/blockstates/pet_bed.json").read_text())
model_id = blockstate["variants"][""]["model"]
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

print("PetBed resource checks passed (JSON, registrations metadata, model texture paths, PNGs, recipe).")
