# Validation

## 0.1.0-beta.4 — gravity navigation and gamefeel campaign

The `tm/gravity-navigation-gamefeel-beta4` campaign changed player gamefeel and added general gravity-aware locomotion for mobs, then shipped as **0.1.0-beta.4**. Beta.5 keeps those semantics and adds only the Gravity Charge Peaceful-mode hotfix documented in the changelog.

### Campaign invariants

Automated coverage for the current development architecture preserves these rules:

- player landing and mob transition forecasts share `TrajectoryPrediction`, `AirMotion` and `LandingSurfaces` rather than maintaining incompatible physics models;
- landing acquisition may begin up to 40 ticks ahead, while visible LAND/BODY_LANDING remains at most 10 ticks and must be backed by a current physical prediction inside that final window;
- Gravity Fall body attitude is persistent; gaze drives bounded body intent, velocity provides only weak stabilization, and anisotropic drag replaces the old W-specific airborne redirect without creating energy;
- owned swimming uses camera-relative WASD plus world +Y/-Y Space/Shift, while unsupported underwater presentation converges to world-up and supported presentation converges to support-up without rewriting logical gravity;
- ordinary navigation is always tried before gravity planning;
- pet follow is history-free: no runtime owner breadcrumb queue/replay drives locomotion;
- general follow/chase/flee use the same gravity locomotion machinery rather than species-specific route tables;
- a local grounded gravity plan is bounded to one mirror path and at most four launch nodes × five alternate gravities (`<=20` transition forecasts);
- new grounded gravity plans are limited to 32 per server level/tick and 4 per 64×64 X/Z region/tick, with excess work deferred through `WAITING_PLAN` while preserving the live goal intent;
- committed flight performs no surface pathfinding; the dynamic monitor horizon is `reactionTicks + 2`, capped at 20 ticks, while the exact committed landing is revalidated independently;
- reaction latency is derived from base `MOVEMENT_SPEED`, bounded to 2–10 ticks and never made instant by fall speed/knockback;
- Reorientation may take a later airborne correction only after reaction delay and a fresh legal physical forecast; spent Clinging never receives a second turn;
- unknown/unloaded geometry, blocking first contact, trapped landings and foreign gravity ownership fail closed.

### Sprint evidence

- **NAV-S03 — anticipated landing:** closed after separating 40-tick acquisition from 10-tick presentation and proving touchdown/invalidations across the full compatibility matrix.
- **NAV-S04 — water controls/camera:** closed with camera-relative WASD, world-vertical Space/Shift, support/world-up presentation and explicitly registered client GameTests.
- **NAV-S05 — general mob planner:** closed with pure physical transition evaluation, bounded launch-region search, first-contact safety and `<=20` forecasts per local plan.
- **NAV-S06 — pet/general goal integration:** closed with history-free pet follow, safe teleport fallback, generic entity/position intents, Melee/Avoid wake seams and gravity-enabled flee while keeping vanilla high-level AI authoritative.
- **NAV-S07 — dynamic target/world reaction:** closed at functional commit `370c2f4c8a40f87b9d89e1895cf0df4df8ba68bd`; **CI #1007 / run `35103896552`** passed build/JUnit, 168 server GameTests, default client, First Person, Scale Brews server/client, Fresh Animations and snapshots.
- **NAV-S08 — efficiency/adversarial:** closed with reaction-bounded flight monitoring, shared global/regional planning budgets, deferred planning ownership and logical-work gates instead of unsupported percentage claims.
- **Pre-convergence functional HEAD `b2de88e83e1e64416288d220c8d86d52aeca014d`: CI #1038 / run `35105097956`** passed the complete matrix after airborne-owner tracking, flee integration and S09 code gates.

### Useful red history

The campaign deliberately kept red results that exposed invalid fixtures or missing registration instead of weakening production contracts:

