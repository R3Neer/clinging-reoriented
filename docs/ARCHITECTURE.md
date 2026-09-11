# Architecture

Clinging: Reoriented is server-authoritative. The client detects a fresh airborne
jump-key press and submits the rendered look direction; the server verifies effect
ownership, state, charge, root vehicle, gravity direction and collision clearance
before applying a change through Gravity Changer.

`GravityInput` and the input mixins handle edge-triggered controls. `AirChanges`
owns Clinging's per-airborne-stretch budget. `ClingingReoriented` validates and
applies player turns. `MountedGravity` uses the same generic non-player
`LivingEntity` root-vehicle contract for every compatible mount. `MobGravity` and
`GravityBreadcrumbs` handle mob ownership, loans and bounded pet route replay.

`Reorientation` registers the effect and brewing recipes. `BeaconPowers` exposes
Clinging as a tier-two beacon choice without adding Reorientation.

## Gravity ownership

Gravity Changer remains the sole physical gravity authority; Clinging tracks only
whether a direction is **its responsibility to retire**. Player ownership is lost
when a foreign write or modifier takes control. Mob state distinguishes the
semantics `NONE`, `OWNED_EFFECT`, `BORROWED_RIDER` and `EXTERNAL`.

A Reorientation rider loan stores the mount's previous ownership and direction.
When the loan ends, that prior frame is restored if still applicable. A later
foreign write revokes Clinging ownership instead of being reset by stale effect
state. Merely observing a gravity effect on a mob never makes Clinging owner of an
existing external direction.

## Collision and retirement

Voluntary turns are atomic and never reposition an entity. Root and complete
passenger hierarchies are preflighted before a turn or recovery candidate commits;
a rejected candidate has zero positional, gravity or momentum mutation.

Forced player retirement first tests DOWN in place, then a deterministic loaded
search limited to a true Euclidean displacement of four blocks. No saved remote
checkpoint and no full-column scan are used. If every local candidate is blocked,
`retirementPending` keeps the current frame temporarily, blocks further voluntary
turns and retries periodically while ordinary movement remains available.

## Presentation ownership

Physical ownership and presentation ownership are intentionally separate. Each
Clinging-initiated visual frame change receives a monotonic clientbound visual
epoch and target direction. `VisualTransitions` associates that epoch with Gravity
Changer's own `GravityRotationAnimation`; upstream interpolation and interruption
handling remain authoritative.

`cameraRotationSeconds` replaces Gravity Changer's duration only for an animation
owned by that visual epoch. External Gravity Changer transitions keep upstream
timing. The First Person offset fix follows the same ownership boundary, including
Clinging's own animated return to DOWN, rather than acting as a global First
Person/Gravity Changer compatibility patch.

## Moving surfaces

The current `MovingSurface` path is transitional pending the future Scale Brews G7
shared-collision migration. It is still server-authoritative and now accepts only
the latest consecutive material-support interval, within a bounded age, and
consumes that interval once. An authentic but older support origin cannot be
replayed for a second correction. Support/revision/teleport discontinuities clear
the causal fence.

This is intentionally minimal hardening, not a second permanent receipt/prediction
architecture. Once Scale's shared collision/reconciliation API is stable, G7 is the
point where Clinging should delete its duplicate anatomical contact, carry,
movement-reference and reconciliation responsibilities.

## Optional integrations

Optional integrations are isolated from the required production classpath. First
Person is gated by the mixin configuration plugin. Scale Brews uses reflective
bridges plus a `@Pseudo` mixin targeted by class name, so neither its classes nor a
Scale JAR are required to compile or load Clinging. The mixin plugin checks only
whether Scale is installed and deliberately avoids reflective API probing during
Mixin bootstrap; its optional injections use `require = 0`. The reflective bridges
resolve the concrete legacy/anatomical APIs later and fail closed when those APIs
are absent or incompatible. The production JAR can therefore load without
Alchemical Leather, Scale Brews or First Person. It cannot load without the
dependencies declared in `fabric.mod.json`.

Tiny Mount gravity-specific mixins no longer live in Clinging. A Tiny Mount is an
ordinary root vehicle to `MountedGravity`; Scale Brews owns the correctness of the
flight/glide/pounce vectors that Scale itself generates. The remaining Scale
anatomical bridge is transitional G7 infrastructure, not a Tiny Mount protocol.

Pet route queues are bounded by count and time, and lifecycle hooks clear them at
teleport, dimension, death and disconnect boundaries.
