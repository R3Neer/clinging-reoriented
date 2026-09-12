# TM implementation plan — gravity snap policy

Temporary working document. Delete before merge.

## Architectural invariants

- Physical gravity is authoritative and changes immediately.
- Clinging-owned presentation never uses Gravity Changer's generic canonical-frame-to-canonical-frame path.
- Every presentation transition ends exactly on `RotationUtil.getEntityRotationQuaternion(targetGravity)`.
- A normal settled transition follows exactly the minimal 90°/180° gravity transport. An interrupted transition follows the shortest path from the currently displayed, yaw-compensated frame to the same canonical target.
- Logical pitch is invariant under gravity transport. Logical yaw receives an authoritative delta atomically with the physical gravity change.
- The client applies the authoritative yaw delta to its *current* yaw rather than an absolute stale yaw, preserving mouse movement made after the request was sent.
- Clinging presentation overrides only transitions explicitly initiated by Clinging/Reorientation; unrelated Gravity Changer animations remain untouched.
- A failed voluntary turn changes neither gravity, look, charge, nor presentation.

## Implementation checklist v2

### A. Pure transition geometry

- [ ] Add `GravityTransition` with a pure `plan(previousGravity, targetGravity, logicalYaw, logicalPitch)` result containing `yawDelta`, `TurnKind` (quarter/half), and diagnostic axis/look data for tests.
- [ ] Compute the authoritative old world look from server logical yaw/pitch + previous gravity, not from the client-supplied selection vector.
- [ ] Perpendicular directions: axis = `normalize(g0 × g1)`, angle = 90°.
- [ ] Opposite directions: axis = old world look projected onto the plane perpendicular to g0; if degenerate, use old local +X transformed to world (current right axis); angle = 180°.
- [ ] Rotate the old world look with Rodrigues/JOML axis-angle and convert it to target-local yaw with Gravity Changer `RotationUtil`.
- [ ] Preserve logical pitch exactly; assert the geometrically reconstructed target pitch agrees within epsilon.
- [ ] Normalize `yawDelta` with wrapped degrees; same-direction transition returns zero/no animation.
- [ ] Fixed timings: 120 ms quarter turn, 180 ms half turn. Easing = `1 - (1-t)^3`.
- [ ] Unit-test all 30 direction pairs and representative yaw/pitch samples, including ±90° pitch degeneracy and opposite-gravity heading axes.
- [ ] Unit-test the presentation identity for settled transitions: `Qold * Ry(yawDelta)` and `Qtarget` differ by exactly the physical gravity angle, never the old 120°/180° canonical excess.

### B. Logical orientation commit

- [ ] Keep client `Request.look` only for selecting the intended target surface; validate finite/nonzero input before `LookDirection.select`.
- [ ] Capture `previousGravity` and server logical yaw/pitch before placement/commit.
- [ ] Compute `GravityTransition.Plan` only after target direction is known and before any mutation.
- [ ] If placement succeeds, send visual transition metadata immediately before the authoritative physical commit.
- [ ] Extend the authoritative commit so gravity + optional center relocation + server yaw delta are applied together; preserve pitch and velocity exactly.
- [ ] For non-relocating turns, do not require a positional teleport merely to make the client presentation work; the custom visual payload carries the yaw delta. Server state still updates immediately.
- [ ] For relocating turns, any existing teleport packet carries the updated yaw and unchanged pitch.
- [ ] Apply the same transport policy to forced retirement to DOWN using server logical yaw/pitch. Retirement remains independent of the airborne charge.
- [ ] Do not alter foreign/unowned gravity writes.

### C. Visual transition protocol

- [ ] Replace `visual_transition_v1` with v2 carrying target direction, wrapped `yawDelta`, turn kind (quarter/half) and sequence.
- [ ] Client validates finite yaw delta, legal direction/kind and monotonic sequence.
- [ ] On receipt, capture the currently displayed gravity quaternion *before* replacing the active transition.
- [ ] Capture current local yaw, then apply `yaw += yawDelta` immediately; preserve pitch; also synchronize previous-yaw interpolation fields so vanilla camera interpolation does not add a second unwanted yaw animation.
- [ ] Compute the compensated visual start frame as `QcurrentVisual * Ry(yawDelta)` (sign verified by unit tests against `RotationUtil.rotToVec`). This keeps the composite world view continuous after the immediate local-yaw gauge change.
- [ ] Mounted Reorientation uses the same rider yaw-delta policy from rider logical yaw/pitch and old/target effective gravity; root physical gravity remains on the mount.