- water client coverage initially produced a false green because new client GameTests were not registered; registration became part of the gate;
- `MeleeAttackGoal` fixtures initially mixed a manually invoked goal with the mob's live scheduler, used a mock player that vanilla did not accept as a stable zombie target, and treated the goal cooldown as relative test time instead of absolute world `gameTime`; fixtures were isolated/corrected while preserving vanilla cadence;
- an S07 rescue fixture accidentally left an alternate DOWN support while asserting that NORTH was the only legal correction; the world fixture was fixed rather than relaxing the reactor's safety/risk rules;
- budget tests were extended to cover both global and regional concentration plus deferred ownership, preventing a nominal global cap from hiding local horde spikes.

These failures are evidence that the tests challenged assumptions rather than merely confirming implementation-shaped fixtures.

### Automated validation versus manual QA

The beta.4 automation proves deterministic contracts: capability, first-contact geometry, transition purity, plan budgets, reaction timing, goal ownership, fluid boundaries, camera snapshots and compatibility lanes. It does **not** prove subjective gamefeel or real-modpack throughput.

Manual QA is still required for:

- feel/readability of posture-driven aerodynamics across long falls, reversals and head/feet orientations;
- landing anticipation at low/high speed and on irregular/moving surfaces with realistic multiplayer latency;
- water frame transitions near uneven support and current/knockback interactions;
- pets, melee pursuit and flee behaviour in real terrain rather than compact GameTest arenas;
- horde behaviour and scheduler fairness in representative mob-heavy modpacks;
- interaction of beta.4 navigation with First Person, Fresh Animations and optional companions during longer sessions.

No percentage performance improvement is claimed for beta.4 without reproducible profiling of a representative server/modpack. The enforced claims are structural budgets and bounded logical work.

## Alchemical Leather compatibility TM

The Alchemical Leather semantic-wear integration was rebuilt on a fresh branch from current `main` rather than merging the stale first prototype, which had diverged by more than a hundred mainline commits.

The compatibility boundary is intentionally narrow and optional:

- Clinging: Reoriented owns Reorientation's boots slot declaration and the semantic facts for successful gravity turns / controlled Reorientation flight;
- Alex's Mobs retains registry ownership of Clinging;
- Alchemical Leather owns infusion/source arbitration, equipment selection, balance interpretation, fractional work and durability damage;
- the Java bridge is linkage-safe and inert when Alchemical Leather is absent.

Reserved GameTest holdouts verify that the compatibility resources are packaged, no event owner is invented with no active gravity effect, Clinging/Reorientation ownership is resolved from real effects with Reorientation precedence, airborne self-controlled Reorientation is eligible for continuous work, and moving/support-surface state or removal of Reorientation suppresses that work. The authoritative turn publisher is attached to the single `ClingingReoriented.attempt(...)` return boundary and only accepts `SUCCESS`; this structurally excludes failed, blocked, unchanged and grounded-mount actions while retaining successful airborne mounted turns.

The ordinary Clinging CI remains the standalone/missing-Alchemical gate. The coordinated Alchemical Leather CI builds this branch and loads it beside Alchemical Leather so its registry-driven VanillaPlus potion/wear audit validates the real compatibility resources. Final convergence evidence is recorded in `TM_ALCHEMICAL_LEATHER_COMPAT.md` after both sides are green on the final documentation-complete heads.

## 0.1.0-beta.3 — Performance and stability hardening

Beta.3 is a small performance/stability prerelease over beta.2. It does not intentionally change gameplay, camera controls, targeting rules, compatibility semantics or visual timings. The campaign focuses on reducing work in global entity/render paths, lowering temporary allocation pressure and bounding rare recovery searches that could otherwise concentrate thousands of collision checks in one tick.

### TM performance campaign

The campaign is documented in `docs/sprints/PERF-S00-performance-plan.md` and `PERF-S07-release.md`.

Key invariants are mechanically preserved:

- ordinary mobs that have no Clinging-owned gravity work can take a cheap tick path, while owned/borrowed states still enter the complete lifecycle;
- directional fall suppression preserves the previous ownership semantics while reusing thread-local context rather than repeatedly deleting/recreating it;
- player/mob gravity recovery retains the exact legacy radius and the same 2,108 valid half-block offsets in the same order, but tests at most 64 candidates per call and continues later;
- landing-provider ordering is unchanged and rebuilt only when the provider registry changes;
- Scale Brews/Anatomy remain optional and dynamically linked with no production compile dependency;
- Gravity Charge retains its 32-block range, 15-degree cone, direct Target Block priority, assisted fan, LOS, ranking and retry cadence;
- Gravity Fall retains the beta.2 full-sphere camera invariants, First Person ownership and Fresh Animations presentation.

