# Validation

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

Automated assertions and snapshots are evidence, not a substitute for human gameplay acceptance.

## Historical releases

- **0.1.0-beta.2**: full-sphere camera/input hardening and underwater retained-camera fix.
- **0.1.0-beta.1**: Gravity Charge; first beta.
- **0.1.0-alpha.15**: pet gravity-breadcrumb pursuit; last alpha.
- **0.1.0-alpha.14**: Gravity Fall control/camera, 500 ms landing, full-sphere look, aerodynamics, fluid/climbable policy, safety and directional mace.
- **0.1.0-alpha.13**: retained camera, Gravity Fall body, absorbed-collision impact and landing-surface API.
- **0.1.0-alpha.12 and earlier**: underwater input arbitration, recharge, jump reservation, tracked mount/pet snaps and original airborne gravity ownership foundations.
