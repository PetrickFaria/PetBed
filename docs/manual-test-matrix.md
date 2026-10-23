# PetBed linked-pet behavior test matrix

Target runtime: Minecraft Java 1.19.2, Forge 43.5.0, Java 17.

CI validates source/resource structure, Java compilation and that the built JAR includes its Mixin config, refmap and follow guard. CI does not launch a Minecraft world. Every gameplay row below remains a manual test until performed in-game.

| # | Scenario | Status |
| --- | --- | --- |
| 1 | Link a wolf and move its owner away | Manual pending |
| 2 | Link a cat and move its owner away | Manual pending |
| 3 | Link a parrot and move its owner away | Manual pending |
| 4 | Confirm linked pets do not follow their owner | Manual pending |
| 5 | Confirm linked pets do not teleport to their owner | Manual pending |
| 6 | Confirm natural autonomous roaming near the bed | Manual pending |
| 7 | Confirm the default square is 50×50 and all five area choices can be selected | Manual pending |
| 8 | Confirm each of the five area sizes persists independently per bed | Manual pending |
| 9 | Confirm pet stays inside 15×15 bounds | Manual pending |
| 10 | Confirm pet stays inside 30×30 bounds | Manual pending |
| 11 | Confirm pet stays inside 45×45 bounds | Manual pending |
| 12 | Confirm pet stays inside 50×50 bounds | Manual pending |
| 13 | Confirm pet stays inside 75×75 bounds | Manual pending |
| 14 | Confirm boundary return uses natural navigation | Manual pending |
| 15 | Confirm distant loaded pet can be linked without proximity checks | Manual pending |
| 16 | Confirm unloaded pet is reported unavailable and no chunks are force loaded | Manual pending |
| 17 | Confirm linked pet posture (sitting/standing) appears in bed UI | Manual pending |
| 18 | Confirm only the owner opens the pet control screen | Manual pending |
| 19 | Select Livre and confirm autonomous roaming | Manual pending |
| 20 | Select Sentar and confirm pet stays put during daytime | Manual pending |
| 21 | Select Seguir and confirm natural owner following within bed bounds | Manual pending |
| 22 | Confirm pet never teleports outside its configured area in Seguir mode | Manual pending |
| 23 | Confirm night return takes priority over each manual mode | Manual pending |
| 24 | Confirm manual Sentar pet walks to bed at night, then remains there after dawn | Manual pending |
| 25 | Confirm Livre and Seguir resume their daytime mode at dawn | Manual pending |
| 26 | Confirm pet is centered and faces the front of the oriented bed without jitter | Manual pending |
| 27 | Unlink and confirm owner-follow goal resumes | Manual pending |
| 28 | Unlink and confirm Vanilla follow teleport is available again | Manual pending |
| 29 | Save/reload world and verify mode, area, posture, and link persistence | Manual pending |
| 30 | Restart dedicated server and verify multiplayer controls and persistence | Manual pending |

Also manually verify owner disconnect/reconnect and dimension travel, chunk unload/reload, bed destruction, pet death, ownership/duplicate rejection, prior sit state restoration, multiple players/beds, and normal behavior for unlinked animals. Dedicated-server launch should be checked to catch client-only class loading.

## Regression checks: bed rest and obstacle navigation

These cases need an in-game Forge world; the source checks and build do not simulate entity physics or terrain pathfinding.

| # | Scenario | Status |
| --- | --- | --- |
| R1 | Approach the bed from its front | Manual pending |
| R2 | Approach the bed from behind | Manual pending |
| R3 | Approach the bed from each side | Manual pending |
| R4 | Rest with bed facing north | Manual pending |
| R5 | Rest with bed facing south | Manual pending |
| R6 | Rest with bed facing east | Manual pending |
| R7 | Rest with bed facing west | Manual pending |
| R8 | Player attempts to push a sleeping pet | Manual pending |
| R9 | Another mob attempts to push a sleeping pet | Manual pending |
| R10 | Observe center position and orientation over several in-game minutes | Manual pending |
| R11 | Change mode/unlink and confirm movement and entity collision return | Manual pending |
| R12 | Save/reload while resting and confirm position, heading, and push protection | Manual pending |
| R13 | Return with an unobstructed route | Manual pending |
| R14 | Return with a solid obstacle between pet and bed | Manual pending |
| R15 | Route around a wall with a lateral opening | Manual pending |
| R16 | Reach a bed behind an open doorway | Manual pending |
| R17 | Change the route obstacle while the pet is walking | Manual pending |
| R18 | Temporarily block the current path and confirm a controlled recalculation | Manual pending |
| R19 | Make the bed unreachable; confirm retries are spaced and AI remains responsive | Manual pending |
| R20 | Confirm final alignment occurs only after reaching an adjacent path waypoint | Manual pending |
| R21 | Confirm night return works with each configured movement area | Manual pending |
| R22 | Confirm no return-path teleport occurs | Manual pending |


## Regression checks: night retry and arrival validation

These tests require a live Forge 1.19.2 world. CI source checks/build do not execute entity navigation or collision physics.

| # | Scenario | Status |
| --- | --- | --- |
| N1 | At night, a linked pet in FREE mode starts a reachable return route | Manual pending |
| N2 | At night, a linked pet in SIT mode is released from sitting and starts route | Manual pending |
| N3 | At night, a linked pet in FOLLOW mode stops following and starts route | Manual pending |
| N4 | Pet with a daytime retry delay starts night return immediately | Manual pending |
| N5 | Pet with stale resting marker away from bed clears marker and walks | Manual pending |
| N6 | Wall with lateral passage: pet follows an alternate route and actually arrives | Manual pending |
| N7 | Multi-turn obstacle course: pet makes progress and reaches the bed | Manual pending |
| N8 | Unreachable bed: no wall crossing; route retries remain rate-limited | Manual pending |
| N9 | Arrival tolerance: pet rests only after reaching a nearby waypoint | Manual pending |
| N10 | Blocked arrival: destination collision prevents final alignment | Manual pending |
| N11 | Reload pet during night: route is restarted when entity/chunks load | Manual pending |
| N12 | Wolf, cat, and parrot: validate species-specific navigator behavior | Manual pending |

No in-game GameTest runner is currently configured. These scenarios are not reported as passed until exercised in Minecraft/Forge.

## Regression checks: one-block accommodation and seated night return

These tests require a live Forge world; CI does not simulate entity movement or nighttime AI.

| # | Scenario | Status |
| --- | --- | --- |
| A1 | Pet reaches an adjacent floor cell within one block and is aligned once | Manual pending |
| A2 | Pet outside one-block radius continues normal path navigation | Manual pending |
| A3 | Pet near the bed on another floor is not aligned through the floor | Manual pending |
| A4 | Nearby pet separated by a wall/blocked destination is not pulled through it | Manual pending |
| A5 | Pet stops navigating and stays stable after accommodation | Manual pending |
| S1 | Link an already seated pet during daytime; verify night return | Manual pending |
| S2 | Link an already seated pet during nighttime; verify immediate return | Manual pending |
| S3 | Mode SIT is temporarily released at night and persists after dawn | Manual pending |
| S4 | Night retry delay does not leave sitting/rest flags blocking movement | Manual pending |
| S5 | Save/reload a seated linked pet during night; verify return resumes | Manual pending |
| S6 | Repeat arrival and sit-return checks with wolf, cat, and parrot | Manual pending |

Historical comparison: the earlier goal used a simple near-bed distance check and cleared the Vanilla sit order during night, but did not verify route progress or destination collision. The current implementation preserves the night sit release and adds bounded, same-level final accommodation after normal navigation. No historical source was restored wholesale.