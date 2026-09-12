# Architecture

Clinging: Reoriented is server-authoritative. The client detects a fresh airborne
jump-key press and submits the rendered look direction only to select the intended
cardinal target. The server verifies effect ownership, state, charge, root vehicle,
gravity direction and collision clearance before committing a change through
Gravity Changer.

`GravityInput` and the input mixins handle edge-triggered controls. `AirChanges`
owns Clinging's per-airborne-stretch budget. Clinging permits exactly one voluntary
turn per airborne stretch, including DOWN; `Reorientation` removes that limit.
`MountedGravity` uses the same generic non-player `LivingEntity` root-vehicle
contract for compatible mounts. `MobGravity` and `GravityBreadcrumbs` handle mob
ownership, loans and bounded pet route replay.

`Reorientation` registers the effect and brewing recipes. `BeaconPowers` exposes
Clinging as a tier-two beacon choice without adding Reorientation.

## Gravity ownership

Gravity Changer remains the physical gravity/coordinate authority; Clinging tracks
whether a direction is **its responsibility to retire**. Player ownership is lost
when a foreign write or modifier takes control. Mob state distinguishes `NONE`,
`OWNED_EFFECT`, `BORROWED_RIDER` and `EXTERNAL`.

A Reorientation rider loan stores the mount's previous ownership and direction.
When the loan ends, that prior frame is restored if still applicable. A later
foreign write revokes Clinging ownership instead of being reset by stale effect
state. Merely observing a gravity effect on a mob never makes Clinging owner of an
existing external direction.

## Voluntary input and sprint-jump intent

Usable Elytra retain priority over Clinging. In addition, `GravityInput` reserves a
fresh airborne Space for an imminent sprint landing only when all of these are true:
the player is sprinting, moving toward the current gravity floor and a bounded
next-tick collision prediction reaches support. The prediction uses the active
Gravity Changer gravity vector and gravity-strength attribute, so it works for all
six directions instead of assuming world DOWN. Ascending, distant or non-sprinting
players keep normal Clinging/Reorientation input.

The same predicate runs in client precheck and server authority; it is an intent
filter, not a blanket sprint lockout.

## Collision and retirement

Voluntary player turns are preflighted before gravity changes. The first candidate
uses Gravity Changer's directional box at the current entity pivot. If blocked,
Clinging computes Gravity Changer's center-aligned position for the same dimensions
and target frame and accepts it only when the resulting rotated box is collision-
free. This preserves the body's world-space center rather than searching arbitrary
nearby space and avoids false `NO_SPACE` results caused solely by rotating around
the old feet.

A rejected candidate has zero positional, gravity, heading or momentum mutation.
Mounted root/passenger hierarchies retain their existing all-or-nothing preflight.

Forced player retirement first tests DOWN in place, then a deterministic loaded
search limited to a true Euclidean displacement of four blocks. If every candidate
is blocked, `retirementPending` keeps the current frame temporarily and retries.
Retirement is forced cleanup, not voluntary input, and therefore ignores the spent
Clinging airborne budget.

## Heading transport

`GravityTransition` defines the orientation policy before mutation. It derives the
old world-space look from authoritative server yaw/pitch and the old Gravity Changer
frame.

For perpendicular gravity vectors it uses the normalized cross product as the
single 90-degree world-space rotation axis. For opposite vectors, where the cross
product is undefined, it projects the current world look onto the old horizontal
plane and uses that heading as the 180-degree axis. Looking parallel to gravity
falls back to the old local +X/right axis.

The old world look is transported by the same axis-angle rotation. Under the target
Gravity Changer frame, pitch is invariant and only a wrapped yaw delta is required.
That delta is committed authoritatively with gravity. The client applies the delta
to its *current* yaw, so mouse movement made after the input request was sent is not
overwitten by a stale absolute rotation.

## Presentation ownership and snap animation

Physical and presentation ownership are separate. A Clinging/Reorientation turn
sends a monotonic `visual_transition_v2` epoch containing target direction,
authoritative yaw delta and quarter/half-turn kind. Unrelated Gravity Changer
transitions do not enter this path.

On receipt the client captures the gravity quaternion actually being displayed,
applies the logical yaw delta immediately and builds a compensated visual start
frame. This compensation makes the composite world view continuous even though the
logical yaw has already moved into the canonical target gauge.

`GravitySnapMixin` overrides `GravityRotationAnimation.getRotation` only while that
specific animation instance has a Clinging-owned epoch. Before the target gravity
attribute arrives it holds the compensated start. Once the target is observed it
force-sets Gravity Changer's hidden animation state to the exact canonical endpoint
and visually SLERPs only from the compensated start to that endpoint with
`easeOutCubic`.

A settled 90-degree change therefore traverses exactly 90 degrees in **120 ms**;
an opposite change traverses exactly 180 degrees in **180 ms**. There is no user
camera-timing setting. An interrupted Reorientation turn captures the currently
displayed frame, applies the new yaw gauge compensation and follows the shortest
path from that real visual state to the latest canonical endpoint. Nothing is queued
and there is no snap-back to an intermediate canonical frame.

Camera, third-person rendering and First Person all consume Gravity Changer's same
visual gravity quaternion. The First Person offset mixin changes only offset-space
ownership; it does not invent a separate orientation trajectory.

## Moving surfaces

The current `MovingSurface` path is transitional pending the future Scale Brews G7
shared-collision migration. It remains server-authoritative and accepts only the
latest consecutive material-support interval, within a bounded age, consuming that
interval once. Support/revision/teleport discontinuities clear the causal fence.

## Optional integrations

Optional integrations are isolated from the required production classpath. First
Person is gated by the mixin configuration plugin. Scale Brews uses reflective
bridges plus a `@Pseudo` mixin targeted by class name, so neither its classes nor a
Scale JAR are required to compile or load Clinging. Optional injections use
`require = 0` and fail closed when the external API is absent or incompatible.

Tiny Mount gravity-specific mixins no longer live in Clinging. A Tiny Mount is an
ordinary root vehicle to `MountedGravity`; Scale Brews owns movement vectors that
Scale itself generates. Pet route queues are bounded by count and time, and
lifecycle hooks clear them at teleport, dimension, death and disconnect boundaries.
