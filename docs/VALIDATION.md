# Validation

## Public-tree revalidation (2026-09-09)

The repository prepared for publication was rebuilt from a clean output directory
with Java 25 and dependencies resolved from their public sources. These commands
completed successfully:

```powershell
.\gradlew.bat build runGameTest -PwithoutScaleBrews --no-daemon --console=plain
.\gradlew.bat runClientGameTest -PwithoutScaleBrews --no-daemon --console=plain
```

The first run passed all 39 required server GameTests and all 10 JUnit tests. The
second launched the real client GameTest environment and passed both client suites.
Scale Brews was available only as a compile-time API and was not loaded for either
runtime check, so the core mod was exercised without its optional scale integration.

The resulting `clinging-reoriented-0.1.0-alpha.6.jar` was inspected separately. It
contains the production classes, mixin configuration, metadata, translations and
effect texture; it contains no GameTest classes, dependency JARs or raw validation
logs. Its embedded metadata identifies `Clinging: Reoriented` 0.1.0-alpha.6 under
GPL-3.0-or-later and declares Alex's Mobs Continued, CodxLib, Gravity Changer and
Cloth Config as required dependencies.

## Alpha.6 automated evidence

The final alpha.6 run executed:

```powershell
.\gradlew.bat build runClientGameTest -PwithFirstPerson --offline --no-daemon
```

It completed successfully in 2 minutes 36 seconds with:

- 39 required server GameTests;
- 10 JUnit tests;
- the main real-client GameTest suite;
- the optional First Person/Scale Visual Compat client suite;
- one-second and two-second camera-animation timing checks;
- generated client configuration verified at the one-second default.

A separate build without Scale Brews passed the server suite, demonstrating that the
optional scale paths are not required for loading or core behavior.

## Covered behavior

Automated fixtures exercise six-direction input and landing, held-key deduplication,
success/failure sounds, obstruction rejection, effect expiry, beacon selection,
Elytra priority, mounted root/passenger validation, passive mob sources, pet route
bounds and lifecycle clearing, and optional compatibility paths. Tests also cover a
real jump toward a wall and repeated airborne Reorientation in a client environment.

## Packaging

The alpha.5 packaging gate compared every production class in the release JAR with
the tested artifact and found them byte-identical; only metadata differed. Public
builds must additionally verify that no GameTest class, local dependency JAR or raw
validation log enters the production artifact.

## Manual QA still required

- Dedicated multiplayer with realistic latency and rapid presses around landing or
  teleport boundaries.
- Long gameplay sessions with repeated effect refresh and expiry.
- Extreme external scale changes and unusual modded passenger attachment overrides.
- Recovery when every nearby DOWN-oriented box is obstructed.
- Long pet routes through loaded and unloaded terrain.
- Full-pack visual acceptance across all supported camera perspectives.

Automated assertions and inspected screenshots are not human gameplay QA.
