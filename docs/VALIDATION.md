# Validation

## 0.1.0-alpha.13 release-candidate validation — 2026-09-14

Alpha.13 is the gamefeel/landing/impact campaign built through GF-S00..GF-S05 and canonicalized in S06. It changes the local-player presentation model from immediate gravity snaps to **free-flight camera retention plus physically committed landing snaps**, adds sustained Gravity Fall body orientation, replaces gravity-turn fall-distance segmentation with absorbed-collision impact semantics, and exposes a public landing-surface provider API.

### Physics and impact

Automated coverage verifies that voluntary gravity changes preserve world velocity, opposite gravity produces real braking/reversal rather than an impulse, and body presentation follows velocity instead of selected gravity. Impact tests derive the normal velocity actually absorbed by collision, map it to vanilla-equivalent fall distance, preserve block/fall callbacks, keep tangential motion harmless, combine multi-axis impacts once and catch late-turn arbitrary-normal collisions even when gravity-relative `onGround` no longer names the blocking surface.

The S05 holdout performs a real late `DOWN→EAST` gravity change immediately before a damaging collision and then exits ownership. The dangerous collision is still charged once. An earlier failing version of this fixture was classified correctly: it only absorbed about 0.58 speed, equivalent to about 2.75 vanilla fall blocks, so production was right not to damage. The fixture, not the damage model, was corrected.

### Camera, landing and controls

Server/client tests cover free-flight HOLD across multi-turn Reorientation, selection from the rendered look, visual-frame W/A/S/D, short real-geometry landing prediction, `LANDING_COMMITTED` input rejection without queueing, surface revalidation, and invalidation that freezes the exact partial landing quaternion rather than snapping back.

The adversarial six-turn sequence `EAST→UP→NORTH→DOWN→WEST→UP` keeps world momentum and retained camera continuity while body orientation responds only to actual velocity. Zero-speed jitter tests verify stable last-frame retention without NaN/flip/twist.

### Gravity Fall

Server tests cover discrete START/LAND/RESUME/RESET lifecycle and the 12-tick entry threshold. Client tests cover the six-tick body blend, cardinal sustained fall, 90-degree curved velocity, zero crossing, physical reversal, BODY_LANDING, resume after invalidation and touchdown/reset. Body rotation is applied around the logical model center to avoid orbiting around the feet.

Elytra, water/lava, vehicle/ownership boundaries, teleport, respawn and tracking replacement are adversarially exercised. Remote tracking uses fresh discrete epochs rather than per-tick quaternion packets.

### Mounts, pets and identity

Existing mount/passenger transactionality, common-axis opposite turns, mob ownership and pet breadcrumb replay remain covered. S05 adds mounted-owner/pet replay through real `FollowOwnerGoal` behavior and checks that mount borrowing does not steal the pet's independent ownership. Tracked entities require entity ID + UUID and monotonic per-entity sequence; retracking receives a fresh effective visual epoch.

### Compatibility matrix

The exact S05 evidence head `a074ff1a1c6f40bef2e23048c420dc9ab1751ec0` passed GitHub Actions run **#464** (`34838805270`):

- build and JUnit;
- **86/86 server GameTests**;
- default client GameTests;
- First Person 2.7.2 + Not Enough Animations 1.12.4;
- Scale Brews beta.5 server and client optional lanes;
- Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2;
- automated semantic snapshot validation;
- artifact retention for JARs, logs, XML, reports and screenshots.

S06 repeats the complete matrix on the final alpha.13 canonical/version head and again after integration into `main`; those exact run/commit IDs are the release gate, not a claim that the earlier S05 head is byte-identical to the packaged release.

### Snapshot evidence

CI preserves three independent screenshot sets and validates eleven required checkpoints. The S05 matrix contained:

- default: six-turn retained camera, zero-jitter, velocity-not-gravity, cancelled landing partial, Falling, Elytra;
- First Person: Gravity Fall root and BODY_LANDING;
- Fresh Animations: sustained DOWN, landing midpoint, landing final.

Every required S05 screenshot was a valid 854×480 PNG. The validator also requires semantically distinct pairs such as Falling/Elytra and FA landing-mid/final not to be byte-identical. Manual artifact inspection confirmed the expected intermediate landing frame, independent First Person camera and preserved FA internal animation under the macro body root.

## Historical release validation

### 0.1.0-alpha.12 — 2026-09-13

Alpha.12 added passive underwater 250 ms double-Space arbitration, gravity-relative water recharge correctness, jump-power-aware sprint-landing reservation and Clinging-owned tracked snap presentation for mounts/pets. Its release candidate passed 47 required server GameTests plus default client, First Person and isolated Scale beta.5 lanes. Alpha.13 preserves those controls while superseding alpha.12's local-player immediate-snap and fall-distance-segmentation behavior.

### 0.1.0-alpha.11 — 2026-09-13

Alpha.11 separated target selection from pitch-independent navigation heading, made presentation epochs survive player replacement and established 180/240 ms quadratic snap timing. Alpha.13 keeps those timing primitives for landing/tracked snaps but no longer rotates the local camera on each free-flight gravity turn.

### 0.1.0-alpha.10 and earlier

Alpha.10 introduced Clinging-owned minimal snap transport, sprint-jump intent protection and the strict one-voluntary-turn budget. Alpha.9 added center-aligned collision-clearance fallback. Alpha.8 was a documentation-sync prerelease over alpha.7 ownership/recovery hardening. Alpha.6 was the first public alpha candidate.

## Packaging gate

A prerelease must be published from the **exact commit** that passed the complete final matrix. The regular and sources JARs must come from the artifact of that exact run. Production output contains no GameTest classes, dependency JARs, temporary `docs/work` planning files or raw validation logs.

## Manual QA still required

- Dedicated multiplayer with realistic latency around rapid Reorientation, landing commitment, water entry/exit, teleport and tracking boundaries.
- Human motion-comfort/readability across repeated free-flight changes and late 90/180-degree landing snaps.
- Human comparison of short Clinging precision, sustained Reorientation Gravity Fall and Elytra visual language.
- Long full-pack sessions with Fresh Animations/First Person together and long mount/pet routes.
- Uneven-terrain sprint-jump feel with normal and high jump-strength modifiers.

Automated assertions and inspected snapshots are evidence, not a substitute for human gameplay acceptance.
