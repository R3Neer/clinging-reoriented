# Player guide

This guide describes Clinging: Reoriented **0.1.0-alpha.16**.

## Controls and gravity turns

1. Leave your local gravity-relative floor by jumping, falling or being launched.
2. Release the configured jump key.
3. Look toward the world-cardinal direction that should become the next down direction.
4. Press the jump key again while airborne.

The target is selected from the **rendered** look. Holding the same press never repeats a turn. Clinging permits one successful voluntary airborne turn; Reorientation permits more. Failed, blocked and same-direction attempts do not spend Clinging's charge.

A successful turn changes gravity immediately but preserves the current world-space velocity. Gravity changes acceleration, not momentum, so reversing gravity brakes the existing motion before accelerating the other way.

## Free-flight camera and 360-degree look

Airborne gravity changes do not force the local camera into the new gravity basis. Clinging retains the world frame that was actually being rendered, while target selection continues to use that retained view.

During sustained **Gravity Fall**, vertical look becomes full-sphere. Pitch can cross both poles and complete a full 360-degree loop instead of clamping at vanilla's +/-90 degrees. When Gravity Fall ends, the current viewing direction is converted to an equivalent vanilla yaw/pitch pair, so returning to vanilla coordinates does not change where the player is looking.

## Gravity Fall body language

After **12 airborne ticks** of Clinging/Reorientation-owned physics, a sustained fall starts Gravity Fall presentation unless another context already owns the moment. The macro body root blends over **6 ticks** toward the actual world velocity direction.

Velocity remains the primary body axis. Near zero speed the last reliable frame is retained so gravity reversals do not produce numerical flips.

The camera may pull the body only after gaze leaves a **35-degree neck deadzone**. Beyond that cone, the macro body follows gradually, capped at **7.5 degrees per tick**. This does not give the camera ownership of logical gravity selection.

Body/airflow alignment also changes drag. Streamlined flight receives no extra penalty; a perfectly broadside body receives at most **1.3% additional drag per tick**.

Fresh Animations/EMF keeps ownership of limbs, head tracking, equipment and micro-animation. Clinging applies only the global Gravity Fall body root.

## Air-diving with W

During sustained Gravity Fall, holding **W** bends existing momentum toward the camera look direction.

- Maximum redirect is **6 degrees per tick**.
- Steering authority is proportional to the positive dot product between velocity direction and gaze.
- Perpendicular or backward gaze produces no steering authority.
- The redirect preserves speed before aerodynamic drag.
- It generates no free thrust or Elytra-style lift.

This is controlled falling, not creative flight wearing a trench coat.

## Fast-air sound

At speed >= **0.75 blocks/tick**, Gravity Fall reuses vanilla's `ELYTRA_FLYING` sound locally. It begins silent, fades in over **10 ticks**, then follows Elytra's speed-squared volume curve and high-speed pitch increase. It stops immediately when Gravity Fall ends or real Elytra flight starts.

## Landing commitment

Clinging predicts a bounded trajectory using the real body, velocity, gravity and landing-surface providers. A candidate floor must be physically valid support under the active gravity.

Alpha.16 retains the shared **10-tick / 500 ms** landing window introduced in alpha.14. Camera LAND and BODY_LANDING use that timing when prediction begins early enough. This is intentionally separate from the shorter 180/240 ms tracked SNAP used by non-player entities.

During `LANDING_COMMITTED`, new gravity requests are discarded rather than queued. If the predicted support disappears while Clinging still owns flight, the exact current presentation becomes the held frame. If another subsystem takes ownership instead, Clinging releases the obsolete landing state.

## Fluids and water

Intersecting **any non-empty fluid volume** suspends Clinging support, landing commitment/prediction and Gravity Fall presentation. The rule is generic, so modded fluids receive the same treatment as water and lava.

A seabed touched while the body is still submerged is not a Clinging floor and does not restore the one-turn budget. Recharge requires genuine gravity-relative support outside fluid context.

Water keeps its deliberate gravity-request gesture: press Space, release it, then press again within **250 ms**. The ordinary/held press remains swimming input. While Clinging/Reorientation owns water movement, Space is world **+Y** and Shift is world **-Y**, regardless of current gravity.

## Climbables

World-vertical ladders, vines and scaffolding use an explicit gravity policy:

