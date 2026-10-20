# PetBed

Independent Minecraft Forge mod project for Minecraft Java 1.19.2, Forge 43.5.0 and Java 17.

The first block, `petbed:pet_bed` (Portuguese display name: **Caminha dos Pets**), is a low wooden woven bed with a pale cushion, custom collision/selection shape, Creative inventory item and shaped recipe.

## Pet linking and behavior

- Right-click opens a server-validated management screen. It lists nearby (8-block radius), loaded pets owned by the player and lets the player link or unlink one.
- One pet may be linked to only one bed. The block entity stores the pet UUID and owner UUID; the pet's Forge persistent data stores the bed position and dimension. Linking requires the pet's ownership adapter to identify the player as its owner.
- Vanilla `TamableAnimal` species are supported, including tamed wolves, cats and parrots. Support for another mod's `Mob` ownership system can be registered with `PetBedPets.register(...)` and `PetOwnershipAdapter` during mod initialization. Non-Mob entities and systems without an adapter are intentionally ineligible.
- During the day a high-priority goal redirects a linked pet toward the bed once it reaches 90 blocks, leaving margin under the 100-block boundary. At night it navigates back at normal goal speed and rests when it reaches the cushion. It does not force-load chunks or teleport during normal behavior. An owner's explicit sit order pauses the goal for adapters that expose that state.
- This initial version uses the animal's idle pose while resting; it does not add species-specific sleep animations. A bed that is unloaded can delay return until its chunk is already loaded through normal world activity. Unreachable paths are retried with a 100-tick cooldown.
- Data is server-authoritative and saved with the block entity and pet entity. No world-wide entity scan runs each tick.

## Build

Requires Java 17 and Gradle 7.5.1. From this folder run:

```sh
gradle --no-daemon build
```

The JAR is written to `build/libs/PetBed-0.1.0.jar`. `.github/workflows/build.yml` runs offline resource checks, builds the Forge mod and uploads the JAR as a workflow artifact.

## Verification status

`python3 tools/validate_resources.py` checks JSON syntax, required files, model texture references, 16x16 PNG signatures, and the recipe output. These checks do not load Minecraft classes or prove runtime behavior.

| Scenario | Status |
| --- | --- |
| JSON/resource paths, PNG format and recipe output | Passed by `tools/validate_resources.py` |
| Forge registrations, actual model rendering and crafting in game | Manual validation pending |
| UI open/sync, ownership rejection, unlink and duplicate protection | Manual multiplayer validation pending |
| Day roaming, 100-block boundary, night return/rest and dawn wake | Manual gameplay validation pending |
| Save/reload, chunk unload, dimension change, bed break and pet death | Manual gameplay validation pending |
| Dedicated server and third-party ownership adapters | Manual validation pending |

The Forge compilation and all gameplay scenarios still require validation. No in-game tests are represented as passed.

## Known limitations

- Only living `Mob` entities can be supported by this adapter API.
- First-build ownership behavior is implemented for vanilla `TamableAnimal`; other ownership systems need an adapter supplied by their mod.
- Resting is implemented by pausing the pet's movement and zeroing its motion at arrival; it does not change global models, animations or pose rendering.
- Navigation respects ordinary pathfinding, doors and loaded chunks. It will retry a blocked/unavailable route periodically instead of forcing chunks or teleporting.
- A bed can show an unavailable pet while that entity is unloaded. The player can unlink it; unloaded state is not treated as death.
