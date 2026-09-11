# Clinging: Reoriented hardening plan

Status: **TEMPORARY — DELETE ON SUCCESS**.

Branch target for implementation: dedicated Clinging work branch, never direct development on `main`.

## C0 — planning and baseline

- [x] Audit current docs against code.
- [x] Identify correctness/ownership defects and Scale migration boundary.
- [x] Converge temporary requirements, analysis, workflow and plan.
- [ ] Capture exact baseline commit/CI before implementation starts.

## C1 — bounded player retirement

**Requirements:** CR-001..003, CR-013, CNFR-002..004, CNFR-006.

- [ ] Replace remote safe-position/full-column recovery with deterministic ≤4-block local search.
- [ ] Keep movement possible while retirement is pending; block new voluntary turns.
- [ ] Remove/ignore obsolete persisted `lastSafeDown` semantics.
- [ ] Add boundary, pending/retry and lifecycle adversarial cases.
- [ ] Review until a complete pass produces no changes.

## C2 — explicit mob gravity ownership

**Requirements:** CR-004..006, CR-010, CR-013.

- [ ] Define ownership/loan state transitions before implementation.
- [ ] Preserve external gravity that predates or supersedes Clinging.
- [ ] Preserve prior external mount frame across rider loan when appropriate.
- [ ] Handle effect-owning mount + borrowed rider interactions deterministically.
- [ ] Extend external-write detection to relevant managed living entities without intercepting unrelated physics globally.
- [ ] Add adversarial ownership/lifecycle cases and converge review.

## C3 — atomic mount/passenger recovery

**Requirements:** CR-007, CR-003, CNFR-003.

- [ ] Build one complete-hierarchy preflight for each candidate.
- [ ] Commit relocation/gravity only after full preflight success.
- [ ] Guarantee zero mutation on failed candidates, including nested passengers.
- [ ] Add root-fits/passenger-fails and nested holdout cases.

## C4 — visual ownership

**Requirements:** CR-008..009, CR-013, CNFR-005.

- [ ] Audit exact Gravity Changer animation initiation/completion hooks for the pinned version.
- [ ] Introduce a bounded client-owned visual-transition marker/epoch tied to Clinging transitions.
- [ ] Gate camera duration override by that marker.
- [ ] Gate First Person offset correction by the same ownership semantics.
- [ ] Prove unrelated Gravity Changer/Anchor transitions retain upstream behavior.

## C5 — temporary MovingSurface anti-replay hardening

**Requirements:** CR-011..012, CNFR-001..004.

- [ ] Add bounded causal sample identity/order/age sufficient to reject replay of old valid samples.
- [ ] Consume accepted reference intervals once.
- [ ] Invalidate fences at support/revision/teleport/dimension discontinuities.
- [ ] Retain current collision/distance bounds.
- [ ] Explicitly avoid a new permanent receipt/epoch architecture.
- [ ] Add stale-authentic-sample, duplicate and reorder adversarial cases.

## C6 — one generic mounted-gravity contract

**Requirements:** CR-015..016, CNFR-007.

- [ ] Treat all eligible non-player `LivingEntity` root vehicles through the same `MountedGravity`/`MobGravity` path.
- [ ] Audit the mounted path for hidden Scale/Tiny assumptions; none may be introduced to replace the current generic contract.
- [ ] Remove `ScaleFlightGravityMixin`, `ScaleWolfGravityMixin` and `ScaleChickenGravityMixin` from Clinging once the final diff is prepared; their semantics belong to Scale-generated mechanics, not Clinging mount policy.
- [ ] Do not add Scale version checks, Tiny capability markers or old/new Scale shim negotiation.
- [ ] Keep future Scale G7 anatomical-contact migration separate from mounted gravity.
- [ ] Add/retain generic mount fixtures proving the contract without Scale classes.

## C7 — aggregate validation and permanent docs

- [ ] Run base server tests/build without Scale.
- [ ] Run real client GameTests.
- [ ] Run generic mounted-gravity regressions using non-Scale living mounts.
- [ ] Run First Person compatibility lane where applicable.
- [ ] Do not make unfinished Scale Brews a prerelease validation dependency.
- [ ] Review README, GUIDE, ARCHITECTURE, CONFIGURATION, COMPATIBILITY, VALIDATION and CHANGELOG against actual final behavior.
- [ ] Remove/update any permanent claim that this prerelease certifies unfinished Scale/Tiny Mount integration.
- [ ] Record only executed evidence in VALIDATION.

## C8 — cleanup, merge and prerelease

- [ ] Complete a full final review with zero changes.
- [ ] Delete every `docs/work/*` temporary file.
- [ ] Re-run candidate CI after cleanup.
- [ ] Merge normally to `main` after green candidate review.
- [ ] Verify green `main` CI.
- [ ] Bump next prerelease version consistently (expected `0.1.0-alpha.7` unless prior release state changes before execution).
- [ ] Publish prerelease in the same style as alpha.6 from the exact validated target.
- [ ] Verify tag target, regular JAR asset, digest/size and release metadata.

## Dependency graph

```text
C0
├── C1
├── C2 → C3
├── C4
├── C5
└── C6

C1+C3+C4+C5+C6 ──→ C7 ──→ C8
```

C1/C2/C4/C5/C6 may be developed as separate small commits/sprints. C3 depends on the final ownership model from C2. Scale's own Tiny Mount gravity work proceeds independently and is not a Clinging prerelease dependency.