`RecoveryBudgetTest` independently rebuilds the old nested recovery loops and asserts exact candidate count/order plus the 64-candidate per-call budget.

### Adversarial history

An attempted PERF-S01 optimization tried to intercept only the `DirectionalFallTracker.tick` invocation injected by Gravity Changer. Run **#822** correctly went red because the target INVOKE is introduced by another mixin and is not visible to that MixinExtras injector phase. That experiment was removed rather than weakening the injector requirement. The final implementation retains the proven owner-context design while eliminating repeated ThreadLocal entry deletion/recreation.

The final pre-version code HEAD `be04f41ebe1289127837eac4e03b867e9d6e6db3` passed **run #831** (`35008292945`) across localization parity, build/JUnit, server GameTests, default client, First Person, Scale Brews server/client, Fresh Animations and semantic snapshot validation.

### Final beta.3 evidence

- **Release-prep HEAD:** `4dfd3ed64d55b0ab55fef6b302cf1b98dbe2a316`.
- **Run #832** (`35009515014`): success across the complete release matrix after version, docs and publisher canonization.
- **PR #29** merged the campaign to `main` as `d45511e0fa8829c8bc451b5f3db3a652f13f3d9f`; the merge preserved the release-prep tree.
- **Main run #834** (`35010672290`): success across localization parity, build/JUnit, server GameTests, default client, First Person, Scale Brews server/client, Fresh Animations and semantic snapshots on the exact release commit.
- **Publish beta.3 prerelease #1** (`35011714128`): success; downloaded the exact artifact from main run #834 and published without rebuilding.
- **Release/tag:** `v0.1.0-beta.3`, target `d45511e0fa8829c8bc451b5f3db3a652f13f3d9f`.
- **Regular JAR SHA-256:** `675b7edbc8579cbfa471e6a372afceab5d23306ed50caf356e8de315b944757d`.
- **Sources JAR SHA-256:** `ce04d04d940461f6e16a2136927b610c4a6c64eba14936bef1bc07e4820dcf4b`.
- A later beta.3 publisher invocation was skipped by the one-shot gate because the release already existed.

No claim is made that beta.3 definitively fixes a previously observed multi-second freeze in a large modpack: that observation was not isolated to this mod. The release does remove several objectively unnecessary hot-path allocations/workloads and bounds one rare worst-case server search.

## 0.1.0-beta.2 — Full-sphere camera hardening

Beta.2 is a regression/hardening release over beta.1. It keeps gameplay authority unchanged while replacing the pole-singular Gravity Fall camera representation and fixing retained-camera ownership for gravity turns initiated inside fluid.

### TM camera campaign

The camera work followed red-before-green TM gates:

- **CAM-S01 red:** commit `73ab816b111824ac79c903eb5f5bd0a9b6d523d3`; run **#792** (`34948135795`) passed build/JUnit and server GameTests, then failed in default Client GameTests after registering screen-space pole/third-person regressions.
- **CAM-S02–S04 green:** commit `ff4624618cfdfead0c74b812fc4bcb9a5a09db27`; run **#793** (`34949005750`) passed build/JUnit, server, default client, First Person, Scale Brews server/client, Fresh Animations and semantic snapshots.
- **CAM-S05 adversarial:** commit `d6a0cf545698e8fe7a2244329664b0d3fb0e480d`; run **#794** (`34950338232`) passed the same complete matrix after adding diagonal pole and first/third-person continuity holdouts.

Automated camera coverage verifies:

- horizontal screen-space input does not invert at either pole;
- diagonal input preserves both screen axes across both poles;
- a pure vertical 360-degree loop restores forward and screen-up without accumulating roll;
- first- and third-person modes share one look frame through a pole crossing;
- third-person keeps vanilla camera distance/wall clipping while using the continuous Gravity Fall look frame;
- entity yaw/pitch remains vanilla-compatible with the same forward vector;
- Gravity Fall exit preserves gaze;
- historical HOLD/LAND/body, First Person, Scale Brews and Fresh Animations tests remain green.

