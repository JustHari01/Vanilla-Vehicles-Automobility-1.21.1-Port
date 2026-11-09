# Vanilla Vehicles for Automobility (UNOFFICIAL)

Source repository: https://github.com/JustHari01/Vanilla-Vehicles-Automobility-1.21.1-Port

A NeoForge 1.21.1 Java port of the Blockworks **Vanilla Vehicles** Bedrock add-on, built on the Automobility project. This working source tree records a local test build; it is not an official Automobility or Blockworks release.

## What it adds

Eight rideable vehicles: hot-air balloon, logcar, mech, minecar, mob truck, plane, Prismer, and steamboat. The port also implements their vehicle-specific movement and flight, balloon lift, submarine effects, seats and mob cargo, storage and customization, repairable Tesla helmet, mech projectiles and boost, smoke, compressor, flags, wind vane, Mechanic trades, guide pages, and converted recipes.

## Install the local test build

- Minecraft 1.21.1 with NeoForge 21.1.249.
- GeckoLib 4.9.3 is required on both client and server.
- The combined Automobility JAR contains the base Automobility mod and this port. Remove any separate Automobility JAR before testing; do not install both copies together.
- Use a copied game instance and world. Keep matching files on the server and every client.

See [the local test notes](docs/LOCAL-TEST-README.txt) for the tested package and [the port status](docs/PORT-STATUS.md) for evidence and limits.

## Attribution and asset rights

Automobility source code and notices are included under the terms in [LICENSE](LICENSE), with the original project credited in [HARI-PORT-NOTICE.md](HARI-PORT-NOTICE.md). The vehicle models, textures, sounds, names, and source behavior data originate from Blockworks' Vanilla Vehicles add-on. The port maintainer has stated that the add-on author permitted porting and public distribution. That statement is recorded in [ASSET-RIGHTS.md](ASSET-RIGHTS.md); Blockworks assets remain Blockworks material and are not relicensed by Automobility's MIT license.

## Test status and known limits

The local record covers a 6,000-tick dedicated-server soak, a second-process vehicle reload, client rendering and controls for all eight vehicles, and an isolated production-server boot. It does not establish compatibility with the user's full modpack or exact Bedrock visual parity. Java dye/material behavior, PBR/emissive rendering, alpha compositing, and several animation controllers differ or remain incomplete. Sounds were converted from WAV to lossy Ogg Vorbis. See [PORT-STATUS.md](docs/PORT-STATUS.md) for the detailed record.

## AI assistance disclosure

AI tools assisted with analysis of the user-provided add-on, Java porting, documentation, and testing. The project maintainer reviewed and ran the local tests recorded here.
