# Validation

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

The complete target matrix remains: build/JUnit/server GameTests without optional
Scale Brews, default real-client GameTests, First Person 2.7.2, then isolated Scale
Brews beta.5 server and client lanes. Production compileClasspath remains Scale-free.

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
success/failure sounds, obstruction rejection, effect expiry, beacon selection,
Elytra priority, sprint-jump intent, mounted hierarchy validation, explicit mob
ownership, prior-frame mount loans, passive mob sources, pet route bounds/lifecycle
clearing, bounded retirement, center-aligned voluntary clearance, selection/heading
separation, snap presentation, respawn visual epochs, fall segmentation and moving-
surface causal replay rejection.

## Packaging

Prereleases are published from an artifact validated for the intended target. The
production artifact contains production classes/resources only: no GameTest classes,
dependency JARs, temporary TM work-plan documents or raw validation logs. Release
tags, regular JARs and sources JARs must correspond to the validated target rather
than a later merely equivalent rebuild.

## Manual QA still required

- Dedicated multiplayer with realistic latency and rapid presses around landing,
  support, teleport and interrupted-Reorientation boundaries.
- Human acceptance of the 180/240 ms quadratic snap across all 30 direction pairs,
  first- and third-person.
- Repeated DOWN↔UP turns after deliberately looking vertical to select the target,
  confirming the pre-selection world heading remains intuitive when the camera is
  levelled again.
- Death/respawn after several turns, confirming the first post-respawn turn keeps
  Clinging presentation and heading semantics.
- Long Reorientation chains with real falls and landings, checking both intended
  fall damage and the reset at every actual gravity direction change.
- Repeated sprint-jump chains over slabs, stairs and uneven terrain.
- Human acceptance of center-aligned sideways turns in irregular caves/tight spaces.
- Long pet routes and full-pack visual acceptance beyond bounded CI fixtures.

Automated assertions and inspected screenshots are not human gameplay QA.