### D. Custom snap presentation

- [ ] Replace `CameraDurationMixin` with a `GravityRotationAnimation` return override active only while `VisualTransitions` owns that exact animation instance.
- [ ] Pending phase: before the target gravity attribute is observed, return the compensated start frame unchanged.
- [ ] When target gravity is observed, `forceSet(target)` on the upstream `GravityRotationAnimation` so its hidden state is already canonical and cannot resume an old 1.25 s animation later.
- [ ] Animate `new Quaternionf(startVisual).slerp(QcanonicalTarget, easeOutCubic(progress))` over 120/180 ms.
- [ ] For a settled transition the start construction guarantees that this SLERP is exactly the desired minimal physical turn; prove this for all direction pairs in JUnit.
- [ ] For an interrupted Reorientation transition, the same construction starts from the actually displayed frame plus the new yaw gauge compensation, then chooses the shortest quaternion path to the new canonical target. No queue and no snap-back to an intermediate canonical frame.
- [ ] At completion return exactly canonical target, release ownership, and leave upstream force-set there.
- [ ] Stale pending transitions time out/clear without affecting upstream animations.
- [ ] First-person camera, third-person model and First Person mod all continue consuming the same Gravity Changer visual quaternion.

### E. Remove configurable camera timing

- [ ] Delete `CameraConfig` and `CameraConfigTest`.
- [ ] Remove startup loading/creation of `clinging-reoriented-client.json`.
- [ ] Existing legacy JSON files are ignored; Clinging never reads or overwrites them.
- [ ] Delete `cameraRotationSeconds` documentation and any claims that camera speed is user-configurable.
- [ ] Document fixed snap timings as part of gameplay/presentation semantics.

### F. Sprint-jump intent guard

- [ ] Add shared `GravityInput.sprintLandingJumpReserved(Player)` and call it from `available()` so client precheck and server authority agree.
- [ ] Guard only when sprinting and not already grounded.
- [ ] Let `toward = deltaMovement · gravityUnit`; require `toward > 1e-4` so ascent/neutral motion is never reserved.
- [ ] Predict the next-tick gravity travel as `toward + 0.08` blocks (vanilla gravity acceleration magnitude), bounded to `[0.10, 0.60]` blocks to keep this a near-floor grace rather than a general falling lockout.
- [ ] Move a slightly deflated current AABB only along the current gravity vector by that predicted distance. Reserve the jump iff the current box is collision-free but the predicted box is not.
- [ ] This rule is gravity-direction agnostic: DOWN/UP/NORTH/SOUTH/EAST/WEST all use the same vector math.
- [ ] Tests: descending sprint near support reserved; ascending sprint not reserved; distant/clear predicted box not reserved; non-sprint not reserved; at least one sideways-gravity support case.

### G. Restore one voluntary Clinging turn

- [ ] Remove the `direction != DOWN` exception from `AIR_CHANGE_USED`.
- [ ] With Clinging only, every second voluntary airborne change is rejected regardless of target direction.
- [ ] Reorientation remains unlimited.
- [ ] Forced retirement-to-DOWN remains independent and available.
- [ ] Update server/client GameTests that encoded the alpha.9 safety-DOWN exception.

### H. Documentation/version/tests

- [ ] Update README, GUIDE, ARCHITECTURE, CONFIGURATION, COMPATIBILITY and VALIDATION where behavior changed.
- [ ] Add changelog entry and bump development version alpha.9 -> alpha.10.
- [ ] Add JUnit `GravityTransitionTest` for all geometric/presentation invariants.
- [ ] Extend server GameTests for sprint-jump reservation and no-DOWN exception.
- [ ] Extend client integration tests for quarter-turn endpoint/timing, half-turn heading preservation and interrupted Reorientation continuity.
- [ ] Extend First Person checks for the same visual endpoint/orientation contract.
- [ ] Run JUnit + server GameTests + client base + First Person + Scale Brews server/client lanes.
- [ ] Review implementation diff against this plan; if production needs a policy change, revise this plan first and re-review before changing code.
- [ ] Delete both temporary TM documents before merge.
