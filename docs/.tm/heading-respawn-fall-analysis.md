# TM analysis — heading, respawn visual epochs, fall reset and snap tuning

Temporary working document. Delete before merge.

## User-facing symptoms to solve

1. Alpha.10 snap trajectory feels much better, but 120/180 ms + cubic ease-out is so front-loaded that the camera appears almost not to rotate.
2. After death/respawn, Clinging-owned turns can revert to Gravity Changer's old 1.25 s canonical-frame interpolation and old orientation semantics.
3. DOWN -> UP selected by looking vertically can reverse the player's prior world heading when the player levels the camera afterward.
4. Fall damage can accumulate across several successful gravity changes, making long Reorientation chains lethal even though each turn establishes a new fall direction.

## Current architecture findings

### Selection and orientation are conflated

`Payloads.Request` currently carries only the rendered world-space look vector. The server uses that vector to select a target direction, but `GravityTransition.plan(...)` separately derives orientation transport from server-side yaw/pitch.

For perpendicular turns this works because a unique minimal 90-degree physical rotation exists. For opposite gravity, `plan(...)` projects the current full look onto the old gravity plane. When the look is vertical that projection degenerates and alpha.10 falls back to the old frame's local +X axis. That axis is not the player's navigation heading and can therefore invert or otherwise change heading after DOWN <-> UP.

Minecraft/Gravity Changer already preserve yaw while pitch approaches +/-90 degrees, so the desired heading can be recovered without a temporal camera-history heuristic: build local forward from yaw with pitch=0 and transform it through the current gravity frame.

### Yaw delta should be a frame property, not tied to stale server yaw

The robust definition is:

- `selectionLook`: actual world camera forward at the input edge; used only to choose target gravity.
- `navigationHeading`: world-space forward projected onto the current gravity plane, derived from yaw with pitch=0; used only to define orientation transport.
- choose physical rotation R:
  - perpendicular: axis = oldGravity x newGravity, angle=90 degrees;
  - opposite: axis = navigationHeading, angle=180 degrees.
- transport heading: H1 = R(H0).
- convert H0 to old-local yaw and H1 to target-local yaw.
- `yawDelta = wrap(targetLocalYaw - oldLocalYaw)`.

This delta is independent of whatever late mouse movement occurs after the request. It can be applied additively to the current client and server yaw gauges.

The same R then transports any look vector sharing that yaw while preserving local pitch, so the camera can remain at its instantaneous pitch while navigation heading survives vertical selection.

### Respawn epoch bug

Client `VisualTransitions.latestSequence` survives until disconnect. Server `PlayerData.visualSequence` belongs to the `ServerPlayer` instance and is not copied on death respawn. After a death the server resumes from sequence 1 while the client may remember a larger sequence and rejects all new `visual_transition_v2` packets as stale. Those packets carry both snap ownership and yaw-gauge correction, so rejecting them recreates upstream Gravity Changer timing/orientation.

Correct ownership boundary:

- visual network epoch is connection-scoped and must remain monotonic across respawn;
- active animation is entity-instance-scoped and must not transfer from the dead entity to the new player object.

The server should copy `visualSequence` in `COPY_FROM` for both alive and death respawns. Client active transitions are keyed by the old entity's `GravityRotationAnimation`, so they naturally do not attach to the replacement entity; no connection-sequence reset should occur on respawn.

### Fall state

There are two fall systems:

- Gravity Changer's `DirectionalFallTracker`, already suppressed/reset by Clinging while Clinging owns physics;
- vanilla `Entity.fallDistance`, still accumulated through Gravity Changer's gravity-relative `checkFallDamage` hooks.

Alpha.10 does not reset vanilla fall distance when a Clinging/Reorientation gravity turn succeeds. New semantic requirement: every successful Clinging-owned operation that actually changes gravity starts a new fall segment and calls `resetFallDistance()` exactly once after successful commit. Failed/unchanged attempts must not reset it.

The rule should cover player voluntary turns, forced retirement, mounted/root turns and Clinging-owned mob turns/replays/restore operations because they all establish a new physical down direction.

### Snap timing

Current 120/180 ms with ease-out cubic completes roughly 90% of movement in 64/97 ms. This is visually too compressed even though the path is correct.

Next tuning candidate is fixed gameplay policy, not configuration:

- quarter turn: 180 ms;
- half turn: 240 ms;
- easing: ease-out quadratic `1 - (1-t)^2`.

At 180 ms the quarter turn reaches 50% around 53 ms, 75% around 90 ms and 90% around 123 ms. It remains a snap but the axis becomes perceptible.

## Security/authority boundary

The client may send `navigationHeading` because it describes presentation/input intent, not physical authority. The server still validates effect ownership, airborne state, target selection, charge and collision. Heading must be finite, non-zero and approximately perpendicular to current gravity; otherwise reconstruct it from server yaw with pitch=0. An arbitrary heading is not a meaningful privilege escalation because clients already control ordinary rotation packets, but malformed values must never reach geometry math.

## Acceptance invariants

- `selectionLook` never determines retained heading except through target gravity selection.
- `navigationHeading` never changes which gravity direction is selected.
- For every perpendicular pair, physical presentation path is exactly 90 degrees.
- For every opposite pair, the player's world navigation heading is invariant across the change.
- Vertical camera selection preserves navigation heading because heading is derived with pitch=0.
- Local pitch is not reset by the gravity change.
- A visual sequence accepted before death cannot cause post-respawn transitions to be rejected as stale.
- Failed or unchanged turns do not reset fall distance; successful gravity changes do.
- Foreign Gravity Changer changes keep upstream presentation/timing.
