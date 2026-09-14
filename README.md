# Clinging: Reoriented

The floor is wherever you decide it is.

**Clinging: Reoriented** turns the Clinging effect from Alex's Mobs into an airborne gravity ability for Minecraft 26.2 on Fabric. Leave your local floor, release **Space**, look toward another world-cardinal direction and press Space again. Clinging grants one voluntary airborne gravity decision; **Reorientation** removes that one-turn limit. **0.1.0-beta.1** adds the **Gravity Charge**, a capturable and relaunchable shulker projectile that becomes Reorientation's brewing ingredient and marks the project's transition from alpha to beta.

[![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-62B47A)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DDBD3B)](https://fabricmc.net/)
[![Build and test](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml/badge.svg)](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml)
[![GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

![Clinging turns east into the new down](docs/images/0000_clinging-east-gravity.png)

## Movement language

A voluntary turn changes **physical gravity immediately** but preserves the existing world-space velocity vector. Gravity changes acceleration, not momentum. Reversing gravity therefore brakes the old motion naturally, crosses zero speed, and only then accelerates the other way.

The local player's camera is independent from logical gravity. Free-flight turns retain the world frame that was actually being rendered, so chained Reorientation choices do not drag the view through every gravity basis. During sustained Gravity Fall the camera also gains **full-sphere vertical look**: pitch can pass through both poles and complete a 360-degree loop. On exit, the same viewing direction is re-expressed as an ordinary vanilla yaw/pitch pair without a camera snap.

When the real trajectory is about to meet a valid gravity-relative floor, Clinging reserves a shared **10-tick / 500 ms landing manoeuvre**. New gravity requests during `LANDING_COMMITTED` are discarded, never queued; invalidated support preserves the current visible frame instead of snapping backward.

## Gravity Fall body, air and steering

After **12 airborne ticks** under Clinging/Reorientation physics, sustained fall enters Gravity Fall presentation. Over a **6-tick blend**, the macroscopic body root follows actual world velocity. Near zero speed it retains the last reliable frame, preventing numerical flips during a gravity reversal.

Velocity remains the primary body axis, but the camera can pull the body once gaze leaves a **35-degree neck deadzone**, capped at **7.5 degrees per tick**. Misalignment with the airflow adds bounded posture drag: streamlined flight is untouched and a perfectly broadside body adds at most **1.3% extra drag per tick**.

Holding **W** during sustained Gravity Fall bends the **existing** velocity toward gaze by at most **6 degrees per tick**, with authority proportional to the positive dot product between gaze and motion. It preserves speed before aerodynamic drag and creates no thrust or Elytra-style lift.

At speed >= **0.75 blocks/tick**, Gravity Fall reuses vanilla's Elytra airflow sound. It fades in over 10 ticks and stops immediately when Gravity Fall releases ownership or real Elytra flight begins.

## Safety, impact and mace

Clinging-controlled airborne world speed is server-capped at **3.92 blocks/tick**, preserving direction. The server also prevents advancement into unavailable chunk columns and recovers safely from hard world/build-border breaches without turning the safety correction into impact damage.

Impact damage follows the **world-space velocity actually absorbed by collision**. A late gravity turn cannot erase a dangerous collision; physically braking early enough can reduce it; tangential travel contributes little or nothing; and a multi-axis collision is resolved once.

Mace smash height under directional gravity is measured as literal geometric distance travelled along the **current gravity direction** since that fall segment began. Changing gravity starts a fresh segment.

## Fluids, water and climbables

Any intersection with a non-empty fluid volume is a context boundary. Water, lava and modded fluids suspend gravity-relative support, landing prediction/commitment and Gravity Fall presentation.

Water keeps the deliberate **Space, release, Space within 250 ms** gravity-request gesture. While Clinging/Reorientation owns water movement, Space is world **+Y** and Shift is world **-Y**, independent of the current gravity basis.

World-vertical climbables use an explicit policy: DOWN keeps vanilla ladders/vines/scaffolding; EAST/WEST/NORTH/SOUTH ignore climbable attachment/damping; UP mirrors vanilla Y climbing.

## Gravity Charge

A shulker bullet can be captured with a **melee hit or arrow**, including an arrow fired by a dispenser. Capture produces exactly one stackable Gravity Charge. A shield does not turn a blocked hit into a charge, and normal collision, expiry or unrelated destruction does not create a drop. Relaunched Gravity Charges can be recaptured under the same one-drop rule.

Right-clicking launches a Gravity Charge from the player's eyes; a dispenser launches the same projectile from its facing. Both consume one item and manual use has a **0.5 second cooldown**. The item ID is **`clinging_reoriented:gravity_charge`**.

The runtime projectile remains the exact vanilla `minecraft:shulker_bullet` entity type, so vanilla impact, levitation, renderer and shulker-duplication semantics remain available. Gravity Charge does not register a lookalike custom projectile type.

Acquisition is server-authoritative and bounded to roughly **32 blocks / 15 degrees**. A Target Block directly under the launch ray has absolute priority. Otherwise entities and assisted Target Blocks are scored by angular error and then distance. A valid lock is sticky. If the target dies, disappears, changes dimension or a locked Target Block ceases to be valid, the projectile retries acquisition from its current position while preserving the original launch intent. With no target it continues cardinal free flight.

Movement deliberately speaks vanilla shulker language: orthogonal/cardinal routing with discrete turns, not continuous missile homing. Real Target Block collisions go through projectile impact and activate the block's normal redstone response.

**Reorientation brewing now consumes a Gravity Charge instead of a Shulker Shell.** Clinging + Gravity Charge produces Reorientation; long Clinging produces long Reorientation, and redstone extends ordinary Reorientation. Vanilla splash/lingering conversion remains available.

The inventory icon, held-model geometry and item texture are original GPL-3.0-or-later project assets. The launched projectile uses Minecraft's renderer/resources only through its retained vanilla entity type; Mojang assets are not redistributed by this project.

## Landing surfaces are extensible

The public `LandingSurfaceProvider` / `LandingSurfaces` contract lets other mods expose bounded support/predicted contact with stable identity for revalidation. External providers cannot bypass Clinging authority, fluid context, collision preflight, input policy or camera ownership.

The API intentionally contains no Scale Brews types. A concrete Scale-specific adapter belongs outside the base public contract.

## Mounts and pets

Mounted Reorientation turns a compatible airborne living root and its passenger hierarchy atomically. A pet with its own compatible effect pursues bounded owner gravity breadcrumbs on its current gravity-relative movement plane, replays the turn there, falls under physics and resumes route pursuit after support is found in the new frame.

Non-player entities keep Clinging's owned **180/240 ms tracked snap** presentation; the 500 ms landing manoeuvre and full-sphere camera are local-player Gravity Fall rules.

## Languages

Player-facing mod strings ship in **English (`en_us`) and Spanish from Spain (`es_es`)**. CI requires exact key parity between both files and rejects missing/empty Spanish entries. In Spanish, Gravity Charge is **«Carga de gravedad»** and Reorientation is **«Reorientación»**.

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

This is a **beta prerelease**. Back up important worlds before updating and use matching versions on every multiplayer participant.

## Optional companions and compatibility

- **Alchemical Leather** remains optional and can supply Clinging/Reorientation through compatible equipment.
- **First Person 2.7.2** is tested with Not Enough Animations 1.12.4.
- **Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2** are exercised in a pinned client lane.
- **Scale Brews beta.5** is exercised in isolated server/client compatibility lanes; production compile/runtime does not require it.

## Project status

**0.1.0-beta.1** is the first beta prerelease. Gravity Charge is the milestone feature that closes the alpha line after alpha.15's pet gravity-breadcrumb pursuit. The beta gate covers build/JUnit, required server GameTests including the Gravity Charge adversarial campaign, default client, First Person, optional Scale Brews server/client, pinned Fresh Animations/Player Extension, localization parity and semantic screenshot validation.

The prerelease is published only from the exact `main` commit that passes that complete matrix. Beta means the core design is coherent enough for broader validation; it does **not** mean feature freeze or guaranteed absence of bugs.

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