- **DOWN:** vanilla behaviour.
- **EAST/WEST/NORTH/SOUTH:** climbables are ignored for attachment, damping and climbing movement.
- **UP:** vanilla Y mechanics are mirrored; climbing impulse points toward world -Y and sliding/downward behaviour mirrors toward +Y.

Vanilla scaffolding/sneak exceptions remain preserved.

## Sprint-jump intent

Near genuine supported landing, Space is reserved for vanilla's next sprint-jump rather than being mistaken for Clinging/Reorientation. Normal effective jump power (`0.42`) keeps the shortest reservation; stronger effective `JUMP_STRENGTH` plus vanilla Jump Boost can extend the bounded lookahead, capped at three ticks.

## Flight safety

While Clinging controls sustained airborne physics, the server caps world-space speed at **3.92 blocks/tick** while preserving vector direction.

The same safety layer prevents movement into chunk columns that are not currently available to the server. At a loaded frontier, position is held and capped momentum retained until the next destination becomes available. Hard build/world-border breaches are moved back inside and outward momentum is discarded.

Safety intervention clears armed impact state so the correction itself cannot become a fake damaging collision.

## Impact damage

Clinging impact damage is based on world-space velocity actually absorbed by collision, not stale vanilla `fallDistance`.

A last-second gravity change cannot erase a dangerous collision that still happens at speed. A real reversal can reduce damage if it brakes the player in time. Tangential motion contributes little or nothing, and one multi-axis collision is resolved once.

The computed equivalent fall is routed back through vanilla block/fall handling whenever possible, retaining relevant block callbacks, immunities and mitigation.

## Mace under directional gravity

Mace smash height is literal geometric distance travelled along the **current gravity direction** in the current fall segment. A gravity-direction change starts a fresh segment.

Previous-axis distance is not inherited, and gravity-strength scaling does not multiply the geometric height. The corrected value feeds smash eligibility, bonus damage and knockback.

## Elytra

Usable Elytra owns Space while airborne. It deploys normally instead of turning gravity, voluntary Clinging turns are rejected while gliding, and Gravity Fall presentation/sound yields immediately to real fall-flying.

## First Person

First Person 2.7.2 + Not Enough Animations 1.12.4 is an explicit compatibility target. Gravity Fall uses a blended camera/body pivot for the avatar root so steep look-down angles avoid clipping without solving the problem by hiding the body. The macro root never feeds back into the real camera.

## Shulker Charge

A **Shulker Charge** is a captured shulker bullet. Hit a live shulker projectile with a melee attack or an arrow to materialize one Charge. An arrow fired by a dispenser counts. A capture event is fenced to one item even if damage/interception paths race. Blocking with a shield does not mint a Charge, and ordinary impact, expiry or unrelated destruction does not drop one.

Charges stack to 64. Right-clicking launches one from the player's eye position in the current look direction and consumes the item. Manual use has a **0.5 second cooldown**. A dispenser launches the same projectile from the dispenser face and consumes exactly one item.

The launched entity remains the exact vanilla `minecraft:shulker_bullet` type. It therefore keeps vanilla projectile impact, levitation and shulker-duplication semantics rather than becoming a lookalike custom missile.

### Target acquisition

Acquisition is server-authoritative and uses the launch intent captured at firing time:

- search range: about **32 blocks**;
- aim cone: about **15 degrees**;
- a Target Block hit directly by the central launch ray has absolute priority;
- otherwise living entities and assisted Target Blocks are ranked first by angular error, then by distance;
- living targets require line of sight when acquired;
- once a target is validly locked, a later better-looking target does not steal the lock;
- losing line of sight after lock does not by itself cancel a living target;
- if the target dies, is removed, changes dimension, or a Target Block is broken/replaced, the Charge reacquires from its current position while preserving the original intent;
- with no valid target it continues forward using cardinal shulker-style movement and keeps retrying periodically.

Movement stays orthogonal/cardinal with discrete routing turns. It does not continuously curve like a homing rocket. A real collision with a Target Block is still a real projectile hit, so the block produces its normal redstone response.

A relaunched Charge can itself be recaptured by melee or arrow under the same one-item rule.

## Effects and brewing

Clinging comes from Alex's Mobs Continued and grants one successful voluntary airborne gravity decision before valid support restores it.

