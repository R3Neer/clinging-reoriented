# Clinging: Reoriented

The floor is wherever you decide it is.

**Clinging: Reoriented** turns the Clinging effect from Alex's Mobs into an airborne gravity ability for Minecraft 26.2 on Fabric. Leave your local floor, release **Space**, look toward another world-cardinal direction and press Space again. Clinging grants one voluntary airborne gravity decision; **Reorientation** removes that one-turn limit.

[![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-62B47A)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DDBD3B)](https://fabricmc.net/)
[![Build and test](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml/badge.svg)](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml)
[![GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

![Clinging turns east into the new down](docs/images/0000_clinging-east-gravity.png)

## The alpha.15 movement language

Alpha.15 keeps alpha.14's player movement model intact. A voluntary turn changes **physical gravity immediately** while preserving the existing world-space velocity vector. During sustained Gravity Fall the local camera can look through a full vertical 360 degrees, the body follows velocity with bounded look-follow/aerodynamic drag, and holding **W** redirects existing momentum toward gaze without creating free thrust.

Landing uses the shared **10-tick / 500 ms** camera/body manoeuvre. At speed >= **0.75 blocks/tick**, Gravity Fall reuses vanilla Elytra airflow audio with a 10-tick fade-in and speed-sensitive volume/pitch. The server caps eligible Clinging flight to **3.92 blocks/tick**, fences unloaded/hard world boundaries and preserves alpha.13's absorbed-collision impact model plus directional mace geometry.

Water movement is world-vertical while owned, fluid volumes suspend landing/support semantics, and climbables are DOWN-vanilla / lateral-ignored / UP-mirrored. First Person keeps a real independent camera while the Gravity Fall body uses a blended pivot to remain visible without clipping.

## Gravity Fall body, air and steering

After **12 airborne ticks** under Clinging/Reorientation physics, sustained fall enters Gravity Fall presentation. Over a **6-tick blend**, the macroscopic body root follows actual world velocity. Near zero speed it retains the last reliable frame, preventing numerical flips during a gravity reversal.

Velocity remains the primary body axis, but the camera can pull the body once gaze leaves a **35-degree neck deadzone**, capped at **7.5 degrees per tick**. Misalignment with airflow adds bounded posture drag: streamlined flight is untouched and a perfectly broadside body adds at most **1.3% extra drag per tick**.

Holding **W** bends existing velocity toward gaze by at most **6 degrees per tick**, with authority proportional to the positive dot product between gaze and motion. It preserves speed before aerodynamic drag, creates no thrust and has no authority for perpendicular/backward gaze.

## Mounts and gravity-following pets

Mounted Reorientation turns a compatible airborne living root and its passenger hierarchy atomically.

A tame pet with its **own** Clinging/Reorientation effect now follows gravity changes as a real route instead of remotely rotating wherever it happens to stand. Each owner turn records a bounded breadcrumb. If a valid step is pending, the pet may temporarily acquire vanilla `FollowOwnerGoal` even while the owner is inside vanilla's normal ten-block follow-start dead zone.

The target is not the owner's impossible airborne 3D point. Clinging projects that breadcrumb onto the pet's **current gravity-relative movement plane** and asks the pet's ordinary navigation to reach the projection. Arrival uses the follow goal's own bounded stop distance. Only then does the pet replay the recorded gravity turn.

After the turn, the pet releases navigation while unsupported and falls under real gravity. Once it finds support in the new frame it can continue toward the next queued breadcrumb. If the rotation would intersect the old support at the current feet position, replay may use one center-aligned, collision-preflighted internal placement; that internal relocation preserves the remaining breadcrumb queue.

Gravity Changer replaces `PathNavigation` when gravity changes, so the integration refreshes `FollowOwnerGoal`'s cached navigation throughout the goal lifecycle. Sitting pauses pursuit without consuming the pending route; standing can resume it. An external teleport invalidates the old route. Effect-free pets keep vanilla following, stale/wrong-dimension breadcrumbs are ignored, and foreign gravity ownership remains foreign.

Non-player entities keep Clinging's shorter **180/240 ms tracked SNAP** presentation; the player's 500 ms landing manoeuvre and full-sphere camera are not copied onto pets.

## World context and combat

Any non-empty fluid intersection suspends Clinging support, landing prediction/commitment and Gravity Fall presentation, including modded fluids. A submerged seabed is not a Clinging floor. In water, Space is world **+Y** and Shift world **-Y** while owned, independent of gravity.

World-vertical climbables use an explicit policy: DOWN keeps vanilla behaviour, lateral gravity ignores them, and UP mirrors vertical climbing. Mace smash height is literal geometric distance along the current gravity direction since the current fall segment began; changing gravity starts a fresh mace segment.

## Landing surfaces are extensible

The public `LandingSurfaceProvider` / `LandingSurfaces` contract lets other mods expose bounded support/predicted contact with stable identity for revalidation. Vanilla collision geometry is the base provider. External providers cannot bypass Clinging authority, fluid context, collision preflight, input policy or camera ownership.

The API intentionally contains no Scale Brews types. A concrete Scale-specific adapter belongs outside the base public contract.

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
- **First Person 2.7.2** is tested with Not Enough Animations 1.12.4; Clinging isolates its macro body root from the real camera.
- **Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2** remain pinned optional client fixtures and keep ownership of internal animation.
- **Scale Brews beta.5** remains in isolated server/client compatibility lanes with no production compile dependency.
- **Gravity Changer 1.5.2-beta.5** remains the coordinate/navigation authority. Pet pursuit explicitly refreshes the navigation object Gravity Changer replaces after directional changes.

## Project status

**0.1.0-alpha.15** is the pet-pursuit/documentation follow-up to alpha.14's Gravity Fall control campaign. Its release gate still requires the complete build/JUnit, server GameTests, default client, First Person, Scale Brews server/client, Fresh Animations and semantic-snapshot matrix. The prerelease is published only from the exact `main` commit that passes that matrix.

Automated assertions are evidence, not human gameplay acceptance. Dedicated multiplayer latency, motion comfort/readability and long full-pack sessions remain manual QA, especially multi-turn pet pursuit on natural terrain.

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
