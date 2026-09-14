# Validation

## 0.1.0-alpha.15 Shulker Charge release-candidate validation — 2026-09-14

Alpha.15 layers Shulker Charge plus the pet breadcrumb-pursuit fix onto the already published alpha.14 Gravity Fall baseline. The Shulker campaign was developed on `feature/shulker-charge` with temporary SPEC/PLAN/WORKFLOW authority through S05, then synchronized with current `main` before the adversarial gate was allowed to close.

### Shulker Charge invariants

Automated coverage verifies the stable feature contract:

- melee and arrow interception, including dispenser arrows, can materialize exactly one Charge;
- mixed arrow/melee races cannot duplicate the drop;
- shield/ordinary impact/expiry paths do not mint Charges;
- player and real dispenser launch use the same exact vanilla `SHULKER_BULLET` entity type and consume one item;
- direct-ray Target Blocks win acquisition; assisted entities/blocks remain bounded by the 32-block / 15-degree selection contract;
- initial living-target acquisition requires line of sight;
- a valid lock is sticky and is not replaced by a later better candidate;
- invalid/dead/removed/dimension-transferred targets are cleared and reacquired from the Charge's current position while preserving original intent;
- targetless Charges continue cardinal free flight and retry instead of freezing or inventing curved homing;
- concurrent Charges keep independent target/retry/capture state;
- Target Block routing ends in a real projectile collision/redstone response;
- vanilla shulker-duplication semantics remain available because the runtime entity type is not replaced.

Client snapshot validation adds dedicated checkpoints for the inventory icon, first-person held model, third-person held model, projectile renderer and fixed 3D presentation. These coexist with the existing Gravity Fall/First Person/Fresh Animations snapshot matrix.

### Adversarial campaign and failure classification

The first S05 failure in run **#701** occurred only in the optional Scale Brews lane: `freeFlightAutomaticallyAcquiresTargetThatAppearsLater` reached its late checkpoint with a projectile that had only ticked four times. The same freeze signature had already identified an under-simulated GameTest chunk boundary. The fixture was hardened to keep the entire intended 32-block flight corridor plus margin at `ENTITY_TICKING`; production remained unchanged and no Scale-specific branch was introduced.

Run **#706** then failed before compilation because Modrinth returned HTTP 503 while resolving multiple required dependencies. That failure was classified as **environment**, not implementation/test, and did not trigger code changes.

Run **#710** (`34895769482`) passed the full matrix before the branch was synchronized with current `main`.

After merging the published alpha.14 baseline and subsequent pet breadcrumb-pursuit fix into the feature branch, run **#714** (`34897063938`) passed the complete post-merge gate on commit `a16d16faaa2cbd3c4b51678f08310ebc9a8bf681`:

- Gradle build and JUnit;
- required server GameTests, including the Shulker adversarial holdouts;
- default client GameTests;
- First Person 2.7.2 + Not Enough Animations 1.12.4;
- Scale Brews beta.5 isolated server and client lanes;
- Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2;
- semantic screenshot validation;
- artifact/log/report retention.

That post-merge run is the S05 no-change gate. The release-prep/canonicalization commit must itself pass the same matrix before integration, and `main` must then pass it again.

### Asset provenance

The Shulker Charge 16x16 GUI icon and project-authored 3D geometry are original GPL-3.0-or-later project assets. Editable icon source remains under `docs/art/shulker-charge/`. The 3D item model references `minecraft:entity/shulker/spark` at runtime; the Mojang texture itself is not redistributed.

## Unreleased pet breadcrumb pursuit — local validation 2026-09-14

The rebased alpha.14 tree passed locally on Java 25 and Minecraft 26.2: **75/75 JUnit tests** and **95/95 required server GameTests**. New coverage exercises all six movement-plane projections, the vanilla close-distance dead zone, real scheduled wolf traversal to an airborne breadcrumb, bounded arrival, center-aligned grounded replay, ballistic goal release, Gravity Changer navigation replacement, ordered-queue preservation, sitting pause/resume, effect-free behavior and external teleport invalidation.

This evidence validates deterministic server logic and integration in the GameTest environment. It does not replace live multiplayer/gameplay observation of a naturally equipped pet following a player through several gravity changes.

## 0.1.0-alpha.14 release-candidate validation — 2026-09-14

Alpha.14 is the Gravity Fall control/compatibility campaign. It preserves alpha.13's world-momentum and absorbed-collision foundations while adding a 500 ms landing manoeuvre, full-sphere camera look, bounded body aerodynamics/W air-diving, Elytra-style airflow audio, generic fluid context, world-vertical water/climbable policies, First Person look-down hardening, directional mace height and server flight-safety fences.

### Integration strategy

Each risky subsystem was developed through a separate TM branch and required a full matrix before integration. Conflict-dirty branches were not force-merged; functionality was rebuilt on the current `main` when neighbouring policy had changed.

Key green evidence includes:

- generic fluid context: run **#655** (`34882827200`);
- 500 ms landing manoeuvre: run **#664** (`34884020691`);
- First Person look-down hotfix: run **#668** (`34884904594`);
- directional mace geometry: run **#669** (`34885200233`);
- full-sphere camera TM: run **#671** (`34885705114`);
- aerodynamics + air-diving on fluid/landing/First-Person/mace main: run **#678** (`34886274878`);
- final full-sphere camera rebuilt on the post-aerodynamics main: run **#684** (`34887380354`), head `91bb5d0859f088fc0e6eb4fd701903ec0a085350`, merged as PR #21.

