# Architecture

Clinging: Reoriented is server-authoritative. A fresh airborne jump-key edge captures
two different pieces of client intent at the same instant: the rendered camera
forward (`selectionLook`) and the player's navigation heading (`navigationHeading`).
The former chooses the intended cardinal target; the latter describes which way the
player considers forward on the current gravity plane. The server still verifies
effect ownership, state, charge, root vehicle, gravity direction and collision
clearance before committing any change through Gravity Changer.

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
the player is sprinting, airborne, moving toward the current gravity floor, the
current body is clear and a bounded swept collision prediction reaches real support.
Ascending, distant or non-sprinting players keep normal Clinging/Reorientation
input.

Alpha.12 makes the prediction horizon jump-power-aware. Effective jump power uses
`Attributes.JUMP_STRENGTH` when present (otherwise Vanilla's `0.42`) plus
`Player.getJumpBoostPower()`. The landing horizon is
`clamp(effectiveJumpPower / 0.42, 1, 3)` ticks, so normal jumping preserves
alpha.11's exact one-tick guard while stronger Jump Boost/Leaping-style jumps widen
only the near-landing reservation. Invalid/non-finite values fail back to the normal
baseline.

Over horizon `h`, gravity-relative travel is predicted as
`toward*h + acceleration*h*(h+1)/2`, capped at `0.60*h`. Collision is tested over
the complete swept AABB between current and predicted body, not only at the endpoint.
Gravity acceleration comes through Gravity Changer, so gravity-strength modifiers
and all six directions participate. The same predicate runs in client precheck and
server authority; it is an intent filter, not a blanket sprint lockout.

### Underwater Space arbitration

The existing `JumpInputMixin` remains the normal airborne/Elytra-adjacent entry
point. Its client entry explicitly refuses water so a single aquatic Space can never
turn gravity through that path.

Underwater input instead uses `WaterDoubleTapDetector`, a tiny passive client state
machine sampled from `options.keyJump.isDown()` at end-of-client-tick. It never
calls `consumeClick()`, clears, or rewrites the key state, so Vanilla keeps ordinary
Space-to-ascend behavior. The detector tracks only rising edges and a monotonic
millisecond timestamp:

- entering a valid water/gameplay context seeds the previous key state and clears
  any partial gesture, preventing an already-held Space from becoming a synthetic
  first tap;
- the first rising edge records a timestamp and sends nothing;
- after a real release, a second rising edge within **250 ms** consumes the pair and
  requests a turn through the same request-building path used in air;
- holding the key cannot repeat because there is no new rising edge;
- an expired second edge becomes the first edge of a new pair;
- leaving water, UI/focus/lifecycle invalidation or disconnect resets the detector.

The pair is consumed before server authority answers. A rejected turn therefore
cannot make a third rapid press inherit the old first tap. Because the detector is
purely observational, accepted and rejected requests both leave Vanilla swimming
input intact.

Server authority is unchanged. `GravityInput.available()` deliberately does not
reject water; Elytra eligibility already yields while submerged. Clinging keeps its
one-turn budget and Reorientation remains unlimited.

## Selection versus navigation heading

`selectionLook` is the actual rendered camera direction and is used only by
`LookDirection.select` to choose the target gravity. `navigationHeading` is derived
from local yaw with pitch forced to zero and transformed into world space through
the current gravity frame. It is used only to determine orientation transport.
Consequently a temporary vertical look required to select UP or DOWN cannot erase
the player's prior forward direction.

The request protocol is `select_intent_v3` and carries both vectors. Client heading
is input intent rather than physical authority: before geometry uses it, the server
projects it onto the plane perpendicular to current gravity and normalizes it. A
non-finite, nearly-zero or degenerate projection falls back to a heading rebuilt
from authoritative server yaw with pitch zero.

## Collision, fall segmentation and retirement

Voluntary player turns are preflighted before gravity changes. The first candidate
uses Gravity Changer's directional box at the current entity pivot. If blocked,
Clinging computes Gravity Changer's center-aligned position for the same dimensions
and target frame and accepts it only when the resulting rotated box is collision-
free. This preserves the body's world-space center rather than searching arbitrary
nearby space and avoids false `NO_SPACE` results caused solely by rotating around
the old feet.

A rejected candidate has zero positional, gravity, heading, momentum or fall-state
mutation. Mounted root/passenger hierarchies retain their existing all-or-nothing
preflight. Entity visual ownership is also created only after that preflight has
succeeded, immediately before the physical commit; failed candidates cannot publish
a cosmetic turn for a change that never happened.

An actual Clinging-owned gravity-direction change also resets vanilla
`fallDistance`. Gravity Changer already evaluates fall damage in local gravity
space, so each new direction represents a new fall segment rather than continuing
the distance accumulated toward the previous floor. The reset happens only after a
successful direction change. The same rule is applied by `MobGravity` to mounts,
pet replay and owned mob recovery.

`AirChanges.grounded()` controls Clinging charge restoration and remains independent
from water input. It requires Minecraft's grounded state plus collision support on
the feet-side face selected by the current gravity. Fluids have no support collision
shape, and side/body contact fails the feet-face geometry test, so free swimming or
brushing a wall cannot recharge Clinging. A real seabed/floor support does.

Forced player retirement first tests DOWN in place, then a deterministic loaded
search limited to a true Euclidean displacement of four blocks. If every candidate
is blocked, `retirementPending` keeps the current frame temporarily and retries.
Retirement is forced cleanup, not voluntary input, and therefore ignores the spent
Clinging airborne budget.

## Heading transport

`GravityTransition` defines the orientation policy before mutation. It accepts the
old gravity, target gravity and sanitized world navigation heading; instantaneous
pitch is deliberately absent from the planning inputs.

For perpendicular gravity vectors it uses the normalized cross product as the
single 90-degree world-space rotation axis. For opposite vectors, where that cross
product is undefined, the navigation heading itself is the 180-degree axis. The
heading is transported by that physical axis-angle rotation.

`yawDelta` is calculated as a coordinate-gauge difference, not from a stale absolute
server yaw: the original world heading is converted to old-local yaw and the
transported heading is converted to target-local yaw, then the wrapped difference
is taken. Applying that same delta to any current local yaw preserves the player's
late mouse movement. Applying it while leaving local pitch unchanged reproduces the
same physical rotation for the complete look vector, including near-vertical views.

`GravityTransition.rebase(physicalPlan, entityHeading)` is the mounted equivalent.
It retains the already chosen previous/target directions, turn kind and physical
axis, projects the mount's own heading onto the old gravity plane, transports that
heading through the same axis-angle and derives a mount-specific `yawDelta`. This is
important for opposite mounted turns: the rider chooses the physical 180-degree axis
once, rather than allowing rider and root mount to rotate around unrelated axes.
Pet replay has no rider-defined plan and therefore constructs an ordinary plan from
the pet's own heading.

The gauge delta is applied together to view, previous view, body and head yaw
accumulators so vanilla interpolation cannot manufacture a separate third-person
twist. The same gauge operation is applied to Clinging-owned mobs on the server and
to their tracked client entities.

## Presentation ownership, respawn epochs and snap animation

Physical and presentation ownership are separate. Player turns use the monotonic
`visual_transition_v2` payload containing target direction, authoritative yaw delta
and quarter/half-turn kind. Clinging-owned non-player changes use a distinct
`entity_visual_transition` payload containing entity ID, UUID, target direction,
yaw delta, turn kind and a per-mob monotonic sequence. Unrelated Gravity Changer
transitions do not enter either path.

Player visual sequence numbers are connection-scoped. `PlayerData` is replaced when
a `ServerPlayer` respawns, so the `COPY_FROM` path explicitly carries
`visualSequence` into the replacement object. The active visual animation itself is
entity-instance-scoped because `VisualTransitions` keys ownership by the old
entity's `GravityRotationAnimation`; an animation cannot migrate from the corpse to
the replacement player. Client player-sequence state resets only when the connection
ends.

Mob sequences live in `MobGravity.State` and are persisted with the entity so a
chunk unload/reload cannot make a genuinely newer transition look stale to a client
that stayed connected. The server sends an entity transition to every player
tracking that entity and explicitly includes all `ServerPlayer` passengers in the
root hierarchy. The client resolves both entity ID and UUID against the current
`ClientLevel`; a missing entity, reused ID or stale UUID fails closed. Accepted
entity sequences are tracked independently per UUID and cleared on disconnect.

On receipt the client captures the gravity quaternion actually being displayed,
applies the logical yaw delta immediately and builds a compensated visual start
frame. This compensation makes the composite world view continuous even though the
logical yaw has already moved into the canonical target gauge.

`GravitySnapMixin` overrides `GravityRotationAnimation.getRotation` only while that
specific animation instance has a Clinging-owned epoch. Before the target gravity
attribute arrives it holds the compensated start. Once the target is observed it
force-sets Gravity Changer's hidden animation state to the exact canonical endpoint
and visually SLERPs only from the compensated start to that endpoint with quadratic
ease-out.

A settled 90-degree change traverses exactly 90 degrees in **180 ms**; an opposite
change traverses exactly 180 degrees in **240 ms**. There is no user camera-timing
setting. An interrupted Reorientation turn captures the currently displayed frame,
applies the new yaw gauge compensation and follows the shortest path from that real
visual state to the latest canonical endpoint. Nothing is queued and there is no
snap-back to an intermediate canonical frame.

For mounts, `MountedGravity` creates the rider's physical plan before mutation and
passes that same plan into `MobGravity.borrow`. The root receives the rebased entity
plan and tracked entity payload, while the rider keeps the ordinary local-player
payload. Pet replay, owned restoration and owned retirement create entity plans from
the mob's own heading. Same-direction writes create neither payload nor yaw mutation.
Foreign/external Gravity Changer writes never acquire Clinging visual ownership and
continue to use upstream animation behavior.

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
