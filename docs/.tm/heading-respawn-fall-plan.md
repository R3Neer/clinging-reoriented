# TM implementation plan — heading, respawn, fall reset and snap tuning

Temporary working document. Delete before merge.

## Architectural invariants

- Physical gravity remains server-authoritative and changes immediately.
- Target gravity selection and retained navigation heading are separate inputs.
- `selectionLook` chooses the target only.
- `navigationHeading` defines orientation transport only.
- `yawDelta` is derived geometrically from old/new frame representations of heading, not from a stale absolute server yaw.
- Local pitch remains unchanged across a turn.
- Visual sequence is connection-scoped and monotonic across respawn; active animation is entity-instance-scoped.
- Every successful Clinging-owned gravity direction change starts a new vanilla fall-distance segment.
- Failed/unchanged attempts change no orientation/fall state.
- Foreign Gravity Changer transitions remain untouched.

## Implementation checklist v2

### A. Input protocol: separate selection look and navigation heading

- [ ] Extend `Payloads.Request` to a new v3 schema with `selectionLook` and `navigationHeading` world vectors.
- [ ] Client `ClingingClient.press()` captures both on the same input edge:
  - [ ] selection look from rendered camera forward;
  - [ ] heading from player's current local yaw with pitch forced to 0, transformed by current logical gravity.
- [ ] Normalize both vectors before sending.
- [ ] Server validates selection look finite/nonzero exactly as today.
- [ ] Server sanitizes heading instead of demanding perfect client geometry:
  - [ ] reject non-finite/nearly-zero raw input from direct use;
  - [ ] project raw heading onto the plane perpendicular to current gravity;
  - [ ] normalize the projected heading when non-degenerate;
  - [ ] if projection degenerates, reconstruct heading from authoritative server yaw with pitch=0 and current gravity.
- [ ] Mounted Reorientation uses the same rider heading snapshot while target selection still comes only from selection look.

### B. Pure geometry redesign

- [ ] Change `GravityTransition.plan(...)` to accept old gravity, target gravity and normalized world navigation heading.
- [ ] Perpendicular gravity: axis = normalized(oldGravity x targetGravity), angle=90°.
- [ ] Opposite gravity: axis = normalized(navigationHeading), angle=180°.
- [ ] Transport heading through that physical rotation.
- [ ] Convert original heading into old-local yaw and transported heading into target-local yaw using `RotationUtil.vecWorldToPlayer` + `vecToRot`.
- [ ] `yawDelta = wrap(targetLocalYaw - oldLocalYaw)`.
- [ ] Do not use pitch or instantaneous full look to choose the 180° axis.
- [ ] Retain diagnostic axis/old heading/transported heading fields in `Plan` for tests.
- [ ] Unit-test every ordered direction pair and representative headings.
- [ ] Explicit regressions:
  - [ ] DOWN -> UP with NORTH/EAST/SOUTH/WEST heading keeps the same world heading;
  - [ ] UP -> DOWN likewise;
  - [ ] DOWN -> NORTH with heading NORTH transports the heading toward world UP;
  - [ ] applying the same yaw delta to arbitrary local pitch reproduces the same physical rotation, proving pitch independence;
  - [ ] malformed/off-plane headings are sanitized before `plan`, never inside quaternion math.
- [ ] Keep quaternion compensated-start proofs, including arbitrary intermediate visual frames.

### C. Authoritative logical commit

- [ ] `ClingingReoriented.attempt(...)` accepts both selection look and navigation heading.
- [ ] Preserve a convenience overload for internal/tests that derives heading from the player's current yaw.
- [ ] Target selection uses only selection look.
- [ ] Transition planning uses only sanitized/fallback navigation heading.
- [ ] Apply the resulting yaw gauge delta additively on server using existing gauge helper.
- [ ] Preserve all yaw/head/body relative offsets.
- [ ] Preserve pitch and velocity.
- [ ] Keep relative-rotation relocation teleport behavior so late mouse input survives.
- [ ] Update mounted turn path to the same heading-based plan.
- [ ] Forced retirement has no client selection snapshot; derive navigation heading from authoritative current yaw with pitch=0.

### D. Respawn-safe visual epochs

- [ ] Copy `visualSequence` from old `PlayerData` to new `PlayerData` before the alive/death branch so both respawn modes preserve monotonicity.
- [ ] Do not reset client `VisualTransitions.latestSequence` on respawn; it remains connection-scoped and resets only on disconnect.
- [ ] Ensure old active animation remains associated only with the dead entity's `GravityRotationAnimation` and cannot own the replacement player's animation.
- [ ] Add server GameTest/state regression proving visual sequence survives death copy.
- [ ] Add client integration or focused client regression proving a post-respawn sequence strictly above the pre-death value is accepted and receives owned snap timing/yaw compensation.

### E. Fall-distance segmentation

- [ ] Reset vanilla `fallDistance` only when a Clinging-owned operation actually changes gravity.
- [ ] Player `writeTransition`: capture previous direction; after successful direction change call `resetFallDistance()`.
- [ ] Generic internal `write` used for direct Clinging state restoration also resets when old != new.
- [ ] `MobGravity.commitTurn`: capture old direction and reset fall distance when old != new, covering mounts, pet replay and owned restore.
- [ ] Do not reset on failed/no-space/unchanged selection.
- [ ] Tests cover:
  - [ ] successful voluntary player turn resets nonzero fall distance;
  - [ ] blocked/unchanged turn preserves it;
  - [ ] mounted/root gravity turn resets root fall distance;
  - [ ] mob replay/restore direction change resets mob fall distance.

### F. Snap tuning

- [ ] Change fixed quarter duration 120 ms -> 180 ms.
- [ ] Change fixed half duration 180 ms -> 240 ms.
- [ ] Replace ease-out cubic with ease-out quadratic `1-(1-t)^2`.
- [ ] Rename helper/tests/docs from cubic to quadratic.
- [ ] Assert monotonic easing and representative fractions (`t=.5 -> .75`, `t=1 -> 1`).
- [ ] Client test verifies owned quarter turn is visibly incomplete at 120 ms and canonical by 180 ms.
- [ ] Half-turn endpoint verified at 240 ms.
- [ ] Unowned Gravity Changer still retains upstream 1.25 s behavior.

### G. Protocol/version compatibility and documentation

- [ ] Bump request payload identifier so alpha.10 peers fail closed rather than decode a changed schema.
- [ ] Visual transition payload can remain v2 if its schema is unchanged.
- [ ] Bump development version alpha.10 -> alpha.11.
- [ ] Update README/GUIDE/ARCHITECTURE/VALIDATION/CHANGELOG with heading semantics, respawn fix, fall reset and 180/240 ms quadratic snap.
- [ ] Keep configuration docs explicit that there is still no Clinging-owned timing JSON.

### H. Iterative verification

- [ ] Review implementation diff against this frozen plan before CI.
- [ ] If a required behavior change emerges, revise and re-review plan before production code.
- [ ] Run build + JUnit + required server GameTests.
- [ ] Run default client GameTests.
- [ ] Run First Person 2.7.2 lane.
- [ ] Run optional Scale Brews server compatibility lane.
- [ ] Run optional Scale Brews client load lane.
- [ ] Perform second adversarial diff review after green CI.
- [ ] Delete both temporary TM documents.
- [ ] Validate exact clean HEAD again.
- [ ] Merge only exact validated head SHA, then verify post-merge `main` CI.
