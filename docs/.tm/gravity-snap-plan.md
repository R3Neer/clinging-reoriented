# TM implementation plan — gravity snap policy

Temporary working document. Delete before merge.

## Architectural invariants

- Physical gravity is authoritative and changes immediately.
- Clinging-owned presentation never uses Gravity Changer's generic canonical-frame SLERP.
- Every presentation transition ends exactly on `RotationUtil.getEntityRotationQuaternion(targetGravity)`.
- The visual path is determined by a single world-space axis and angle chosen before the turn commits.
- Logical yaw/pitch are changed atomically with the physical gravity so gameplay/raycast/movement use the transported look immediately.
- Clinging presentation overrides only transitions explicitly initiated by Clinging/Reorientation; unrelated Gravity Changer animations remain untouched.
- A failed voluntary turn changes neither gravity, look, charge, nor presentation.

## Implementation checklist v1

### A. Pure transition geometry

- [ ] Add a production helper (`GravityTransition`) with pure functions:
  - [ ] direction -> unit world vector;
  - [ ] choose minimal transition axis/angle for old gravity, target gravity and current world look;
  - [ ] perpendicular: normalized cross product, 90°;
  - [ ] opposite: projected heading axis; fallback to transported/right axis if projection degenerates;
  - [ ] rotate a world vector around the chosen axis/angle;
  - [ ] convert transported world look to target-local yaw/pitch with Gravity Changer `RotationUtil`;
  - [ ] normalize yaw deterministically.
- [ ] Define fixed timings in production code: 120 ms for quarter turn, 180 ms for half turn.
- [ ] Define ease-out cubic `1 - (1-t)^3`.
- [ ] Unit-test all 30 direction pairs and representative looks, including opposite-gravity degeneracies.
- [ ] Assert all perpendicular changes are exactly 90° and all opposites exactly 180°.
- [ ] Assert transported look preserves its angle to local up (pitch invariant within epsilon).

### B. Logical orientation commit

- [ ] In voluntary `attempt`, validate and normalize input world look before using it.
- [ ] Capture `previousGravity` before selection/commit.
- [ ] Compute a transition descriptor before placement/write.
- [ ] If placement succeeds, compute target-local yaw/pitch from the transported look.
- [ ] Extend `write(...)` or add a dedicated commit method so gravity, optional center-aligned relocation and target yaw/pitch are synchronized in the same authoritative operation.
- [ ] Preserve velocity exactly.
- [ ] Apply the same look-transport policy to forced retirement to DOWN, using the server player's actual current world look.
- [ ] Do not alter foreign/unowned gravity writes.

### C. Visual transition protocol

- [ ] Replace `visual_transition_v1` with a new payload carrying at least target direction, world axis, angle class (90/180) and sequence.
- [ ] Send visual metadata immediately before the authoritative gravity/yaw commit.
- [ ] Mounted Reorientation must use the same descriptor policy based on the rider's look and root old/target gravity; presentation remains on the rider, physical gravity on the root.
- [ ] Validate payload axis is finite/unit-ish client-side before accepting it.

### D. Custom snap presentation

- [ ] Replace duration-only `CameraDurationMixin` with a Clinging-owned animation override.
- [ ] `VisualTransitions.begin(...)` stores target gravity, normalized axis, total angle, start time, sequence and transition duration.
- [ ] While owned and target gravity has committed, return:
  `Qvisual(t) = Rot(axis, -angle * (1 - easeOutCubic(t))) * Qcanonical(target)`.
- [ ] At t=0 the adjusted logical yaw/pitch + compensated visual frame must reproduce the pre-turn composite orientation.
- [ ] At t=end return exactly the canonical target quaternion and release ownership.
- [ ] If a new Clinging transition arrives while an old one is active, capture the currently displayed visual frame and construct the new presentation from that frame rather than resetting or queueing.
- [ ] Unrelated Gravity Changer visual transitions must continue to use upstream behavior.
- [ ] Ensure first-person and third-person consume the same returned visual quaternion.

### E. Remove configurable camera timing

- [ ] Delete `CameraConfig`.
- [ ] Delete `CameraConfigTest`.
- [ ] Remove startup loading of `clinging-reoriented-client.json`.
- [ ] Stop creating/reading that JSON file. Existing user files are simply ignored.
- [ ] Remove `cameraRotationSeconds` from README/docs/configuration/architecture/validation/changelog references.
- [ ] Document fixed snap timings as gameplay/presentation policy, not configuration.

### F. Sprint-jump intent guard

- [ ] Add shared `GravityInput.sprintLandingJumpReserved(Player)` (or dedicated helper).
- [ ] Guard only when sprinting.
- [ ] Require velocity component toward current gravity to be positive above a tiny epsilon.
- [ ] Compute an arbitrary-gravity collision sweep/probe from the current AABB toward the gravity direction for approximately the next-tick downward travel, with a small bounded grace margin.
- [ ] Suppress Clinging/Reorientation input only if the current box is collision-free and the swept/probed box would contact support.
- [ ] Reuse the same method in client precheck and server authoritative `GravityInput.available()`.
- [ ] Tests: near-ground descending sprint is reserved; ascending sprint is not; distant descending sprint is not; non-sprint is not; at least one sideways-gravity case.

### G. One voluntary Clinging turn again

- [ ] Change `AIR_CHANGE_USED` rule to reject every voluntary direction, including DOWN, when Clinging charge is spent and Reorientation is absent.
- [ ] Keep Reorientation unlimited.
- [ ] Keep forced retirement-to-DOWN independent of charge.
- [ ] Update server/client GameTests that previously asserted the alpha.9 DOWN safety exception.

### H. Documentation/version/tests

- [ ] Update README, GUIDE, ARCHITECTURE, CONFIGURATION, COMPATIBILITY and VALIDATION where behavior changed.
- [ ] Add changelog entry and bump development version from alpha.9 to alpha.10.
- [ ] Extend client integration test to assert snap timing/endpoint and repeated transitions.
- [ ] Extend First Person checks to cover the same presentation endpoint/orientation invariants.
- [ ] Run JUnit + server GameTests + client base + First Person + Scale Brews server/client lanes.
- [ ] Review implementation diff against this plan; if implementation needs a policy change, revise this plan first and re-review before changing code.
- [ ] Delete both temporary TM documents before merge.