Run #684 passed the complete matrix before merge: build/JUnit, required server GameTests, default client, First Person, Scale Brews server/client, Fresh Animations and semantic snapshot validation. The published alpha.14 release commit was then validated again on `main`, and its release workflow consumed that successful run's artifact rather than rebuilding separately.

### Camera and landing

Client holdouts cross +120 and -120 degrees of pitch, complete a 360-degree vertical loop, compare entity look with the rendered camera quaternion beyond both poles, and verify that Gravity Fall exit returns to vanilla's +/-90-degree pitch representation **without changing the look vector**.

Landing tests use the shared `LandingTiming` contract: 10 ticks / 500 ms for local camera LAND and Gravity Fall BODY_LANDING. Predictor/commitment validity, input rejection, support revalidation and partial-frame cancellation remain covered.

First Person testing keeps the real camera independent while applying the macro body root through a blended camera/body pivot. Baseline and Gravity Fall look-down screenshots exercise steep angles where the old body-center pivot clipped.

### Body aerodynamics and air-diving

Pure tests verify the 35-degree body look deadzone, 7.5-degree/tick follow cap, drag factor bounds and zero extra drag when streamlined.

W steering tests verify that momentum is redirected by at most 6 degrees/tick, scales with the positive velocity/look dot product, preserves speed before drag and gives zero authority for perpendicular/backward gaze or zero input. The server remains authoritative through bounded monotonic world-look/forward samples.

The adversarial client holdout continues to require the body to remain velocity-owned while allowing the new bounded look-follow offset. Camera continuity is checked independently.

### Sound and flight safety

Gravity Fall fast-air sound has unit coverage for its Elytra-derived speed-squared volume/high-speed pitch mapping and 10-tick admission fade semantics. Runtime ownership ends the loop on Gravity Fall release or real Elytra flight.

Flight-safety tests cover the 3.92 blocks/tick directional speed cap, loaded-frontier hold/resume and hard-boundary recovery. Safety intervention clears impact state so the rescue cannot become synthetic damage.

### Fluids, water and climbables

Server GameTests exercise generic non-empty fluid intersection rather than only water/lava. A submerged entity touching a solid seabed remains in fluid context and cannot regain Clinging support or start a landing reorientation.

Water tests preserve the passive 250 ms double-Space gesture and verify world +Y/-Y ascent/descent under owned non-DOWN gravity.

Climbable tests cover DOWN vanilla behaviour, lateral-gravity ignore and mirrored UP world-Y climbing.

### Impact and directional mace

Alpha.13 absorbed-collision impact tests remain in the suite: high-speed late turns still damage, genuine braking can reduce damage, tangential motion stays harmless and multi-axis absorption is charged once.

Directional mace GameTests verify literal fall geometry independently from vanilla/Gravity Changer `fallDistance`: four EAST blocks count as four, a gravity change starts at zero, one SOUTH block does not arm smash, two do, and changing gravity strength does not alter those two geometric blocks. Mixin coverage targets the actual 26.2 `LivingEntity`/`Entity` field owners used by mace bytecode.

### Compatibility matrix

Every release-gating run executes:

- Gradle build and JUnit;
- required server GameTests;
- default client GameTests;
- First Person 2.7.2 + Not Enough Animations 1.12.4;
- Scale Brews beta.5 isolated server and client lanes;
- Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2;
- semantic screenshot validation;
- artifact retention for JARs, logs, XML, reports and screenshots.

CI preserves default, First Person and Fresh Animations screenshot sets. Alpha.15 extends the default set with Shulker Charge inventory/held/projectile/fixed-3D checkpoints while retaining the alpha.14 camera/body evidence.

## Packaging gate

A prerelease is published only from the **exact `main` commit whose complete `Build and test` run succeeded**. The release automation downloads the regular and sources JARs from that exact workflow artifact, records SHA-256 digests in the release notes and creates the prerelease tag against that commit. It never performs a second release build.

Production output must not contain GameTest classes, dependency JARs, temporary planning files or raw validation logs. Temporary Shulker Charge SPEC/PLAN/WORKFLOW documents are removed from the release tree during S06 canonization.

## Manual QA still required

- Dedicated multiplayer with realistic latency around rapid Reorientation, air-diving input, landing commitment, Shulker Charge capture/relaunch/reacquisition, fluid entry/exit, teleport and tracking boundaries.
- Human motion-comfort/readability during repeated full-sphere look, gravity reversals and 500 ms 90/180-degree landing manoeuvres.
- Human tuning assessment of the 35-degree neck cone, 7.5-degree body follow, 6-degree W redirect and posture drag.
- Audio feel at low/high Gravity Fall speeds and handoff to real Elytra.
- Long full-pack sessions with First Person + Fresh Animations together, plus mount/pet routes, Shulker Charge use and modded fluids.
- Uneven-terrain sprint-jump feel with normal and high jump-strength modifiers.

Automated assertions and snapshots are evidence, not a substitute for human gameplay acceptance.

## Historical release validation

### 0.1.0-alpha.13 — 2026-09-14

Alpha.13 introduced retained free-flight camera, velocity-owned Gravity Fall body presentation, physically committed landing snaps, absorbed-collision impact damage and the landing-surface provider API. Its final matrix included 86 server GameTests plus default client, First Person, Scale Brews and pinned Fresh Animations lanes.

### 0.1.0-alpha.12 and earlier

Alpha.12 added underwater double-Space arbitration, gravity-relative recharge, jump-aware sprint-landing reservation and tracked mount/pet snaps. Alpha.11 established selection-vs-navigation intent and monotonic visual epochs. Earlier alphas established ownership, recovery and the one-turn Clinging budget.
