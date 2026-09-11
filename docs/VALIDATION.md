# Validation

## Alpha.7 hardening candidate — 2026-09-11

The hardening branch is validated without loading optional Scale Brews at runtime.
The core CI command is:

```powershell
.\gradlew.bat build runGameTest -PwithoutScaleBrews --no-daemon --console=plain
.\gradlew.bat runClientGameTest -PwithoutScaleBrews --no-daemon --console=plain
```

Implementation checkpoint run `34586087290` completed successfully with **42 required
server GameTests**, all **10 JUnit tests** and the real default client GameTest suites.
Scale Brews beta.5 was fetched only as a compile-time API for the transitional
anatomical bridge and was absent from the runtime fixture.

The alpha.7-specific adversarial holdouts cover:

- forced retirement with every local DOWN candidate obstructed, followed by a safe
  retry; the entity remains in its current frame while pending and never relocates
  farther than four blocks;
- a mob frame first acquired by Clinging and then overwritten by an external
  Gravity Changer source; later effect expiry preserves the external direction;
- root relocation whose root box fits but passenger box is obstructed; the entire
  failed candidate leaves root/passenger positions, gravity and momentum unchanged;
- moving-surface references using the latest authentic support interval, duplicate
  reuse, an authentic-but-older interval, stale age and a non-consecutive sequence;
- client camera timing where an unrelated Gravity Changer transition keeps upstream
  timing and only a Clinging-owned visual epoch uses `cameraRotationSeconds`.

The normal runtime suite also covers the generic mount contract without Scale:
compatible non-player `LivingEntity` roots use the same mounted-gravity path and the
removed Tiny-Mount-specific Clinging mixins are not required for core behavior.

The optional First Person 2.7.2 fixture is intentionally tracked separately from the
default release lane. Alpha.7 narrows that fixture to First Person itself, without
requiring Scale Brews or Scale Visual Compat. Do not infer a fresh First Person
runtime pass from the default CI result until that optional lane is executed.

## Public-tree revalidation (2026-09-09, alpha.6)

The repository prepared for alpha.6 publication was rebuilt from a clean output
directory with Java 25 and dependencies resolved from their public sources. These
commands completed successfully:

```powershell
.\gradlew.bat build runGameTest -PwithoutScaleBrews --no-daemon --console=plain
.\gradlew.bat runClientGameTest -PwithoutScaleBrews --no-daemon --console=plain
```

The first run passed all 39 required server GameTests and all 10 JUnit tests. The
second launched the real client GameTest environment and passed both client suites.
Scale Brews was available only as a compile-time API and was not loaded for either
runtime check.

## Historical alpha.6 optional evidence

The final alpha.6 optional run executed:

```powershell
.\gradlew.bat build runClientGameTest -PwithFirstPerson --offline --no-daemon
```

It completed successfully with the then-current 39 server tests, 10 JUnit tests,
main real-client suite, First Person/Scale Visual Compat fixture and one/two-second
camera-animation checks. This is historical alpha.6 evidence, not a substitute for
a fresh alpha.7 optional-mod run after the ownership changes.

## Covered behavior

Automated fixtures exercise six-direction input and landing, held-key deduplication,
success/failure sounds, obstruction rejection, effect expiry, beacon selection,
Elytra priority, mounted hierarchy validation, explicit mob ownership, prior-frame
mount loans, passive mob sources, pet route bounds/lifecycle clearing, bounded
retirement and moving-surface causal replay rejection. Tests also cover a real jump
toward a wall and repeated airborne Reorientation in a client environment.

## Packaging

The final release candidate must be rebuilt after temporary work files are removed.
The production artifact must contain only production classes/resources and no
GameTest classes, dependency JARs, work-plan documents or raw validation logs. The
release tag/asset must point at the exact candidate that passed the cleaned CI run.

## Manual QA still required

- Dedicated multiplayer with realistic latency and rapid presses around landing,
  support and teleport boundaries.
- Long gameplay sessions with repeated effect refresh and expiry.
- Extreme external scale changes and unusual modded passenger attachment overrides.
- Long pet routes through loaded and unloaded terrain.
- Full-pack visual acceptance across supported camera perspectives.
- Fresh optional First Person runtime acceptance for alpha.7 if that integration is
  to be promoted beyond historical/compile-time evidence.

Automated assertions and inspected screenshots are not human gameplay QA.