**Reorientation is now brewed with a Shulker Charge, not a Shulker Shell.** Add a Shulker Charge to a Clinging potion to obtain Reorientation; long Clinging maps to long Reorientation, and redstone extends normal Reorientation. Vanilla splash and lingering routes remain available. Clinging remains available as a tier-two beacon power; Reorientation is not a beacon choice.

## Mounts and gravity-following pets

Clinging itself does not grant mounted turning. With Reorientation, a fresh Space while a compatible root mount is airborne can turn the complete passenger hierarchy only when destination preflight succeeds for every member. Failure is atomic.

A tame pet does **not** inherit your gravity remotely. To replay your gravity route it needs its **own compatible Clinging or Reorientation effect** and the normal tame-owner relationship.

Every relevant owner turn records a bounded breadcrumb containing place, direction, dimension and time. When a valid breadcrumb is pending, the pet can temporarily use vanilla `FollowOwnerGoal` to pursue it even if the owner is inside vanilla's normal ten-block follow-start dead zone.

The pet does not pathfind to an impossible airborne owner coordinate. The breadcrumb is projected onto the pet's **current gravity-relative movement plane**:

- DOWN/UP gravity: XZ plane;
- EAST/WEST gravity: YZ plane;
- NORTH/SOUTH gravity: XY plane.

The pet walks to that projection using ordinary navigation and the follow goal's own bounded stopping semantics, capped at a two-block arrival radius. Only then does it replay the recorded gravity turn.

After replay, the pet releases `FollowOwnerGoal` while unsupported so directional gravity physics owns the fall. Once it finds support in the new frame, pursuit of the next queued breadcrumb may resume.

If rotating at the current feet position would intersect the old support, replay may use **one center-aligned, collision-preflighted internal relocation**. That move belongs to the same breadcrumb transaction and preserves later queued turns. An **external teleport** is different: it invalidates the old route and stops Clinging-authored pursuit.

Gravity Changer replaces a mob's `PathNavigation` after gravity changes. Clinging refreshes the `FollowOwnerGoal` navigation reference across the goal lifecycle so the pet always drives the current directional navigator rather than a stale pre-turn object.

Safety/lifecycle rules:

- sitting pauses pursuit; standing can resume the pending breadcrumb;
- pets without a compatible effect keep vanilla follow behaviour and its normal start dead zone;
- stale and wrong-dimension breadcrumbs are skipped;
- owner lifecycle cleanup removes obsolete trails;
- passenger/vehicle or other incompatible contexts do not replay;
- foreign gravity ownership remains foreign and is never stolen;
- ordinary Clinging still respects its one-air-turn budget; Reorientation can replay further airborne steps.

Non-player gravity presentation remains Clinging's shorter **180/240 ms tracked SNAP**. Pets do not inherit the local player's 500 ms landing presentation or full-sphere camera.

## Recovery and lifecycle

When Clinging-owned gravity must retire, the mod first attempts DOWN in place and then a deterministic validated local search within four blocks. If no safe placement exists, retirement remains pending instead of teleporting to a distant checkpoint.

Teleport, dimension transfer, death/respawn, disconnect, fluid entry, Elytra and foreign ownership explicitly clear or transfer transient landing/Gravity Fall state. Respawn keeps visual epochs monotonic so stale packets cannot become new presentation state. Pet breadcrumb routing separately distinguishes its bounded internal replay relocation from an external teleport that invalidates the route.

## Landing-surface API

Other mods can register a `LandingSurfaceProvider`. Providers may expose bounded support/predicted contact plus a stable revalidation key. They do not choose gravity, camera behaviour, placement or input policy. Invalid, stale, exceptional or non-finite provider results fail closed.

Vanilla collision geometry is the base provider. Scale Brews types remain outside the public Clinging API; a concrete Scale-specific adapter belongs in a consumer/integration layer.

## Installation

Client and server need matching versions plus Minecraft 26.2, Java 25, Fabric Loader 0.19.5+, Fabric API 0.159.0+26.2+, Alex's Mobs Continued 2.1.9, CodxLib 1.5.1+, Gravity Changer Unofficial Port 1.5.2-beta.5-mc26.2 and Cloth Config API.

This remains an alpha. Back up important worlds before updating.
