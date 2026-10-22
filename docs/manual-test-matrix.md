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