### Fluid retained-camera regression

PR #27 first registered the underwater HOLD regression red, then separated dry-origin and fluid-origin retained-camera epochs. A HOLD created by a gravity request while already inside fluid survives that fluid epoch; a later distinct fluid entry still retires an older dry-flight HOLD. Water/lava, exit and re-entry are covered without re-enabling submerged landing or Gravity Fall body semantics.

### Beta.2 visual evidence

The release matrix requires semantic camera snapshots in the default, First Person and Fresh Animations lanes, including pole-crossing and first/third-person continuity checkpoints. Each screenshot has a neighboring numerical invariant; screenshots are supporting evidence rather than the sole assertion.

## 0.1.0-beta.1 — Gravity Charge beta gate

Gravity Charge is the feature that moves Clinging: Reoriented from alpha to beta. The beta label does **not** weaken validation: the feature passed the complete matrix on the final branch HEAD and then again on the exact integrated `main` commit before `v0.1.0-beta.1` was published.

### Final public/internal naming

Before first publication, the provisional development name “Shulker Charge” was replaced completely by:

- display name: **Gravity Charge**;
- Spanish (`es_es`): **Carga de gravedad**;
- registry ID: `clinging_reoriented:gravity_charge`;
- implementation/tests/assets: `GravityCharge*`, `gravity_charge*`, snapshots `gravity-charge-*`;
- sprint namespace: `GC-S00` … `GC-S06`.

No released world or artifact ever used `clinging_reoriented:shulker_charge`, so beta.1 intentionally carries no legacy alias. CI validates exact `en_us`/`es_es` key parity and non-empty Spanish values.

### Gravity Charge invariants

Automated coverage verifies:

- melee and arrow interception, including dispenser arrows, can materialize exactly one Gravity Charge;
- mixed arrow/melee races cannot duplicate the drop;
- shield, ordinary impact, expiry and unrelated destruction do not mint items;
- player and real dispenser launch use the same exact vanilla `SHULKER_BULLET` entity type and consume one item;
- direct-ray Target Blocks win acquisition; assisted entities/blocks stay within 32 blocks / 15 degrees;
- initial living-target acquisition requires line of sight;
- valid locks are sticky and are not replaced by a later better candidate;
- dead/removed/dimension-transferred entity targets and invalid Target Blocks are cleared and reacquired from current projectile position while preserving original intent;
- targetless Gravity Charges continue cardinal free flight and retry instead of freezing or inventing curved homing;
- concurrent Gravity Charges keep independent target/retry/capture state;
- Target Block routing ends in real projectile collision/redstone response;
- vanilla shulker-duplication semantics remain available because runtime entity type is unchanged;
- Reorientation brewing accepts Gravity Charge and explicitly rejects Shulker Shell as the old ingredient.

### Client evidence

The default client snapshot matrix includes:

- `gravity-charge-inventory-icon`;
- `gravity-charge-first-person-held`;
- `gravity-charge-third-person-held`;
- `gravity-charge-projectile-renderer`;
- `gravity-charge-fixed-3d`.

The item GUI/held assets are original project assets using the project-owned `gravity_charge` texture. The projectile snapshot intentionally exercises Minecraft's vanilla ShulkerBullet renderer because runtime entity type remains vanilla.

### Adversarial campaign history

Run **#701** exposed a GameTest fixture problem in the optional Scale Brews lane: a late-target projectile stopped ticking after leaving the simulated chunk region. The fixture was hardened to keep the full 32-block flight corridor plus margin at `ENTITY_TICKING`; production remained unchanged and no Scale-specific code path was introduced.

Run **#706** then failed before compilation because Modrinth returned HTTP 503 for multiple required dependencies. It was classified as **environment** and caused no code change.

Run **#710** (`34895769482`) passed the complete matrix before the alpha.14 synchronization. Run **#714** (`34897063938`) passed the complete post-merge matrix on `a16d16faaa2cbd3c4b51678f08310ebc9a8bf681`, including build/JUnit, server GameTests, default client, First Person, Scale Brews server/client, Fresh Animations and semantic screenshots.

