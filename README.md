# Clinging: Reoriented

The floor is wherever you decide it is.

**Clinging: Reoriented** turns the Clinging effect from Alex's Mobs into an airborne gravity ability for Minecraft 26.2 on Fabric. Leave your local floor, release **Space**, look toward another world-cardinal direction and press Space again. Clinging grants one voluntary airborne gravity decision; **Reorientation** removes that one-turn limit. **0.1.0-beta.5** is the current prerelease and includes the gravity-navigation/gamefeel campaign described below.

[![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-62B47A)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DDBD3B)](https://fabricmc.net/)
[![Build and test](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml/badge.svg)](https://github.com/R3Neer/clinging-reoriented/actions/workflows/ci.yml)
[![GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

![Clinging turns east into the new down](docs/images/0000_clinging-east-gravity.png)

## Movement language

A voluntary turn changes **physical gravity immediately** but preserves the existing world-space velocity vector. Gravity changes acceleration, not momentum. Reversing gravity therefore brakes the old motion naturally, crosses zero speed, and only then accelerates the other way.

The local player's camera is independent from logical gravity. Free-flight turns retain the world frame that was actually being rendered, so chained Reorientation choices do not drag the view through every gravity basis. During sustained Gravity Fall the camera also gains **full-sphere look**: it can pass through both poles and complete 360-degree loops while horizontal and vertical mouse intent remains consistent on screen. First- and third-person views share that same look frame. On exit, the same viewing direction returns to ordinary vanilla yaw/pitch without a camera snap.

Landing prediction now separates **acquisition from presentation**. A valid future support can be tracked up to **40 ticks / 2 seconds** ahead, while the visible landing manoeuvre remains at most **10 ticks / 500 ms** and is timed to finish at the predicted touchdown. New gravity requests during `LANDING_COMMITTED` are discarded, never queued; invalidated support preserves the current visible frame instead of snapping backward.

## Gravity Fall body and aerodynamics

After **12 airborne ticks** under Clinging/Reorientation physics, sustained fall enters Gravity Fall presentation. The body keeps a persistent world-space attitude instead of being transported directly by velocity every tick.

Player gaze expresses body-attitude intent. The camera can move freely inside a **35-degree neck deadzone**; outside it the macro body follows by at most **7.5 degrees per tick**. Velocity contributes only a weak **1.25 degrees/tick** weathercock stabilization and always chooses the nearer head/feet orientation, so it cannot manufacture a 180-degree body flip.

Aerodynamics follows the visible body rather than a keyboard exception. Momentum is decomposed along and across the body's long axis: longitudinal momentum is retained while transverse momentum receives **2.5% additional drag per tick**. The trajectory therefore bends gradually toward the body's attitude while losing energy. **W no longer performs special airborne steering**, and the model adds no thrust or Elytra-style lift.

At speed >= **0.75 blocks/tick**, Gravity Fall reuses vanilla's Elytra airflow sound. It fades in over 10 ticks and stops immediately when Gravity Fall releases ownership or real Elytra flight begins.

## Safety, impact and mace

Clinging-controlled airborne world speed is server-capped at **3.92 blocks/tick**, preserving direction. The server also prevents advancement into unavailable chunk columns and recovers safely from hard world/build-border breaches without turning the safety correction into impact damage.

Impact damage follows the **world-space velocity actually absorbed by collision**. A late gravity turn cannot erase a dangerous collision; physically braking early enough can reduce it; tangential travel contributes little or nothing; and a multi-axis collision is resolved once.

Mace smash height under directional gravity is measured as literal geometric distance travelled along the **current gravity direction** since that fall segment began. Changing gravity starts a fresh segment.

## Fluids, water and climbables

Any intersection with a non-empty fluid volume is a context boundary for support, landing prediction/commitment and Gravity Fall body presentation. Water, lava and modded fluids use the same policy. A camera HOLD created by a gravity turn while already inside the fluid remains stable for that fluid epoch; entering fluid later with an older dry-flight HOLD still releases that obsolete frame.

Water keeps the deliberate **Space, release, Space within 250 ms** gravity-request gesture. While Clinging/Reorientation owns water movement, **W/S follow the camera forward/backward including pitch, A/D follow camera left/right, Space is world +Y and Shift is world -Y**. Stored logical gravity does not redefine free-swimming controls.

The underwater visual frame also follows context rather than the stored gravity attribute: **free swimming converges to world-up**, while real gravity-relative support converges to that support's up direction. These are camera/control semantics only; entering water does not rewrite logical gravity.

World-vertical climbables use an explicit policy: DOWN keeps vanilla ladders/vines/scaffolding; EAST/WEST/NORTH/SOUTH ignore climbable attachment/damping; UP mirrors vanilla Y climbing.

## Gravity Charge

A shulker bullet can be captured with a **melee hit or arrow**, including an arrow fired by a dispenser. Capture produces exactly one stackable Gravity Charge. A shield does not turn a blocked hit into a charge, and normal collision, expiry or unrelated destruction does not create a drop. Relaunched Gravity Charges can be recaptured under the same one-drop rule.

Right-clicking launches a Gravity Charge from the player's eyes; a dispenser launches the same projectile from its facing. Both consume one item and manual use has a **0.5 second cooldown**. The item ID is **`clinging_reoriented:gravity_charge`**.

The runtime projectile remains the exact vanilla `minecraft:shulker_bullet` entity type, so vanilla impact, levitation, renderer and shulker-duplication semantics remain available. Gravity Charge does not register a lookalike custom projectile type. Marked relaunched charges explicitly bypass vanilla's Peaceful-only shulker-bullet despawn; ordinary shulker bullets keep that vanilla rule.

Acquisition is server-authoritative and bounded to roughly **32 blocks / 15 degrees**. A Target Block directly under the launch ray has absolute priority. Otherwise entities and assisted Target Blocks are scored by angular error and then distance. A valid lock is sticky. If the target dies, disappears, changes dimension or a locked Target Block ceases to be valid, the projectile retries acquisition from its current position while preserving the original launch intent. With no target it continues cardinal free flight.

Movement deliberately speaks vanilla shulker language: orthogonal/cardinal routing with discrete turns, not continuous missile homing. Real Target Block collisions go through projectile impact and activate the block's normal redstone response.

**Reorientation brewing now consumes a Gravity Charge instead of a Shulker Shell.** Clinging + Gravity Charge produces Reorientation; long Clinging produces long Reorientation, and redstone extends ordinary Reorientation. Vanilla splash/lingering conversion remains available.

The inventory icon, held-model geometry and item texture are original GPL-3.0-or-later project assets. The launched projectile uses Minecraft's renderer/resources only through its retained vanilla entity type; Mojang assets are not redistributed by this project.

## Landing surfaces are extensible

The public `LandingSurfaceProvider` / `LandingSurfaces` contract lets other mods expose bounded support/predicted contact with stable identity for revalidation. External providers cannot bypass Clinging authority, fluid context, collision preflight, input policy or camera ownership.

The API intentionally contains no Scale Brews types. A concrete Scale-specific adapter belongs outside the base public contract.

## Mounts, pets and gravity-aware mobs

Mounted Reorientation turns a compatible airborne living root and its passenger hierarchy atomically.

Pet following is **history-free**: pets no longer replay owner gravity breadcrumbs. A pet with its own compatible effect uses the owner's current/filtered position plus current world geometry, prefers ordinary navigation when it works, walks to a validated launch frontier when gravity is actually useful, revalidates immediately before committing and replans only after stable support or a material change. An airborne owner remains a tracked objective without remotely forcing the pet to copy a turn.

The same locomotion layer is available to ordinary mobs with legitimate Clinging/Reorientation capability. Vanilla goals remain owners of intent: melee pursuit, escape and ordinary position goals try vanilla navigation first, then may use bounded support-to-support gravity transitions when the normal route cannot satisfy the intent. The planner has no per-species route table.

During a gravity-aware `APPROACH`, a brief vanilla navigation jump may temporarily remove real support. The executor keeps the already-owned route/intent for at most **20 ticks**, but performs no new planning and cannot commit a gravity transition until support returns; a longer support loss fails and cools down that maneuver.

During a committed gravity flight the mob monitors the real trajectory without surface pathfinding. Material target/world changes become actionable only after a **2–10 tick reaction delay derived from base movement speed**. Reorientation may make another airborne correction only when that correction is physically legal; spent Clinging never receives a second turn. A block placed too late can therefore still result in a perfectly ordinary collision.

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

**0.1.0-beta.5 is the current prerelease.** Back up important worlds before testing prereleases and use matching versions on every multiplayer participant.

## Optional companions and compatibility

- **Alchemical Leather** remains optional. When present, Clinging: Reoriented owns Reorientation's boots slot plus the semantic wear events for successful gravity turns and controlled Reorientation flight. Passive riding/support transport does not count as continuous work; Alchemical Leather remains responsible for source arbitration, armor selection and durability. There is no hard Alchemical Leather dependency.
- **First Person 2.7.2** is tested with Not Enough Animations 1.12.4.
- **Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2** are exercised in a pinned client lane.
- **Scale Brews beta.5** is exercised in isolated server/client compatibility lanes; production compile/runtime does not require it.

The exact Alchemical Leather ownership and validation contract is recorded in [the compatibility guide](docs/COMPATIBILITY.md) and [TM closeout](docs/TM_ALCHEMICAL_LEATHER_COMPAT.md).

## Project status

**0.1.0-beta.5** is the current beta. It carries the beta.4 gravity-navigation/gamefeel stack plus a focused Gravity Charge hotfix for Peaceful worlds. Grounded gravity planning is bounded to at most **20 transition forecasts per local plan**, **32 new plans per level/tick** and **4 per 64×64 region/tick**; committed flight uses only a short reaction-bound monitor until support/recovery.

The release gate covers localization parity, build/JUnit, required server GameTests, default client, First Person, optional Scale Brews server/client, pinned Fresh Animations/Player Extension and semantic screenshot validation. Prerelease artifacts are created only from the exact successful `main` CI artifact, never from a second build.

## Build and documentation

Use Java 25 and the included Gradle wrapper:

```powershell
.\gradlew.bat build runGameTest
.\gradlew.bat runClientGameTest
```

Production `compileClasspath` remains free of optional Scale Brews and Alchemical Leather APIs. Optional compatibility fixtures are isolated test/runtime inputs.

- [Player guide](docs/GUIDE.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Compatibility](docs/COMPATIBILITY.md)
- [Configuration](docs/CONFIGURATION.md)
- [Validation](docs/VALIDATION.md)
- [Changelog](CHANGELOG.md)

[GPL-3.0-or-later](LICENSE). Third-party projects keep their own licenses and are not bundled; see [credits and notices](THIRD_PARTY_NOTICES.md).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.