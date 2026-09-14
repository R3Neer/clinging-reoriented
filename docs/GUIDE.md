# Player guide

This guide describes Clinging: Reoriented **0.1.0-alpha.15**.

## Controls and gravity turns

1. Leave your local gravity-relative floor by jumping, falling or being launched.
2. Release the configured jump key.
3. Look toward the world-cardinal direction that should become the next down direction.
4. Press the jump key again while airborne.

The target is selected from the **rendered** look. Holding the same press never repeats a turn. Clinging permits one successful voluntary airborne gravity decision; Reorientation permits more. Failed, blocked and same-direction attempts do not spend Clinging's charge.

A successful turn changes gravity immediately but preserves current world-space velocity. Gravity changes acceleration, not momentum, so reversing gravity brakes existing motion before accelerating the other way.

## Free-flight camera and 360-degree look

Airborne gravity changes keep the local camera independent from logical gravity. During sustained Gravity Fall, vertical look becomes full-sphere: pitch may pass through both poles and complete a 360-degree loop. When Gravity Fall ends, the same view is converted to an equivalent vanilla yaw/pitch pair rather than snapping to a different direction.

## Gravity Fall body language and air-diving

After **12 airborne ticks** of Clinging/Reorientation-owned physics, sustained fall starts Gravity Fall presentation. The macro body blends over **6 ticks** toward actual world velocity and keeps its last reliable frame near zero speed.

Velocity remains the primary body axis. Gaze can pull the body only after leaving a **35-degree neck deadzone**, capped at **7.5 degrees per tick**. Misalignment with airflow adds bounded drag: streamlined posture adds none and perfectly broadside posture adds at most **1.3% extra drag per tick**.

Holding **W** during sustained Gravity Fall redirects existing momentum toward gaze by at most **6 degrees per tick**, scaled by the positive velocity/look dot product. Perpendicular/backward gaze gives no steering authority. The redirect preserves speed before drag and creates no free thrust or lift.

At speed >= **0.75 blocks/tick**, Gravity Fall reuses vanilla Elytra airflow audio. It starts silent, fades in over **10 ticks**, then follows Elytra's speed-squared volume and high-speed pitch behaviour. Real Elytra flight takes ownership immediately.

## Landing commitment

Clinging predicts a bounded trajectory from real body dimensions, velocity, gravity and landing-surface providers. Camera LAND and BODY_LANDING share a **10-tick / 500 ms** manoeuvre when prediction starts early enough.

During `LANDING_COMMITTED`, new gravity requests are discarded rather than queued. If support becomes invalid while Clinging still owns the flight, the exact current visual frame is retained instead of snapping backward. Ordinary non-player tracked SNAP remains on the shorter 180/240 ms timing.

## Fluids and water

Intersecting **any non-empty fluid volume** suspends support, landing prediction/commitment and Gravity Fall presentation, including modded fluids. A solid seabed touched while still submerged is not a Clinging floor and does not recharge the one-turn budget.

Water keeps the deliberate gravity-request gesture: press Space, release it, then press again within **250 ms**. While Clinging/Reorientation owns water movement, Space is world **+Y** and Shift world **-Y**, independent of the current gravity basis.

## Climbables

World-vertical ladders, vines and scaffolding use an explicit gravity policy:

- **DOWN:** vanilla behaviour;
- **EAST/WEST/NORTH/SOUTH:** climbables are ignored for attachment, damping and climbing movement;
- **UP:** vanilla Y climbing is mirrored, so climb impulse points toward world -Y and sliding/downward behaviour mirrors toward +Y.

Vanilla scaffolding/sneak exceptions remain preserved.

## Sprint-jump intent

Near genuine supported landing, Space is reserved for vanilla's next sprint-jump rather than being mistaken for a gravity request. Normal effective jump power keeps the shortest reservation; stronger `JUMP_STRENGTH` plus vanilla Jump Boost can extend only that bounded prediction, capped at three ticks.

## Flight safety

Eligible Clinging flight is capped server-side at **3.92 blocks/tick** while preserving velocity direction. At an unavailable chunk frontier the server holds position and capped momentum until the next destination is available. Hard world/build-border breaches are moved back inside and outward momentum is discarded.

Safety intervention clears armed impact state so the correction itself cannot become a fake damaging collision.

## Impact damage

Impact damage uses world-space velocity actually absorbed by collision rather than stale `fallDistance`. A late gravity change cannot erase a high-speed collision; genuine braking can reduce it; tangential motion contributes little or nothing; and one multi-axis collision is handled once.

The equivalent fall is routed back through vanilla-compatible block/fall handling whenever possible.

## Mace under directional gravity

Mace smash height is literal geometric distance travelled along the **current gravity direction** in the current fall segment. A gravity-direction change starts a fresh segment, so old-axis distance and gravity-strength scaling cannot leak into smash thresholds, damage or knockback.

