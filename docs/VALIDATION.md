# Validation

## 0.1.0-alpha.15 release-candidate validation — 2026-09-14

Alpha.15 is a focused follow-up to alpha.14: player Gravity Fall mechanics remain unchanged, while tame-pet owner-follow is upgraded from proximity-triggered breadcrumb replay into a real gravity-relative pursuit route.

### Pet breadcrumb pursuit

The deterministic pet suite covers:

- all six gravity directions / all three movement-plane projections;
- activation inside vanilla's close owner-follow start dead zone when an eligible breadcrumb is pending;
- a real scheduled wolf AI traversal to an airborne breadcrumb projection;
- bounded arrival using the follow goal's stop-distance semantics;
- replay at the projected point rather than remote simultaneous rotation;
- center-aligned collision-preflighted replay when old support blocks an in-place turn;
- release of `FollowOwnerGoal` while unsupported so directional gravity owns the ballistic phase;
- preservation and ordered continuation of later breadcrumbs after internal replay placement;
- refresh of Gravity Changer's replaced `PathNavigation` inside the existing follow goal;
- sitting pause/resume without consuming the pending step;
- effect-free pets retaining vanilla dead-zone behaviour;
- external teleport invalidating the authored route;
- stale, wrong-dimension and lifecycle-invalid breadcrumbs failing closed;
- Clinging one-air-turn budget versus Reorientation multi-turn replay.

The rebased implementation had already passed local Java 25 / Minecraft 26.2 evidence of **75/75 JUnit tests** and **95/95 required server GameTests** before this release-prep branch. The exact alpha.15 `main` commit must additionally pass the complete CI matrix below before publication.

### Full compatibility matrix

Every release-gating `Build and test` run executes:

- Gradle build and JUnit;
- required server GameTests;
- default client GameTests;
- First Person 2.7.2 + Not Enough Animations 1.12.4;
- Scale Brews beta.5 isolated server and client lanes;
- Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2;
- semantic screenshot validation;
- artifact retention for JARs, logs, XML, reports and screenshots.

Alpha.14's full-sphere +120/-120/360 camera holdouts, steep First Person snapshots, Gravity Fall/landing snapshots, fluid/world-control tests, directional mace tests, aerodynamics/air-diving tests and impact/lifecycle adversarial suite remain part of that matrix. Alpha.15 adds the stronger pet route coverage above rather than replacing earlier gates.

### Packaging gate

The prerelease is published only from the **exact successful `main` commit**. The release workflow downloads the regular and sources JARs from that exact CI artifact, computes SHA-256 digests and tags the same commit. It does not rebuild for release.

## 0.1.0-alpha.14 release validation — 2026-09-14

Alpha.14 introduced the 500 ms landing manoeuvre, full-sphere Gravity Fall camera, bounded body aerodynamics/W air-diving, Elytra-style airflow audio, generic fluid context, world-vertical water/climbable rules, First Person look-down hardening, directional mace geometry and server flight-safety fences.

Key green integration evidence included fluid run #655, landing #664, First Person #668, mace #669, full-sphere #671, aerodynamics #678 and final combined full-sphere/aerodynamics run #684. The exact release commit `74fa2b5c505ef872d58eef4cab04feaacbab1f8f` passed `main` run #703 (`34888645528`) and was published from that exact artifact as `v0.1.0-alpha.14`.

## Manual QA still required

- naturally equipped pets following owners through several gravity turns across uneven real terrain;
- dedicated multiplayer latency around breadcrumb creation, pursuit, replay and owner/pet tracking;
- transitions involving water, moving surfaces, vehicles and long queued routes;
- continued motion-comfort tuning for player full-sphere look/air-diving and 500 ms landings;
- long full-pack First Person + Fresh Animations + Scale sessions.

Automated assertions and screenshots are evidence, not a substitute for human gameplay acceptance.
