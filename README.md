# PetBed

Independent Minecraft Forge mod project for Minecraft Java 1.19.2, Forge 43.5.0 and Java 17.

The first block, `petbed:pet_bed` (Portuguese display name: **Caminha dos Pets**), is a low wooden woven bed with a pale cushion, custom collision/selection shape, Creative inventory item and shaped recipe.

## Pet linking and behavior

- Right-click opens a server-validated management screen. It lists nearby (8-block radius), loaded pets owned by the player and lets the player link or unlink one.
- One pet may be linked to only one bed. The block entity stores the pet UUID and owner UUID; the pet's Forge persistent data stores the bed position and dimension. Linking requires the pet's ownership adapter to identify the player as its owner.
- Vanilla `TamableAnimal` species are supported, including tamed wolves, cats and parrots. Support for another mod's `Mob` ownership system can be registered with `PetBedPets.register(...)` and `PetOwnershipAdapter` during mod initialization. Non-Mob entities and systems without an adapter are intentionally ineligible.
- While the pet has a valid bed link, a conditional Mixin pauses only Vanilla's `FollowOwnerGoal` (`canUse`, continuation and tick); the goal is not removed or duplicated. This prevents the owner's follow path and its built-in teleport from running. Removing the link removes the guard immediately, so the same Vanilla goal resumes without reconstructing the entity or its AI.
- The pet keeps its other species goals. During the day it explores using its natural goals; the PetBed return goal takes movement priority at 90 blocks, leaving a margin before the 100-block limit. If the route cannot be found, the goal retries after 100 ticks rather than teleporting or loading chunks. At night it walks back using ordinary navigation and rests at the cushion. Only the original sit order is temporarily cleared on linking and restored on unlink; this does not disable AI.
- This initial version uses the animal's idle pose while resting; it does not add species-specific sleep animations. A bed that is unloaded can delay return until its chunk is already loaded through normal world activity. Unreachable paths are retried with a 100-tick cooldown.
- Data is server-authoritative and saved with the block entity and pet entity. No world-wide entity scan runs each tick.

## Build

Requires Java 17 and Gradle 7.5.1. From this folder run:

```sh
gradle --no-daemon build
```

The JAR is written to `build/libs/PetBed-0.1.0.jar`. `.github/workflows/build.yml` runs offline resource checks, builds the Forge mod and uploads the JAR as a workflow artifact.

## Verification status

`python3 tools/validate_resources.py` checks JSON syntax, required files, model texture references, 16x16 PNG signatures, the recipe, and the follow-guard configuration/source. The workflow also compiles the Forge mod and checks that the guard is packaged in the JAR. These checks do not prove runtime behavior.

See [`docs/manual-test-matrix.md`](docs/manual-test-matrix.md) for the fifteen requested gameplay scenarios and the additional cases still awaiting an in-game run.

| Scenario | Status |
| --- | --- |
| Guard source/config and resources, recipe, texture/model paths | Automated static checks |
| Mixin compilation and guard files in built JAR | GitHub Actions build checks |
| Link wolf/cat/parrot, owner travel, no follow/teleport, autonomous roaming, 100-block boundary | Manual in-game validation pending |
| Night return/rest, dawn wake, sitting before link and restoration after unlink | Manual in-game validation pending |
| Unlink restores Vanilla follow and teleport | Manual in-game validation pending |
| Save/reload, chunk unload/reload, server restart, dimension change, bed break, pet death | Manual single-player and dedicated-server validation pending |
| Other players' pets rejected; duplicate links and unrelated untamed animals unchanged | Manual multiplayer validation pending |

The Forge compilation and all gameplay scenarios still require validation. No in-game tests are represented as passed.

## Known limitations

- Only living `Mob` entities can be supported by this adapter API.
- First-build ownership behavior is implemented for vanilla `TamableAnimal`; other ownership systems need an adapter supplied by their mod.
- Resting is implemented by pausing the pet's movement and zeroing its motion at arrival; it does not change global models, animations or pose rendering.
- Navigation respects ordinary pathfinding, doors and loaded chunks. It will retry a blocked/unavailable route periodically instead of forcing chunks or teleporting.
- A bed can show an unavailable pet while that entity is unloaded. The player can unlink it; unloaded state is not treated as death.
