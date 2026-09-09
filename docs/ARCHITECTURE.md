# Architecture

Clinging: Reoriented is server-authoritative. The client detects a fresh airborne
jump-key press and submits the rendered look direction; the server verifies effect
ownership, state, charge, root vehicle, gravity direction and collision clearance
before applying a change through Gravity Changer.

`GravityInput` and the input mixins handle edge-triggered controls. `AirChanges`
owns Clinging's per-airborne-stretch budget. `ClingingReoriented` validates and
applies player turns. `MountedGravity` performs group transfer and cleanup, while
`MobGravity` and `GravityBreadcrumbs` handle passive effect sources and bounded pet
route replay.

`Reorientation` registers the effect and brewing recipes. `BeaconPowers` exposes
Clinging as a tier-two beacon choice without adding Reorientation.

Gravity Changer remains the sole physics authority. Mixins suppress only conflicting
upstream assumptions while this mod owns active Clinging gravity; they do not
replace its acceleration, movement, jumping, collision or camera systems. The
camera-duration mixin changes presentation timing only.

Optional integrations are isolated by the mixin configuration plugin and reflective
bridges. The production JAR can load without Alchemical Leather, Scale Brews, First
Person or Scale Visual Compat. It cannot load without the dependencies declared in
`fabric.mod.json`.

Voluntary turns are atomic and never reposition an entity. Forced return to DOWN
may search validated nearby space only when effect removal would otherwise embed the
entity. Route queues are bounded by count and time, and lifecycle hooks clear them
at teleport, dimension, death and disconnect boundaries.
