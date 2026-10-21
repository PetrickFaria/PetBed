# PetBed linked-pet behavior test matrix

Target runtime: Minecraft Java 1.19.2, Forge 43.5.0, Java 17.

The automated checks validate source/resource structure, Java compilation and that the built JAR includes its Mixin config, refmap and follow guard. They do not launch a Minecraft world. Every gameplay row below therefore remains a manual test until performed in-game.

| # | Scenario | Status |
| --- | --- | --- |
| 1 | Link a wolf and move its owner away | Manual pending |
| 2 | Link a cat and move its owner away | Manual pending |
| 3 | Link a parrot and move its owner away | Manual pending |
| 4 | Confirm linked pets do not follow their owner | Manual pending |
| 5 | Confirm linked pets do not teleport to their owner | Manual pending |
| 6 | Confirm natural autonomous roaming near the bed | Manual pending |
| 7 | Confirm the pet turns back before passing the 100-block boundary | Manual pending |
| 8 | Confirm the pet navigates back to the bed at night | Manual pending |
| 9 | Confirm it stays resting through the night | Manual pending |
| 10 | Confirm it resumes roaming at dawn, including after sleeping in a player bed | Manual pending |
| 11 | Unlink and confirm normal owner-follow behavior returns | Manual pending |
| 12 | Unlink and confirm Vanilla owner-follow teleport is available again | Manual pending |
| 13 | Save and reload a world while a pet is linked | Manual pending |
| 14 | Restart a dedicated server while pets are linked | Manual pending |
| 15 | Confirm unlinked animals still behave normally | Manual pending |

Additional manual coverage: owner disconnect/reconnect and dimension travel; chunk unload/reload; bed destruction; pet death; ownership rejection; duplicate link rejection; a pet already sitting before linking; multiple players and beds. Dedicated-server launch should also be checked to catch client-only class loading.
