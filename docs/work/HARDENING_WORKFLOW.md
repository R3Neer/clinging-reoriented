# Temporary TM workflow for Clinging hardening

Status: **TEMPORARY — DELETE ON SUCCESS**.

This is a lighter-weight adaptation of Scale Brews' iterative workflow. It keeps adversarial testing and explicit failure classification without reproducing the collision project's much larger formal test program.

## 1. Per-sprint cycle

```text
SCOPE + REQUIREMENTS
      ↓
CURRENT-STATE ANALYSIS
      ↓
IMPLEMENTATION PLAN ↺
      ↓
ADVERSARIAL MODEL ↺
      ↓
IMPLEMENTATION ↺
      ↓
TARGETED + ADVERSARIAL TESTS
      ↓
FAILURE CLASSIFICATION
      ↓
FULL REVIEW ↺
      ↓
NO-CHANGE PASS
```

A `↺` phase is reviewed after completion. If the review changes the phase, review starts again on the revised version.

## 2. Planning rules

Each sprint must state:

- included CR/CNFR requirements;
- one primary technical thesis;
- affected production classes and external contracts;
- explicit exclusions;
- dependency on Scale Brews or released compatibility versions;
- observable acceptance conditions.

Do not combine unrelated ownership, networking and presentation refactors merely because they touch the same class.

## 3. Adversarial model

Before coding, consider at minimum when applicable:

- exact boundaries and both sides of them;
- stale/replayed/duplicated state;
- external ownership changes mid-operation;
- partial failure after one mutation;
- nested passenger hierarchies;
- save/load, death, teleport and dimension discontinuities;
- optional mod absent/present/old/new;
- non-DOWN frames and DOWN equivalence;
- NaN/invalid network vectors and bounded-work limits;
- double application when two compatibility layers are installed.

Reserve at least one concrete adversarial scenario per sprint until after implementation, so the implementation is not written solely to its visible tests.

## 4. Failure classification

Every failed test/review is classified before patching:

1. implementation bug;
2. plan defect;
3. requirement/design defect;
4. test defect;
5. environment/version/evidence problem.

Return to the corresponding phase instead of patching until green without understanding the failure.

## 5. Validation levels

Use the smallest useful level first, then the integration level required by the behavior:

- unit/helper tests for pure selection/state logic;
- server GameTests for ownership, recovery, passengers and effect lifecycle;
- real client GameTests for input/camera/First Person behavior;
- real optional-mod fixture for Scale Brews integration;
- CI clean build on the exact candidate commit.

Adversarial coverage is mandatory for the new failure mode, but exhaustive mutation/fuzz infrastructure is optional unless a sprint exposes a sufficiently risky input surface.

## 6. Finalization

After all sprints:

1. review final code and tests against every CR/CNFR;
2. run complete applicable CI/compatibility suites;
3. update permanent docs according to their existing scope;
4. perform a final full no-change review;
5. delete all `docs/work/*` temporary artifacts;
6. merge normally to `main` only after the cleaned candidate remains green;
7. run/verify `main` CI;
8. publish and verify the requested prerelease from the exact validated target.

No force-push, history rewrite, tag or release occurs during planning/implementation sprints.
