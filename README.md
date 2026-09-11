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
- Use Reorientation while riding to turn any compatible airborne living mount
  and its complete passenger hierarchy through the same generic mount path.

When an owned gravity effect expires, the mod first restores DOWN in place. If
that is obstructed it may relocate locally by at most four blocks; if no safe
local placement exists, the previous frame remains temporarily while retirement
is retried. It never falls back to an old remote checkpoint.

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
  recommended companion. Its current alpha.3 can infuse Clinging or
  Reorientation into compatible boots and dyeable animal armor. Reorientation
  remains deliberately excluded from its villager-trade economy.
- **First Person** is optional. Clinging scopes its body-offset correction to
  Clinging-owned visual frames/transitions instead of becoming a global
  Gravity Changer patch. Alpha.8 CI exercises First Person 2.7.2 in a separate
  real-client lane.

Scale Brews is under active architectural development and is **not part of the
alpha.8 runtime/support target**. Clinging's mounted-gravity contract is generic:
a Tiny Mount is just a living root vehicle. Scale is responsible for making its
own flight/glide/pounce mechanics honor that root's gravity when Scale is ready.
Optional legacy Scale integration is discovered reflectively and disables itself
when the installed Scale API is absent or incompatible.

## Camera timing

The first client launch creates `config/clinging-reoriented-client.json`:

```json
{
  "cameraRotationSeconds": 1.0
}
```

Choose a decimal value from `0.05` to `10` seconds and restart the client. The
physical gravity change remains immediate. The configured duration applies only
to visual transitions initiated by Clinging/Reorientation, including their own
return to DOWN; unrelated Gravity Changer transitions retain upstream timing.
See [configuration](docs/CONFIGURATION.md).

## Project status

**0.1.0-alpha.8** is a documentation-sync prerelease of the alpha.7 hardening
implementation. It intentionally makes no gameplay behavior changes relative to
alpha.7; current main additionally removes the transitional compile-time dependency
on optional Scale Brews without changing Clinging's base gameplay contract.

The release validation covers **42 dedicated server GameTests**, 10 JUnit tests,
the real default client GameTest suites and a separate real-client First Person
2.7.2 lane, all without loading optional Scale Brews at runtime. The adversarial
coverage includes bounded retirement/retry, external mob-gravity ownership, atomic
passenger recovery and stale/replayed moving-surface references. Full-pack human
playtesting, dedicated multiplayer latency and long pet routes remain manual checks;
automated success is not presented as human gameplay QA.

## Build and contribute

Use Java 25 and the included Gradle wrapper. A connected clean checkout resolves
the audited required versions from Modrinth and builds without any Scale Brews JAR:

```powershell
.\gradlew.bat build runGameTest
.\gradlew.bat runClientGameTest
```

Production `compileClasspath` deliberately excludes Scale Brews, and the build has
a guard that fails if a Scale JAR leaks into it. CI proves the clean base build
first. A separate optional compatibility lane downloads the public Scale Brews
beta.5 JAR into `test-libs` and loads it only at runtime with `-PwithScaleBrews`;
the production artifact never requires or bundles that fixture. First Person uses
its own isolated test fixtures in the same way.

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