## Gravity-following pets

A tame pet does **not** inherit your gravity remotely. To follow your gravity route it needs its **own compatible Clinging or Reorientation effect** and normal tame-owner relationship.

Every relevant owner gravity change leaves a bounded breadcrumb containing place, direction, dimension and time. When a valid breadcrumb is pending, the pet can temporarily use vanilla `FollowOwnerGoal` to pursue it even if the owner is close enough that vanilla would normally refuse to start following because of its ten-block start dead zone.

The pet does not pathfind directly to an airborne owner coordinate. The breadcrumb is projected onto the pet's **current gravity-relative movement plane**:

- DOWN/UP gravity: XZ movement plane;
- EAST/WEST gravity: YZ movement plane;
- NORTH/SOUTH gravity: XY movement plane.

The pet walks to that projected place using its ordinary navigation and the follow goal's own bounded stop distance, with a maximum two-block arrival radius. It reorients only when it actually reaches that step.

After replaying the turn, the pet releases `FollowOwnerGoal` while unsupported. Directional gravity physics owns the fall. Once the pet gains support in the new frame, the next queued breadcrumb may be pursued.

If rotating in place would collide with the old support, replay may use **one center-aligned, collision-preflighted internal relocation**. That move is part of the same breadcrumb transaction, so later queued steps are preserved. An **external teleport** is different: it invalidates the old route and stops Clinging-authored navigation.

Gravity Changer replaces the mob's `PathNavigation` after a gravity change. Clinging therefore refreshes the `FollowOwnerGoal` navigation reference during `canUse`, continuation, start/stop and tick so the goal always drives the current directional navigator rather than a stale pre-turn object.

Safety/lifecycle rules:

- sitting pauses pursuit; standing can resume the still-pending breadcrumb;
- pets without a compatible effect retain vanilla follow behaviour and the normal start dead zone;
- stale and wrong-dimension breadcrumbs are skipped;
- owner lifecycle cleanup removes obsolete trails;
- passenger/vehicle or otherwise incompatible contexts do not replay;
- foreign gravity ownership remains foreign and is never stolen;
- ordinary Clinging still respects its one-air-turn budget; Reorientation can replay further airborne steps.

Non-player gravity presentation remains Clinging's shorter 180/240 ms tracked SNAP. Pets do not inherit the local player's 500 ms landing presentation or full-sphere camera.

## Mounts

Clinging itself does not grant mounted turning. With Reorientation, a fresh Space while a compatible root mount is airborne can turn the complete passenger hierarchy only when destination preflight succeeds for every member. Failure is atomic.

## Elytra

Usable Elytra owns Space while airborne. It deploys normally instead of turning gravity, voluntary turns are rejected while gliding, and Gravity Fall presentation/sound yields immediately to real fall-flying.

## First Person and animation compatibility

First Person 2.7.2 + Not Enough Animations 1.12.4 is an explicit compatibility target. The local avatar uses a blended camera/body pivot so steep look-down angles avoid clipping without hiding the body. Fresh Animations/EMF retains internal limb/head/equipment animation while Clinging applies only the macro body root.

## Effects and brewing

Clinging comes from Alex's Mobs Continued and grants one successful voluntary airborne gravity decision before valid support restores it. Add a **shulker shell** to a Clinging potion to brew Reorientation, which removes the airborne turn limit. Redstone, gunpowder and dragon's breath retain their ordinary extension/splash/lingering routes. Clinging remains available as a tier-two beacon power; Reorientation is not a beacon choice.

## Recovery and lifecycle

When Clinging-owned gravity must retire, the mod first attempts DOWN in place and then a deterministic validated local search within four blocks. If no safe placement exists, retirement remains pending instead of teleporting to a distant checkpoint.

Teleport, dimension transfer, death/respawn, disconnect, fluid entry, Elytra and foreign ownership explicitly clear or transfer incompatible transient player state. Pet breadcrumb routing separately distinguishes its own bounded internal replay relocation from an external teleport that invalidates the route.

## Landing-surface API

Other mods can register a `LandingSurfaceProvider` exposing bounded support/predicted contact plus a stable revalidation key. Providers do not choose gravity, camera behaviour, placement or input policy, and invalid/stale/non-finite results fail closed.

Vanilla collision geometry is the base provider. Scale Brews types remain outside the public Clinging API; a concrete Scale-specific adapter belongs in a consumer/integration layer.

## Installation

Client and server need matching versions plus Minecraft 26.2, Java 25, Fabric Loader 0.19.5+, Fabric API 0.159.0+26.2+, Alex's Mobs Continued 2.1.9, CodxLib 1.5.1+, Gravity Changer Unofficial Port 1.5.2-beta.5-mc26.2 and Cloth Config API.

This remains an alpha. Back up important worlds before updating.
