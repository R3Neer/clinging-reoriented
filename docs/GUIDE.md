# Player guide

This guide describes Clinging: Reoriented **0.1.0-alpha.15**.

## Controls and gravity turns

Leave your local gravity-relative floor, release the configured jump key, look toward the world-cardinal direction that should become down, then press jump again. Clinging permits one successful voluntary airborne gravity decision; Reorientation permits more. Gravity changes acceleration immediately but preserves current world-space velocity.

## Free-flight camera, Gravity Fall and air-diving

Airborne gravity changes keep the local camera independent from logical gravity. During sustained Gravity Fall, vertical look is full-sphere and can pass through both poles. On exit, the same viewing direction is converted to an equivalent vanilla yaw/pitch pair.

After 12 airborne ticks, the macro body blends toward actual world velocity. Gaze can pull the body only outside a 35-degree neck deadzone, at up to 7.5 degrees/tick. Broadside posture adds bounded drag while streamlined posture adds none.

Holding **W** during sustained Gravity Fall redirects existing momentum toward gaze by at most 6 degrees/tick, scaled by positive velocity/look alignment. It preserves speed before drag and creates no thrust. At >=0.75 blocks/tick, Elytra-style airflow audio fades in over 10 ticks and then follows vanilla's speed-sensitive volume/pitch language.

## Landing and context

Camera LAND and BODY_LANDING share a 10-tick / 500 ms manoeuvre. During `LANDING_COMMITTED`, new gravity requests are discarded rather than queued.

Any intersecting non-empty fluid suspends support, landing and Gravity Fall semantics. In water, Space is world +Y and Shift world -Y while Clinging/Reorientation owns the movement. Ladders/vines/scaffolding are vanilla with DOWN gravity, ignored under lateral gravity and vertically mirrored under UP gravity.

## Flight safety, impact and mace

Eligible Clinging flight is capped server-side at 3.92 blocks/tick while preserving direction. Unloaded chunk frontiers hold the player until movement is safe; hard world/build boundaries recover inside the valid region.

Impact damage uses world-space velocity actually absorbed by collision. Mace smash height uses literal geometric distance along the current gravity direction in the current fall segment; changing gravity starts a fresh mace segment.

## Gravity-following pets

A tame pet does **not** inherit your gravity remotely. To follow your gravity route it needs its **own compatible Clinging or Reorientation effect** and normal tame-owner relationship.

Every relevant owner gravity change leaves a bounded breadcrumb containing place, direction, dimension and time. When a valid breadcrumb is pending, the pet can temporarily use vanilla `FollowOwnerGoal` to pursue it even if the owner is close enough that vanilla would normally refuse to start following because of its ten-block start dead zone.

The pet does not pathfind directly to an airborne owner coordinate. The breadcrumb is projected onto the pet's **current gravity-relative movement plane**:

- with DOWN/UP gravity, it walks across its current horizontal Y plane;
- with EAST/WEST gravity, movement is projected onto the YZ plane;
- with NORTH/SOUTH gravity, movement is projected onto the XY plane.

The pet walks to that projected place using its ordinary navigation and the follow goal's own bounded stop distance, capped by Clinging to a maximum two-block arrival radius. It reorients only when it actually reaches that step.

After replaying the turn, the pet releases `FollowOwnerGoal` while unsupported. It then falls/moves under real directional gravity instead of being dragged by pathfinding. When it gains support in the new gravity frame, the next queued breadcrumb can be pursued.

If rotating in place would collide with the old support, the replay may use one center-aligned, collision-preflighted internal relocation. This is part of the same breadcrumb transaction, so later queued steps are preserved. By contrast, an **external teleport** of the pet invalidates the old breadcrumb route and stops Clinging-authored navigation.

Gravity Changer replaces the entity's `PathNavigation` when gravity changes. Clinging refreshes the `FollowOwnerGoal` navigation reference during `canUse`, continuation, start/stop and tick, so the goal always drives the current directional navigator rather than a stale pre-turn object.

Safety/lifecycle rules:

- sitting pauses pursuit; standing again can resume the still-pending breadcrumb;
- pets without a compatible effect retain vanilla follow behaviour, including the normal start dead zone;
- stale breadcrumbs and breadcrumbs from another dimension are skipped;
- owner death/offline cleanup removes old trails;
- passenger/vehicle contexts and incompatible entities do not replay;
- foreign gravity ownership remains foreign and is never stolen;
- ordinary Clinging still respects its one-air-turn budget, while Reorientation can replay further airborne steps.

Non-player gravity presentation remains Clinging's shorter 180/240 ms tracked SNAP. Pets do not inherit the local player's full-sphere camera or 500 ms landing presentation, because, mercifully, wolves do not need a cinematography system.

## Mounts

Clinging itself does not grant mounted turning. With Reorientation, a compatible airborne root mount and passenger hierarchy can turn atomically only when destination preflight succeeds for every member.

## First Person and animation compatibility

First Person 2.7.2 + Not Enough Animations 1.12.4 is an explicit compatibility target. The local avatar uses a blended camera/body pivot so steep look-down angles avoid clipping without hiding the body. Fresh Animations/EMF retains internal limb/head/equipment animation while Clinging applies only the macro body root.

## Effects and brewing

Clinging comes from Alex's Mobs Continued and grants one successful voluntary airborne gravity decision before valid support restores it. Add a shulker shell to a Clinging potion to brew Reorientation. Redstone, gunpowder and dragon's breath retain their ordinary routes.

## Installation

Client and server need matching versions plus Minecraft 26.2, Java 25, Fabric Loader 0.19.5+, Fabric API 0.159.0+26.2+, Alex's Mobs Continued 2.1.9, CodxLib 1.5.1+, Gravity Changer Unofficial Port 1.5.2-beta.5-mc26.2 and Cloth Config API.

This remains an alpha. Back up important worlds before updating.
