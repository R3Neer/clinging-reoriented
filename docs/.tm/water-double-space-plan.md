# TM implementation plan — underwater input, jump-aware landing grace and entity snaps

Temporary working document. Delete before merge.

## Frozen behavioral policy

- Outside water, Space behavior remains as alpha.11 except for stronger-jump-aware sprint-landing reservation.
- In water, Vanilla owns ordinary Space-to-ascend behavior.
- A gravity turn is requested only on the second rising edge of Space after a release, within 250 ms.
- The water detector never consumes or rewrites Vanilla input.
- A detected double-tap gesture consumes its pair even if the gravity attempt is later rejected.
- Water never recharges Clinging; only real feet-side support on a solid block does.
- Sprint-jump reservation remains gravity-relative/support-predicted, with near-landing grace scaling upward with effective jump power.
- Normal jump power preserves alpha.11's one-tick guard exactly.
- Every Clinging/Reorientation-owned gravity change for mounts/pets uses the same minimal 180/240 ms quadratic snap policy as players; foreign Gravity Changer changes remain untouched.

## A-E. Underwater input and recharge

- [x] Pure 250 ms water double-tap detector with release/hold/context semantics.
- [x] Scope detector to exact `LocalPlayer` + `ClientLevel` identity.
- [x] Air path refuses water; underwater path passively observes keyJump without consuming it.
- [x] Shared request construction retains selection look + navigation heading semantics.
- [x] Server authority and Clinging/Reorientation charge rules unchanged.
- [x] Server water/recharge regressions added.
- [x] Real-client water fixture covers held ascent, double tap, repeated Reorientation and spent-Clinging rejection while ascent remains active.

## F. Jump-power-aware sprint-landing grace

- [x] Add package-visible helpers in `GravityInput` for effective jump power / landing grace.
- [x] Effective jump power uses `Attributes.JUMP_STRENGTH` when present, otherwise 0.42, plus `Player.getJumpBoostPower()`.
- [x] Invalid/non-finite derived values fall back safely.
- [x] `landingGraceTicks = clamp(effectiveJumpPower / 0.42, 1.0, 3.0)`.
- [x] Preserve alpha.11 exactly at `h=1`.
- [x] Extend predicted travel as `toward*h + acceleration*h*(h+1)/2`, clamp max to `0.60*h`.
- [x] Collision-test the complete swept AABB between current and predicted body.
- [x] Keep original gates: sprinting, airborne, descending, current body clear, real predicted support.
- [x] Add tests for baseline, Jump Boost I/II monotonicity, boosted-only reservation, ascending/no-support cases, sideways gravity, gravity-strength interaction and 3-tick cap.

## G. Entity visual snap generalization

- [ ] Add `GravityTransition.rebase(physicalPlan, entityHeading)`:
  - [ ] same previous/target/kind/axis as the physical plan;
  - [ ] project/normalize the supplied entity heading onto the old gravity plane;
  - [ ] transport it through the physical plan's axis-angle;
  - [ ] derive an entity-specific yaw delta from old/target local gauges;
  - [ ] unit-test multiple headings rebased onto one quarter and one half-turn physical plan.
- [ ] Add entity-scoped client payload, separate from player `visual_transition_v2`:
  - [ ] entity id + UUID;
  - [ ] target direction;
  - [ ] yaw delta;
  - [ ] turn kind;
  - [ ] per-mob monotonic visual sequence.
- [ ] Extend `MobGravity.State` with `visualSequence`.
- [ ] Server sender targets `PlayerLookup.tracking(entity)` and explicitly includes all ServerPlayer passengers in the root hierarchy.
- [ ] Send entity visual payload only for actual direction changes and only after collision preflight succeeds, immediately before physical commit.
- [ ] Client resolves id+UUID against current `ClientLevel`; stale/id-reused packets fail closed.
- [ ] `VisualTransitions` keeps current connection-scoped sequence gate for local-player payloads and a separate per-entity-UUID sequence map for tracked entities.
- [ ] Entity `begin` path captures displayed frame, applies entity yaw gauge, enrolls that entity's Gravity Changer animation, and then reuses the exact existing override/easing code.
- [ ] `clear()`/disconnect clears both local and tracked sequence state.

### Mounted turns

- [ ] Rider creates the existing physical `GravityTransition.Plan` from rider selection heading.
- [ ] Rebase root mount's own heading onto the rider plan's same axis/kind.
- [ ] Add `MobGravity.borrow(..., physicalPlan)` overload so the root uses the rebased plan before commit.
- [ ] Apply root server yaw/body/head gauge delta before/with commit; rider keeps existing player-specific yaw delta.
- [ ] Assert root and rider share the same physical axis/kind for 180-degree mounted turns.
- [ ] Mount root receives entity snap for all tracking clients/passenger clients; rider still receives its existing local-player snap.

### Pets / mob-owned changes

- [ ] Pet replay builds an ordinary plan from pet current heading and target, sends entity visual transition, applies yaw gauge, then commits.
- [ ] Borrow without an explicit rider plan, owned restore/retirement and prior-frame restoration use the mob's own heading as the physical plan.
- [ ] Same-direction writes do not send a visual transition or mutate yaw.
- [ ] External/foreign gravity writes never gain Clinging visual ownership.

### Visual tests

- [ ] Add geometry/JUnit coverage for rebase common-axis semantics.
- [ ] Add real-client or focused client GameTest that enrolls a non-player entity animation and proves quarter turn is incomplete at old upstream timing assumptions but canonical by 180 ms.
- [ ] Verify foreign/unowned mob Gravity Changer animation still uses upstream timing.
- [ ] Add server/GameTest coverage that pet replay / mounted borrow increments/sends only owned transition state after successful preflight.

## H. Documentation/version

- [x] Bump development version to `0.1.0-alpha.12`.
- [x] Update underwater docs/config/validation/changelog.
- [ ] Update docs for jump-power-aware landing grace / Leaping behavior.
- [ ] Update docs for mount/pet snap parity and tracked-entity presentation ownership.

## I. Iterative verification

- [x] Initial water plan reviewed twice.
- [x] Jump-power plan reviewed twice before production changes.
- [ ] Re-review entity-snap extension until two consecutive reviews require no policy change before production networking/mob changes.
- [ ] Implement remaining work only on `tm/water-double-space`.
- [ ] Review complete diff adversarially against this revised frozen plan.
- [ ] Run build + JUnit + server GameTests.
- [ ] Run default client GameTests.
- [ ] Run First Person 2.7.2 lane.
- [ ] Run optional Scale Brews server compatibility lane.
- [ ] Run optional Scale Brews client load lane.
- [ ] Perform second adversarial review after green CI.
- [ ] Delete temporary TM documents.
- [ ] Re-run complete CI on exact clean HEAD.
- [ ] Merge only exact validated SHA, then verify post-merge `main` CI.
