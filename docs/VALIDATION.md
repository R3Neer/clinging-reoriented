# Validation

## 0.1.0-alpha.10 development validation — 2026-09-12

Alpha.10 changes the orientation model, input intent and presentation path, so it
must pass the complete repository matrix: JUnit geometry tests, required server
GameTests, the real default client, First Person 2.7.2, and isolated optional Scale
Brews beta.5 server/client lanes. Production compileClasspath remains free of Scale.

Alpha.10-specific automated coverage includes:

- all 30 distinct cardinal gravity-direction pairs across representative yaw/pitch
  values, proving a settled Clinging presentation traverses only the physical
  90-degree or 180-degree gravity angle rather than Gravity Changer's historical
  120/180-degree canonical-frame excess;
- compensated visual-start identity: applying the authoritative yaw delta does not
  cause an instantaneous world-view jump before the short gravity snap begins;
- transported world look reaching the canonical target frame with pitch preserved;
- opposite-gravity heading selection plus the vertical-look right-axis fallback;
- fixed ease-out timing: 120 ms quarter turns and 180 ms half turns;
- unowned Gravity Changer animation retaining upstream 1.25-second behavior while a
  Clinging-owned epoch finishes at its fixed snap endpoint and releases ownership;
- spent Clinging rejecting every second voluntary airborne turn including DOWN,
  while Reorientation permits the same turn and forced effect-expiry retirement
  remains independent of the charge;
- near-floor descending sprint input being reserved for an imminent landing jump,
  with ascending, distant and non-sprinting cases left available;
- the same landing prediction under sideways gravity and under changed Gravity
  Changer gravity strength;
- alpha.9 center-aligned clearance beside an old floor, plus the genuine-obstruction
  fixture that still returns `NO_SPACE` atomically.

The client suites additionally cover held-key deduplication, sounds, Elytra priority,
rapid Reorientation usage, forced return to DOWN and the real wall/jump integration
flow. The First Person lane validates the same visual ownership boundary against the
installed First Person binary rather than a duplicated offset formula.

## 0.1.0-alpha.9 development validation — 2026-09-12

Alpha.9 introduced center-aligned voluntary-turn clearance and temporarily allowed
a spent Clinging player to select DOWN as a safety return. Alpha.10 deliberately
supersedes that latter policy and removes configurable camera timing; alpha.9 remains
historical evidence for the collision-clearance change.

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
clearing, bounded retirement, center-aligned voluntary clearance, heading transport,
snap presentation and moving-surface causal replay rejection.

## Packaging

Prereleases are published from an artifact validated for the intended target. The
production artifact contains production classes/resources only: no GameTest classes,
dependency JARs, temporary TM work-plan documents or raw validation logs. Release
tags, regular JARs and sources JARs must correspond to the validated target rather
than a later merely equivalent rebuild.

## Manual QA still required

- Dedicated multiplayer with realistic latency and rapid presses around landing,
  support, teleport and interrupted-Reorientation boundaries.
- Human acceptance of the 120/180 ms snap feel across all 30 direction pairs, both
  first- and third-person, especially opposite-gravity transitions while looking
  nearly parallel to gravity.
- Repeated real sprint-jump chains over slabs, stairs, uneven terrain and altered
  gravity strength to tune the bounded next-tick landing grace if necessary.
- Human acceptance of center-aligned sideways turns in irregular caves and tight
  spaces.
- Long gameplay sessions with repeated effect refresh/expiry, external scale changes
  and unusual passenger attachment overrides.
- Long pet routes and full-pack visual acceptance beyond the bounded CI fixtures.

Automated assertions and inspected screenshots are not human gameplay QA.
