# Calisthenics (Fabric, Minecraft 1.21.11)

Calisthenics emotes that you AND everyone nearby can see.
Emotes (UI slots 1-8): Handstand, Planche, Elbow Lever, Maltese, Push-up, Handstand Push-ups, Backflip, Flip.

## Use in game
- `O` opens the emote UI: a 3x3 grid with the 8 emotes around your own head in the middle.
  Click an emote or press `1`-`8`. Click your head (or press `H`) to stop. `O` again closes the UI.
- Held emotes stay until you stop, move, jump or sneak. Backflip and Flip play once by themselves.
- Press `F5` for third person to see yourself.
- Multiplayer: the **server and every friend** need this mod AND the Player Animation Library mod
  (Modrinth / CurseForge: "Player Animation Library" by ZigyTheBird, build for 1.21.11) plus Fabric API.

## Build on GitHub (no installs needed)
1. Create a new repository on github.com and upload all files of this folder (including the hidden `.github` folder).
2. Open the repo's **Actions** tab. The "Build mod jar" workflow runs by itself on every upload
   (or press **Run workflow**).
3. When it turns green, open the run and download **calisthenics-mod-jar** at the bottom. Unzip it: that is your mod jar.
   If it turns red, open the failed step and copy the error text.

## Build on your own PC
Needs Java 21 and Gradle 8.14+ (or open the folder in IntelliJ IDEA).

    gradle build          # jar in build/libs/calisthenics-1.0.0.jar
    gradle runClient      # dev client for testing

Versions are in `gradle.properties` (Fabric Loader 0.18.1, Fabric API 0.141.2+1.21.11, PAL 1.1.11+mc.1.21.11).
This uses Mojang mappings (Yarn stops at 1.21.11).

## IMPORTANT: not compiled or tested yet
This was written without being able to compile (the build sites were blocked), so expect small fixes:
1. **Imports of the animation library** in `CalisthenicsClient.java` were written from its docs, not checked
   against the real jar. If a line is red, use auto-import (Alt+Enter) to fix the package.
2. **Animation ids**: the docs say the id's path is the animation's name inside the json. Each file here holds
   one animation named like the emote (e.g. `calisthenics:handstand`). If an emote does nothing, check the log.
3. **Poses** have not been seen in game. See below.

## Tuning the poses
1. Edit numbers in `tools/generate_animations.py` (explained at the top).
2. `python3 tools/generate_animations.py`
3. In game press `F3 + T` (reload resources).

Common fixes:
- Everything pitches the wrong way (flips go backwards, handstand tilts forward): set `PITCH_SIGN = 1`.
- Body floats/sinks the wrong way: set `Y_SIGN = -1`.
- Body too high/low in handstand/planche: change the `y` of that pose (pixels).
- Flip direction swapped: swap `+1` / `-1` in the `flip(...)` calls.

## Known gaps
- No elbow bending yet: push-ups move the whole body up and down instead of bending the arms, and the
  elbow lever has no folded forearms. This can be added with the library's `bend` value once the base poses look right.
- Only 1.21.11 is supported. Other 1.21.x versions each need their own build (internal names change between versions,
  and the animation library had no builds for 1.21.2-1.21.6 and 1.21.7-1.21.10 is marked unsupported).
