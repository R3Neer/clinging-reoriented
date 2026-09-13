# Validation

## 0.1.0-alpha.12 validation — 2026-09-13

Alpha.12 extends alpha.11 in three independent areas while retaining the same
server-authoritative gravity contract: underwater input arbitration, stronger-jump
sprint-landing grace, and Clinging-owned snap presentation for mounts and pets.

Underwater, Vanilla keeps ordinary Space-to-ascend behavior. Clinging/Reorientation
requests a turn only on a deliberate second Space rising edge after a release and
within a fixed **250 ms** window. The detector observes the key passively and does
not consume or rewrite Vanilla input.

Sprint-jump reservation now scales its near-landing prediction horizon with effective
jump power. Vanilla's normal `0.42` jump keeps alpha.11's exact one-tick guard;
stronger Jump Boost/Leaping-style jumps widen only that prediction, with a hard cap
at three ticks. The original sprinting, airborne, descending and real-support gates
remain mandatory.

Clinging-owned non-player gravity changes now use the same **180 ms** quarter-turn /
**240 ms** half-turn quadratic snap as players. Entity transitions carry ID + UUID,
per-mob monotonic sequence, target, yaw delta and turn kind. Mounted half-turns use
one rider-selected physical axis for the root and rider while rebasing each entity's
own heading into its own yaw gauge. Pet replay and owned recovery use the mob's own
heading. Foreign Gravity Changer writes remain outside Clinging presentation
ownership.

Alpha.12-specific automated coverage includes:

- pure double-tap tests for first press, hold, release, in-window second press,
  expiry, pair consumption, context reset and enter-while-held behavior;
- a real-client underwater fixture proving single/held Space ascends without a
  gravity request, double Space requests exactly once, separate Reorientation pairs
  can turn repeatedly, and a rejected spent-Clinging request still preserves
  Vanilla ascent;
- submerged spent-Clinging, side/body contact and true seabed-support regressions,
  retaining the pre-existing all-six-directions gravity-relative landing suite;
- jump-power tests for the alpha.11 baseline, Jump Boost I/II monotonicity,
  boosted-only near-landing reservation, ascending/no-support cases, sideways
  gravity, gravity-strength interaction and the three-tick cap;
- geometry coverage for `GravityTransition.rebase`, including multiple headings on
  shared quarter- and half-turn physical plans;
- server GameTests proving successful pet/mount owned turns advance entity visual
  epochs only after preflight, failed/same-direction attempts do not publish or
  mutate yaw, foreign writes remain external, and rider/root half-turns share one
  physical axis while retaining entity-specific yaw deltas;
- focused real-client non-player snap coverage, including UUID-safe entity
  resolution, canonical completion and preservation of foreign Gravity Changer
  animation ownership;
- an off-screen tracked-entity regression: active owned snaps advance every client
  tick even when the entity is not rendered, so a stale 180/240 ms transition cannot
  replay later when the entity re-enters view.

The complete release-candidate matrix passes: build/JUnit plus all **47 required
server GameTests** without optional Scale Brews, default real-client GameTests,
First Person 2.7.2 real-client compatibility, isolated Scale Brews beta.5 server
compatibility, and the optional Scale Brews client-load lane. Production
`compileClasspath` remains Scale-free.

## 0.1.0-alpha.11 development validation — 2026-09-12

Alpha.11 is a game-feel/correctness follow-up to alpha.10 playtesting. It keeps the
minimal gravity transport but separates target selection from navigation heading,
makes presentation epochs survive player-entity replacement on death, starts a new
vanilla fall-distance segment on each actual Clinging-owned gravity change and
slows the snap enough to make its axis readable.

Alpha.11-specific automated coverage includes:

- all ordered cardinal gravity pairs using a yaw-derived world navigation heading,
  proving the settled visual path still traverses only the physical 90/180-degree
  gravity angle;
- the same heading-derived yaw delta transporting full view vectors correctly for
  representative pitches from nearly straight up through nearly straight down;
- opposite gravity changes preserving world navigation heading for representative
  yaw values, including DOWN↔UP;
