# Clinging: Reoriented

The floor is wherever you decide it is.

**Clinging: Reoriented** turns the Clinging effect from Alex's Mobs into an airborne gravity ability for Minecraft 26.2 on Fabric. Leave your local floor, release **Space**, look toward another world-cardinal direction and press Space again. Clinging grants one voluntary airborne gravity decision; **Reorientation** removes that one-turn limit.

[![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-62B47A)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DDBD3B)](https://fabricmc.net/)
[![Build and test](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml/badge.svg)](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml)
[![GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

![Clinging turns east into the new down](docs/images/0000_clinging-east-gravity.png)

## The alpha.14 movement language

A voluntary turn changes **physical gravity immediately** but preserves the existing world-space velocity vector. Gravity changes acceleration, not momentum. Reversing gravity therefore brakes the old motion naturally, crosses zero speed, and only then accelerates the other way.

The local player's camera is independent from logical gravity. Free-flight turns retain the world frame that was actually being rendered, so chained Reorientation choices do not drag the view through every gravity basis. During sustained Gravity Fall the camera also gains **full-sphere vertical look**: pitch can pass through both poles and complete a 360-degree loop. On exit, the same viewing direction is re-expressed as an ordinary vanilla yaw/pitch pair without a camera snap.

When the real trajectory is about to meet a valid gravity-relative floor, Clinging reserves a shared **10-tick / 500 ms landing manoeuvre**. Prediction begins early enough for camera LAND and BODY_LANDING to use that window rather than compressing a long visual idea into the last few ticks. New gravity requests during `LANDING_COMMITTED` are discarded, never queued; invalidated support preserves the current visible frame instead of snapping backward.

## Gravity Fall body, air and steering

After **12 airborne ticks** under Clinging/Reorientation physics, sustained fall enters Gravity Fall presentation. Over a **6-tick blend**, the macroscopic body root follows actual world velocity. Near zero speed it retains the last reliable frame, preventing numerical flips during a gravity reversal.

Velocity remains the primary body axis, but the camera can pull the body once gaze leaves a **35-degree neck deadzone**, capped at **7.5 degrees per tick**. Misalignment with the airflow adds bounded posture drag: streamlined flight is untouched and a perfectly broadside body adds at most **1.3% extra drag per tick**.

Holding **W** during sustained Gravity Fall performs air-diving steering. It bends the **existing** velocity toward gaze by at most **6 degrees per tick**, with authority proportional to the positive dot product between gaze and motion. It preserves speed before aerodynamic drag, creates no thrust, and gives no steering authority when gaze is perpendicular to or behind the current velocity.

At speed >= **0.75 blocks/tick**, Gravity Fall reuses vanilla's Elytra airflow sound. It fades in over 10 ticks and then follows Elytra's speed-squared volume/high-speed pitch language. The loop stops immediately when Gravity Fall releases ownership or real Elytra flight begins.

## Safety, impact and mace

Clinging-controlled airborne world speed is server-capped at **3.92 blocks/tick**, preserving direction. The server also prevents advancement into unavailable chunk columns, temporarily holding capped momentum at the loaded frontier, and recovers safely from hard world/build-border breaches without turning the safety correction into impact damage.

Impact damage follows the **world-space velocity actually absorbed by collision**. A late gravity turn cannot erase a dangerous collision; physically braking early enough can reduce it; tangential travel contributes little or nothing; and a multi-axis collision is resolved once.

Mace smash height under directional gravity is measured as literal geometric distance travelled along the **current gravity direction** since that fall segment began. Changing gravity starts a fresh segment, so old-axis distance and gravity-strength scaling cannot leak into smash thresholds, damage or knockback.

## Fluids, water and climbables

Any intersection with a non-empty fluid volume is a context boundary. Water, lava and modded fluids suspend gravity-relative support, landing prediction/commitment and Gravity Fall presentation. A submerged seabed is therefore not treated as a Clinging floor and does not recharge the one-turn budget until the body is actually clear of fluid and genuinely supported.

Water keeps the deliberate **Space, release, Space within 250 ms** gravity-request gesture. While Clinging/Reorientation owns water movement, Space is world **+Y** and Shift is world **-Y**, independent of the current gravity basis.

World-vertical climbables also have an explicit policy: DOWN keeps vanilla ladders/vines/scaffolding; EAST/WEST/NORTH/SOUTH ignore climbable attachment/damping; UP mirrors vanilla Y climbing so climb impulse points toward world -Y and sliding/downward behaviour mirrors toward +Y.

## Landing surfaces are extensible

The public `LandingSurfaceProvider` / `LandingSurfaces` contract lets other mods expose bounded support/predicted contact with stable identity for revalidation. Vanilla collision geometry is the base provider. External providers cannot bypass Clinging authority, fluid context, collision preflight, input policy or camera ownership, and invalid results fail closed.

The API intentionally contains no Scale Brews types. A concrete Scale-specific adapter belongs outside the base public contract.

## Mounts and pets

Mounted Reorientation still turns a compatible airborne living root and its passenger hierarchy atomically. Pet replay still follows bounded owner breadcrumbs when the pet has its own compatible effect. Non-player entities keep Clinging's owned **180/240 ms tracked snap** presentation; the 500 ms landing manoeuvre and full-sphere camera are local-player Gravity Fall rules, not generic mob camera concepts.

Tracked transitions are fenced by entity UUID plus monotonic sequence and continue advancing while off-screen. Foreign Gravity Changer writes remain foreign and retain upstream presentation ownership.

## Install

Install the regular JAR on **both client and server** with:

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.5 or newer
- Fabric API 0.159.0+26.2 or newer
- Alex's Mobs Continued 2.1.9
- CodxLib 1.5.1 or newer
- Gravity Changer Unofficial Port 1.5.2-beta.5-mc26.2
- Cloth Config API

This is an **alpha**. Back up important worlds before updating and use matching versions on every multiplayer participant.

## Optional companions and compatibility

- **Alchemical Leather** remains optional and can supply Clinging/Reorientation through compatible equipment.
- **First Person 2.7.2** is tested with Not Enough Animations 1.12.4. Alpha.14 uses a blended camera/body pivot during Gravity Fall so the body stays intentionally visible while the macro root remains isolated from the real camera.
- **Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2** are exercised in a pinned client lane. They remain optional and keep ownership of internal limb/head/equipment animation.
- **Scale Brews beta.5** is exercised in isolated server/client compatibility lanes. Production compile/runtime does not require it, and no concrete Scale landing adapter is bundled.

## Project status

**0.1.0-alpha.14** is the Gravity Fall control/compatibility prerelease. The release gate covers build/JUnit, server GameTests, default client, First Person, optional Scale Brews server/client, pinned Fresh Animations/Player Extension, full-sphere camera holdouts and semantic screenshot validation. The prerelease is published only from the exact `main` commit that passes that complete matrix.

Automated assertions are evidence, not human gameplay acceptance. Dedicated multiplayer latency, motion comfort/readability and long full-pack sessions remain manual QA.

## Build and documentation

Use Java 25 and the included Gradle wrapper:

```powershell
.\gradlew.bat build runGameTest
.\gradlew.bat runClientGameTest
```

Production `compileClasspath` remains Scale-free. Optional compatibility fixtures are isolated test-only inputs.

- [Player guide](docs/GUIDE.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Compatibility](docs/COMPATIBILITY.md)
- [Configuration](docs/CONFIGURATION.md)
- [Validation](docs/VALIDATION.md)
- [Changelog](CHANGELOG.md)

[GPL-3.0-or-later](LICENSE). Third-party projects keep their own licenses and are not bundled; see [credits and notices](THIRD_PARTY_NOTICES.md).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
