# Clinging: Reoriented

The floor is wherever you decide it is.

**Clinging: Reoriented** turns the Clinging effect from Alex's Mobs into an
airborne gravity ability for Minecraft 26.2 on Fabric. Release **Space** after
leaving the ground, then press it again while airborne to fall toward the
nearest world-cardinal direction to your view.

[![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-62B47A)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DDBD3B)](https://fabricmc.net/)
[![Build and test](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml/badge.svg)](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml)
[![GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

![Clinging turns east into the new down](docs/images/0000_clinging-east-gravity.png)

## Two ways to turn

- **Clinging** grants one successful gravity turn per airborne stretch. Landing
  on your local floor restores the charge.
- **Reorientation** grants unlimited airborne turns. Brew it by adding a
  **shulker shell** to a Clinging potion. Extended, splash and lingering forms
  retain the ordinary brewing routes.

A valid turn preserves position and world momentum. It is rejected when the
rotated collision box would be obstructed, the chosen direction is unchanged,
or another mechanic owns the input. Holding Space never repeats a turn.

Usable Elytra always take priority: Space deploys the Elytra instead of changing
gravity, and no turns are accepted while gliding. Gravity Changer remains the
owner of acceleration, movement, jumping, collisions and camera rotation.

## Things to try

- Jump into open air, look toward a wall and press Space again.
- Land sideways, jump relative to your new floor and spend Clinging's restored
  charge.
- Brew Reorientation and cross a room without agreeing on which way is down.
- Give a tamed animal its own gravity effect and let it replay turns along the
  route where it follows you.
- Use Reorientation while riding to turn an airborne mount and its passengers
  together.

The full behavior of mounts, pets, beacons, recovery and effect expiry is in the
**[player guide](docs/GUIDE.md)**.

## Install

Install the regular JAR on **both the client and server** together with:

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.5 or newer
- Fabric API 0.159.0+26.2 or newer
- Alex's Mobs Continued 2.1.9
- CodxLib 1.5.1 or newer
- Gravity Changer Unofficial Port 1.5.2-beta.5-mc26.2
- Cloth Config API

Alex's Mobs is required because this mod deliberately builds on its Clinging
effect and potions. Gravity Changer is required because it supplies the actual
gravity physics and camera transformation.

This is an **alpha**. Back up important worlds before updating and use matching
versions on every multiplayer participant. See the
[validation report](docs/VALIDATION.md) for tested behavior and remaining QA.

## Recommended companions

- **[Alchemical Leather](https://github.com/R3Neer/alchemical-leather)** is the
  recommended companion. Infuse Clinging or Reorientation into leather boots
  so the effect lasts while they are equipped. Its future animal-armor release
  can also supply gravity effects to leather horse armor and wolf armor.
- **[Scale Brews](https://github.com/R3Neer/scale-brews)** is optional for extra
  chaos: combine changing gravity with growing, shrinking and size-dependent
  entity interactions. The tested integration uses Scale Brews beta.4's existing
  entity surfaces; the newer shared anatomical system is not released gameplay.
- **First Person** is optional. Its audited compatibility keeps the rendered
  body aligned with the rotated camera when used with Scale Visual Compat.

None of these companions is required for the core Clinging and Reorientation
experience.

## Camera timing

The first client launch creates `config/clinging-reoriented-client.json`:

```json
{
  "cameraRotationSeconds": 1.0
}
```

Choose a decimal value from `0.05` to `10` seconds and restart the client. The
physical gravity change remains immediate; this setting changes only the visual
transition. See [configuration](docs/CONFIGURATION.md).

## Project status

Alpha.6 passes 39 dedicated server GameTests, 10 JUnit tests and real client
GameTest suites. The public tree was revalidated on 2026-09-09 both with a clean
server build and in a real client GameTest environment without loading optional
Scale Brews. Earlier alpha.6 evidence also covers configurable camera timing and
First Person integration. Full-pack human playtesting, dedicated multiplayer
latency and long pet routes remain manual checks. Automated success is not
presented as human gameplay QA.

## Build and contribute

Use Java 25 and the included Gradle wrapper. A connected clean checkout resolves
the audited required versions from Modrinth:

```powershell
.\gradlew.bat build runGameTest -PwithoutScaleBrews
.\gradlew.bat runClientGameTest -PwithoutScaleBrews
```

For offline development against the exact installed binaries, use
`scripts/prepare-dependencies.ps1`; Scale Brews is copied only when its tested
beta.4 JAR is present. No dependency or test JAR is included in version control.

- [Player guide](docs/GUIDE.md)
- [Compatibility](docs/COMPATIBILITY.md)
- [Configuration](docs/CONFIGURATION.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Validation](docs/VALIDATION.md)
- [Changelog](CHANGELOG.md)

[GPL-3.0-or-later](LICENSE). Third-party projects keep their own licenses and are
not bundled; see [credits and notices](THIRD_PARTY_NOTICES.md).

Not an official Minecraft product. Not approved by or associated with Mojang or
Microsoft.
