VANILLA VEHICLES FOR AUTOMOBILITY - LOCAL TEST BUILD

Minecraft 1.21.1 / NeoForge 21.1.249
Automobility build: 0.5.0.h-vv1-localtest

Only try this in a separate, copied instance and copied world. The main jar
bundles Automobility and the Vanilla Vehicles port under the same automobility
mod id; it cannot be placed next to a second Automobility jar.

The package includes the matching GeckoLib 4.9.3 NeoForge dependency. Put both
jars in the copied instance's mods directory and give every connected client
and server the same two jars. Preserve the original files in the original
instance. Do not use an existing world as the only copy.

This is a local-test build. All source vehicle PNGs, geometry cubes/bones/UVs,
and the listed animation frames were compared with the supplied addon. Sound
files were converted from WAV to Ogg Vorbis. Java's dye/material and animation
rendering still differs from Bedrock; see PORT-STATUS.md for test evidence
and remaining gaps. The development server passed a 6,000-tick soak and a
separate-process vehicle reload test; the production JAR booted in an isolated
dedicated server and spawned a minecar and mech. GeckoLib logged a missing
refmap warning in that production run. The build has not been tested against
the entire installed modpack or cleared for public release.

Blockworks' original assets remain attributed to Blockworks. See ASSET-RIGHTS.md.
