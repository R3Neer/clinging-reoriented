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

- **Clinging** grants exactly one voluntary gravity turn per airborne stretch.
  Landing on your local floor restores the charge. Once spent, every further
  voluntary direction, including vanilla **DOWN**, waits for a real landing.
- **Reorientation** grants unlimited airborne turns. Brew it by adding a
  **shulker shell** to a Clinging potion. Extended, splash and lingering forms
  retain the ordinary brewing routes.

A valid turn preserves world momentum and the physical body's world-space center
when a feet-pivot rotation would otherwise clip the floor or wall being left behind.
It is still rejected when the rotated body is genuinely obstructed, the chosen
direction is unchanged, or another mechanic owns the input. Holding Space never
repeats a turn.

Usable Elytra always take priority: Space deploys the Elytra instead of changing
gravity, and no turns are accepted while gliding. A queued sprint-jump also keeps
Space when the player is sprinting downward and predicted to touch the current
local floor on the next simulation step, preventing a near-landing double tap from
being mistaken for Clinging/Reorientation.

## Gravity snap presentation

Clinging/Reorientation gravity is physical immediately, but the camera and body use
a short visual snap instead of Gravity Changer's generic canonical-frame
interpolation. Quarter turns settle in **0.18 s** and opposite half turns in
**0.24 s**, with a quadratic ease-out: still immediate in feel, but long enough to
see the direction of the rotation.

Target selection and retained heading are deliberately separate. The rendered
camera direction chooses the new gravity, while the player's navigation heading is
captured independently from yaw with pitch treated as zero. Looking straight up or
down just to select UP/DOWN therefore no longer destroys the direction the player
was travelling. A DOWN↔UP turn keeps that world heading when the camera is levelled
again.

Perpendicular changes rotate only around the single axis required to reach the new
floor. Rapid Reorientation changes continue from the frame currently on screen
rather than snapping back or queueing old rotations. Each successful gravity change
also starts a fresh vanilla fall-distance segment, so chaining legitimate
Reorientation turns does not accumulate one artificial mega-fall.

These timings are gameplay/presentation semantics and are not configurable.
Clinging creates no client JSON configuration file; old
`config/clinging-reoriented-client.json` files from earlier alphas are ignored.
Unrelated Gravity Changer changes keep Gravity Changer's own animation behavior.

## Things to try

- Jump into open air, look toward a wall and press Space again.
- Land sideways, jump relative to your new floor and spend Clinging's restored
  charge.
- Spend Clinging's turn and verify that even DOWN now waits for a real landing.
- With gravity DOWN, face north, glance straight up to choose UP, then level the
  camera again: your world heading should still be north.
- Chain rapid Reorientation turns and watch each snap continue from the current
  displayed frame without accumulating fall damage from earlier segments.
- Sprint-jump repeatedly across flat ground without accidental gravity changes.
- Die and respawn after several turns; the first new turn must still use the same
  Clinging snap presentation rather than Gravity Changer's old interpolation.
- Give a tamed animal its own gravity effect and let it replay turns along the
  route where it follows you.
- Use Reorientation while riding to turn any compatible airborne living mount
  and its complete passenger hierarchy through the same generic mount path.

When an owned gravity effect expires, the mod first restores DOWN in place. If
that is obstructed it may relocate locally by at most four blocks; if no safe
local placement exists, the previous frame remains temporarily while retirement
is retried. This forced retirement is cleanup, not a voluntary Clinging turn, and
is not blocked by a spent airborne charge.

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
gravity state, coordinate transforms, movement and collision physics.

This is an **alpha**. Back up important worlds before updating and use matching
versions on every multiplayer participant. See the
[validation report](docs/VALIDATION.md) for tested behavior and remaining QA.

## Recommended companions

- **[Alchemical Leather](https://github.com/R3Neer/alchemical-leather)** is the
  recommended companion. Its current alpha.3 can infuse Clinging or
  Reorientation into compatible boots and dyeable animal armor. Reorientation
  remains deliberately excluded from its villager-trade economy.
- **First Person** is optional. Clinging transforms First Person's body offset only
  while Clinging owns the visual gravity frame/transition. CI exercises First
  Person 2.7.2 in a separate real-client lane.

Scale Brews is under active architectural development and is **not part of the
current runtime/support target**. Clinging's mounted-gravity contract is generic:
a Tiny Mount is just a living root vehicle. Scale is responsible for making its
own flight/glide/pounce mechanics honor that root's gravity when Scale is ready.
Optional legacy Scale integration is discovered reflectively and disables itself
when the installed Scale API is absent or incompatible.

## Project status

**0.1.0-alpha.11** is the current development version. It keeps alpha.10's minimal
snap-style gravity transport while separating target selection from navigation
heading, preserving heading through vertical DOWN↔UP selection, making visual
transition epochs survive death/respawn, resetting fall distance on actual
Clinging-owned gravity changes and slowing the snap just enough to make its axis
readable. It retains the sprint-jump intent guard, strict one-turn Clinging budget,
center-aligned clearance fallback and earlier ownership/recovery hardening.

CI covers dedicated server GameTests, JUnit geometry tests, the real default client
suites, a separate real-client First Person 2.7.2 lane and optional Scale Brews
runtime compatibility lanes. Full-pack human playtesting, dedicated multiplayer
latency and long pet routes remain manual checks; automated success is not presented
as human gameplay QA.

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
