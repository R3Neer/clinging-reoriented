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

## Implementation checklist v1

### A. Input protocol: separate selection look and navigation heading

- [ ] Extend `Payloads.Request` to v3 with `selectionLook` and `navigationHeading` world vectors.
- [ ] Client `ClingingClient.press()` captures both on the same input edge:
  - [ ] selection look from rendered camera forward;
  - [ ] heading from player's current local yaw with pitch forced to 0, transformed by current gravity.
- [ ] Keep both vectors normalized before sending.
- [ ] Server validates selection look finite/nonzero exactly as today.
- [ ] Server validates heading finite/nonzero and approximately perpendicular to current gravity.
- [ ] If heading validation fails, reconstruct heading from authoritative server yaw with pitch=0 instead of rejecting the entire turn.
- [ ] Mounted Reorientation uses the same rider heading snapshot while target selection still comes from selection look.

### B. Pure geometry redesign

- [ ] Change `GravityTransition.plan(...)` to accept old gravity, target gravity and normalized world navigation heading.
- [ ] Perpendicular gravity: axis = normalized(oldGravity x targetGravity), angle=90°.
- [ ] Opposite gravity: axis = normalized(navigationHeading), angle=180°.
- [ ] Transport heading through that physical rotation.
- [ ] Convert original heading into old-local yaw and transported heading into target-local yaw using `RotationUtil.vecWorldToPlayer` + `vecToRot`.
- [ ] `yawDelta = wrap(targetLocalYaw - oldLocalYaw)`.
- [ ] Do not use pitch or instantaneous full look to choose the 180° axis.
- [ ] Retain diagnostic axis/transported heading fields in `Plan` for tests.
- [ ] Unit-test every ordered direction pair and representative headings.
- [ ] Explicit regressions:
  - [ ] DOWN -> UP with NORTH/EAST/SOUTH/WEST heading keeps the same world heading;
  - [ ] UP -> DOWN likewise;
  - [ ] DOWN -> NORTH with heading NORTH ends with world look/heading transported toward UP;
  - [ ] yaw delta remains identical for the same heading regardless of pitch.
- [ ] Keep quaternion compensated-start proofs, including arbitrary intermediate visual frames.

### C. Authoritative logical commit

- [ ] `ClingingReoriented.attempt(...)` accepts both selection look and navigation heading.
- [ ] Target selection uses only selection look.
- [ ] Transition planning uses only validated/fallback navigation heading.
- [ ] Apply the resulting yaw gauge delta additively on server using existing gauge helper.
- [ ] Preserve all yaw/head/body relative offsets.
- [ ] Preserve pitch and velocity.
- [ ] Keep relative-rotation relocation teleport behavior so late mouse input survives.
- [ ] Update mounted turn path to the same heading-based plan.
- [ ] Forced retirement has no client selection snapshot; derive navigation heading from authoritative current yaw with pitch=0.

### D. Respawn-safe visual epochs

- [ ] Copy `visualSequence` from old `PlayerData` to new `PlayerData` for both alive-copy and death respawn.
- [ ] Do not reset client `VisualTransitions.latestSequence` on respawn.
- [ ] Ensure old active animation remains associated only with the dead entity's animation object and cannot own the replacement player's animation.
- [ ] Add server GameTest/unit-level state regression proving visual sequence survives death copy and next emitted sequence is monotonic.
- [ ] Add client integration regression: establish a high accepted sequence, replace/respawn player state, accept the first subsequent transition rather than falling back to upstream timing.

### E. Fall-distance segmentation

- [ ] Reset vanilla `fallDistance` only when a Clinging-owned operation actually changes gravity.
- [ ] Player `writeTransition`: capture previous direction; after successful direction change call `resetFallDistance()`.
- [ ] Generic internal `write` used for direct Clinging state restoration should also reset when direction changes.
- [ ] `MobGravity.commitTurn`: reset fall distance when old != new, covering mounts, pet replay and owned restore.
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
- [ ] Assert monotonic easing and representative timing fractions.
- [ ] Client test verifies owned quarter turn is not complete at 120 ms and is canonical by 180 ms.
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