- DOWN→NORTH transporting a northward heading into world UP along the new wall;
- server sanitization of off-plane, degenerate and non-finite heading input before
  transition geometry runs;
- compensated visual-start and interrupted-frame continuity invariants;
- fixed quadratic ease-out timing: 180 ms quarter turns and 240 ms half turns,
  with a real-client assertion that the old 120/180 ms endpoints are visibly
  incomplete;
- successful player gravity changes resetting vanilla fall distance while unchanged
  attempts preserve it;
- the same fall segmentation for owned mob/mount/root gravity changes;
- visual sequence state surviving death-copy and the first following visual epoch
  remaining strictly monotonic instead of being discarded by the client as stale;
- the existing strict one-voluntary-turn Clinging budget, sprint-jump intent guard,
  center-aligned clearance fallback and genuine-obstruction atomicity.

## 0.1.0-alpha.10 development validation — 2026-09-12

Alpha.10 introduced Clinging-owned minimal snap-style gravity transport, the
strict one-turn Clinging budget including DOWN, the gravity-relative sprint-jump
intent guard and removal of the client timing JSON. Its 120/180 ms cubic snap and
vertical opposite-turn fallback are superseded by alpha.11; the minimal transport,
body/head gauge compensation and interruption model remain the foundation.

## 0.1.0-alpha.9 development validation — 2026-09-12

Alpha.9 introduced center-aligned voluntary-turn clearance and temporarily allowed
a spent Clinging player to select DOWN as a safety return. Alpha.10 deliberately
superseded that latter policy. Alpha.9 remains historical evidence for the
collision-clearance change.

## 0.1.0-alpha.8 documentation-sync prerelease — 2026-09-11

Alpha.8 made no gameplay or production-code behavior changes relative to alpha.7.
Alpha.7 supplied the ownership/recovery hardening evidence carried forward by later
alphas: bounded retirement/retry, external mob-gravity ownership, transactional
passenger recovery, moving-surface replay fences and generic mounted gravity.

## Historical alpha.6 evidence

Alpha.6 was the first public alpha candidate. It established the original airborne
Space control, one-turn Clinging budget, Reorientation potion family, Elytra
priority, beacon support, mounted turns and pet route replay. Later validation
supersedes its presentation/configuration behavior.

## Covered behavior

Automated fixtures exercise six-direction input and landing, held-key deduplication,
underwater single/held Space ascent and double-tap selection, rejected underwater
attempts preserving swimming, success/failure sounds, obstruction rejection, effect
expiry, beacon selection, Elytra priority, jump-power-aware sprint-jump intent,
mounted hierarchy validation, explicit mob ownership, prior-frame mount loans,
entity visual epochs, mounted common-axis transport, pet snap presentation,
off-screen transition expiry, passive mob sources, pet route bounds/lifecycle
clearing, bounded retirement, center-aligned voluntary clearance,
selection/heading separation, snap presentation, respawn visual epochs, fall
segmentation and moving-surface causal replay rejection.

## Packaging

Prereleases are published from an artifact validated for the intended target. The
production artifact contains production classes/resources only: no GameTest classes,
dependency JARs, temporary TM work-plan documents or raw validation logs. Release
tags, regular JARs and sources JARs must correspond to the validated target rather
than a later merely equivalent rebuild.

## Manual QA still required

- Dedicated multiplayer with realistic latency and rapid presses around landing,
  water entry/exit, support, teleport and interrupted-Reorientation boundaries.
- Human underwater feel around the 250 ms boundary and repeated Reorientation pairs.
- Human sprint-jump feel with normal jumps and high Jump Boost/Leaping on slabs,
  stairs and uneven terrain.
- Human acceptance of the 180/240 ms quadratic snap across player, mount and pet
  transitions in first- and third-person, including entities entering/leaving view.
- Repeated DOWN↔UP turns after deliberately looking vertical to select the target.
- Death/respawn after several turns, long Reorientation fall chains and long pet
  routes/full-pack visual acceptance beyond bounded CI fixtures.

Automated assertions and inspected screenshots are not human gameplay QA.