Alpha.15 was subsequently published as the last alpha for pet gravity-breadcrumb pursuit. Beta.1 therefore revalidated Gravity Charge on top of the **published last-alpha baseline**, rather than inheriting an older green run by assumption.

### Final beta.1 evidence

The final public/internal rename and beta preparation landed at `6a22223bc20591a9e320bef84521fb402501df56`.

- **Feature branch run #754** (`34900246377`): success — localization parity, build/JUnit, server GameTests, default client, First Person, Scale Brews server/client, Fresh Animations and semantic snapshots all green.
- `main` was fast-forwarded to that exact commit, so integration introduced no different merge tree.
- **Main run #758** (`34901126185`): success — the complete matrix passed again on the exact release commit.
- **Publish beta.1 prerelease #11** (`34902013181`): success — downloaded the artifact from main run #758, verified the expected regular/sources JARs and published without rebuilding.
- **Release/tag:** `v0.1.0-beta.1`, target `6a22223bc20591a9e320bef84521fb402501df56`.
- **Regular JAR SHA-256:** `41d0d9f3d0fab9c504cab619facd740508442ba4350d2d680b39f126ed5630fb`.
- **Sources JAR SHA-256:** `c761f923dbbe45f51b5286a462525190d998f8564bfa6e9cf7ecda672d2a0215`.

This is the evidence that closes GC-S06 and the alpha line.

## Compatibility matrix

Every beta release-gating run executes:

- localization key/value parity (`en_us` ↔ `es_es`);
- Gradle build and JUnit;
- required server GameTests;
- default client GameTests;
- First Person 2.7.2 + Not Enough Animations 1.12.4;
- Scale Brews beta.5 isolated server and client lanes;
- Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2;
- semantic screenshot validation;
- artifact retention for JARs, logs, XML, reports and screenshots.

## Packaging gate

A prerelease is published only from the **exact `main` commit whose complete `Build and test` run succeeded**. The version-specific release workflow downloads the regular and sources JARs from that exact workflow artifact, verifies one of each, records SHA-256 digests and creates the matching prerelease tag against the same commit. It never performs a second release build.

Production output must not contain GameTest classes, dependency JARs, temporary planning files or raw validation logs.

## Asset provenance

The Gravity Charge 16x16 GUI icon, item texture and project-authored 3D geometry are original GPL-3.0-or-later assets. Editable icon source lives under `docs/art/gravity-charge/`. Minecraft's ShulkerBullet renderer/resources remain third-party runtime material and are not redistributed.

## Manual QA still required

- Dedicated multiplayer with realistic latency around rapid Reorientation, Gravity Charge capture/relaunch/reacquisition, landing commitment, fluid entry/exit, teleport and tracking boundaries.
- Human motion-comfort/readability during repeated full-sphere look, gravity reversals and 500 ms landing manoeuvres.
- Long full-pack sessions with First Person + Fresh Animations together, mount/pet routes, Gravity Charge use and modded fluids.
- Human feel review of Gravity Charge targeting readability and dispenser/build interactions.
- Performance profiling in a representative large modpack remains recommended before attributing or quantifying any multi-second pause to this mod.

Automated assertions and snapshots are evidence, not a substitute for human gameplay acceptance.

## Historical releases

- **0.1.0-beta.3**: performance and stability hardening with bounded recovery work and reduced hot-path allocation/CPU overhead.
- **0.1.0-beta.2**: full-sphere camera/input hardening and underwater retained-camera fix.
- **0.1.0-beta.1**: Gravity Charge; first beta.
- **0.1.0-alpha.15**: pet gravity-breadcrumb pursuit; last alpha.
- **0.1.0-alpha.14**: Gravity Fall control/camera, 500 ms landing, full-sphere look, aerodynamics, fluid/climbable policy, safety and directional mace.
- **0.1.0-alpha.13**: retained camera, Gravity Fall body, absorbed-collision impact and landing-surface API.
- **0.1.0-alpha.12 and earlier**: underwater input arbitration, recharge, jump reservation, tracked mount/pet snaps and original airborne gravity ownership foundations.
