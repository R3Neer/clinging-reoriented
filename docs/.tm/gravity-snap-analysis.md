# TM analysis — gravity snap policy

Temporary working document. Delete before merge.

## Scope

Implement the closed gameplay/presentation policy for Clinging/Reorientation:

1. Physical gravity changes immediately.
2. Visual gravity rotation follows the minimal transport between old and new gravity, not Gravity Changer's canonical-frame SLERP.
3. Perpendicular changes use `normalize(g0 × g1)` and 90 degrees.
4. Opposite changes use the player's current horizontal heading as the 180-degree axis, falling back to the current right axis when heading degenerates.
5. The world-space look direction is transported by the same rotation; local yaw/pitch are recomputed under the target gravity immediately.
6. Presentation is a snap with a short ease-out: 0.12 s for 90 degrees, 0.18 s for 180 degrees.
7. If a new Clinging/Reorientation change arrives during an active presentation transition, start the new presentation from the currently displayed frame instead of queuing or snapping back to a canonical intermediate.
8. First- and third-person presentation must use the same visual gravity frame.
9. Sprint-jump protection: while sprinting, moving toward the current gravity floor, and close enough to contact it on the next simulation step, reserve Space for the landing jump and do not trigger Clinging/Reorientation.
10. Clinging again grants exactly one voluntary airborne gravity change, including DOWN. Reorientation remains unlimited. Forced retirement to DOWN is not a voluntary input and remains available.
11. Remove the JSON camera timing option entirely; existing legacy config files are ignored and no new config file is created.

## Current-state findings

### Gravity Changer presentation

`GravityRotationAnimation` stores canonical start/target frame quaternions and performs smoothstep + quaternion SLERP over 1.25 s. The canonical frames include arbitrary yaw/twist choices around the gravity axis. That makes some 90-degree gravity changes interpolate 120 or 180 degrees around diagonal axes.

`GravityCoreRotationAnimation` already demonstrates a better model for perpendicular changes: a single axis from the cross product of old/new gravity and an angle around that axis.

### Gauge/representation problem

The target physical frame must eventually return to Gravity Changer's canonical target quaternion because its coordinate transforms and render hooks are defined around that frame. Merely animating to `Rmin * Qold` would end in a noncanonical frame and cause a later jump.

The clean solution is:

- immediately convert the logical yaw/pitch so that, under the canonical target gravity frame, the logical look equals the old world look transported by the minimal gravity rotation;
- visually start from `inverse(remaining minimal rotation) * Qtarget` and animate the remaining angle to zero;
- at t=0 this compensates the immediate yaw/pitch gauge change, so the composite camera/body orientation is continuous;
- at t=end the visual frame is exactly canonical and the adjusted yaw/pitch already represent the transported heading.

This is mathematically the same anchoring strategy used by Gravity Changer's core animation, generalized to 90/180-degree transitions and to Clinging's chosen 180-degree axis.

### Networking/order

`Payloads.visual(...)` currently precedes the attribute update in voluntary turns. The visual payload currently contains only target direction + sequence. Opposite 180-degree transitions need the server-selected axis derived from the authoritative pre-turn look, so the payload must carry enough transition metadata (previous direction and world axis, or directly axis + angle class) to make the client presentation deterministic.

### Logical look

`attempt(ServerPlayer, Vec3 worldLook)` already receives the normalized client camera look used for direction selection. Production should validate finiteness/nonzero length, derive the minimal transport from the server's current gravity and that look, convert the transported world look to target-local yaw/pitch using Gravity Changer `RotationUtil`, and apply those logical angles with the gravity change. Forced retirement can use the server player's current world look instead.

### Sprint-jump false activation

`JumpInputMixin` calls `ClingingClient.press()` whenever vanilla's airborne Elytra attempt fails. `GravityInput.available()` currently treats any `!onGround` state as genuinely airborne. There is no near-floor grace.

The guard should be shared client/server inside `GravityInput.available()` and depend on the current gravity vector, sprinting state, movement toward gravity, and a short swept collision probe in the gravity direction. It must not blanket-disable Clinging while sprinting.

### DOWN exception

Current code explicitly exempts `Direction.DOWN` from `AIR_CHANGE_USED`. Remove that exception only for voluntary input. `retire()` remains a separate forced cleanup path.

### Configuration

`CameraConfig` exists solely for `cameraRotationSeconds`; `CameraDurationMixin` exists solely to replace Gravity Changer's interpolation duration. Both become obsolete once Clinging owns its snap transition geometry/timing. Their JUnit test and documentation references should be removed.

## Risk areas

- packet ordering between visual-transition metadata, gravity attribute sync, and yaw/pitch teleport/rotation sync;
- composition order between Gravity Changer's visual gravity quaternion and vanilla yaw/pitch in first- and third-person rendering;
- interruption/reorientation while a prior visual transition is still active;
- preserving mouse input during the very short transition;
- not changing animations initiated by Gravity Changer itself (anchors/cores/other mods);
- sprint-jump guard must work for all six gravity directions and must not suppress legitimate early-air or high-altitude turns;
- passenger/mounted Reorientation follows a separate root-entity gravity path and must either use the same presentation metadata or be explicitly covered by a consistent rule.
