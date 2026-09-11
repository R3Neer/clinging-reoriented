# Validation

## 0.1.0-alpha.8 documentation-sync prerelease — 2026-09-11

Alpha.8 intentionally makes no gameplay or production-code behavior changes relative
to alpha.7. Its purpose is to publish the already-hardened implementation with the
repository documentation synchronized to the released state. No file under
`src/main/java` or `src/client/java` is changed by this version bump.

The release is accepted only after the repository's full CI repeats all required
server/JUnit tests, the default real-client suites and the separate First Person
compatibility lane on the exact alpha.8 target. Runtime validation continues to omit
Scale Brews; beta.5 is used only as a compile-time API for the transitional anatomical
bridge.

Alpha.7 supplied the functional hardening evidence that alpha.8 carries forward. The
validated alpha.7 `main` run `34590289860` at commit
`62ee1706ba311445d930dd25dc9aa93e8944eb07` passed **42 required server GameTests**,
all **10 JUnit tests**, the real default client GameTest suites and the real-client
First Person fixture. Alpha.8 repeats those same lanes before publication so the
version/documentation-only change does not bypass release validation.

The First Person fixture uses **First Person 2.7.2** with **Not Enough Animations
1.12.4**, without Scale Brews at runtime. It proves that unrelated Gravity Changer
frames retain First Person's native body offset, Clinging-owned frames rotate that
native baseline exactly once and external world-space offset handlers remain
world-space. The first version of this fixture exposed a stale hard-coded First
Person offset assumption; the test was corrected to derive its baseline from the
installed First Person binary, with no production-code change required.

The hardening-specific adversarial holdouts cover:

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

## Historical alpha.6 evidence

The repository prepared for alpha.6 publication was rebuilt from a clean output
directory with Java 25 and dependencies resolved from their public sources. The
server run passed all 39 required server GameTests and all 10 JUnit tests; the real
client GameTest environment passed both client suites. A separate optional run also
covered the then-current First Person/Scale Visual Compat fixture. Alpha.7/alpha.8
supersede that presentation evidence with the narrower First Person-only ownership
fixture described above.

## Covered behavior

Automated fixtures exercise six-direction input and landing, held-key deduplication,
success/failure sounds, obstruction rejection, effect expiry, beacon selection,
Elytra priority, mounted hierarchy validation, explicit mob ownership, prior-frame
mount loans, passive mob sources, pet route bounds/lifecycle clearing, bounded
retirement and moving-surface causal replay rejection. Tests also cover a real jump
toward a wall, repeated airborne Reorientation and scoped camera/First Person
presentation in real client environments.

## Packaging

Prereleases are published from the exact artifact produced by the successful final
`main` CI run; the release process does not rebuild a merely equivalent JAR after
validation. The production artifact contains production classes/resources only: no
GameTest classes, dependency JARs, temporary work-plan documents or raw validation
logs. The release tag, regular JAR and sources JAR are checked against the exact
validated target and recorded digests before publication.

## Manual QA still required

- Dedicated multiplayer with realistic latency and rapid presses around landing,
  support and teleport boundaries.
- Long gameplay sessions with repeated effect refresh and expiry.
- Extreme external scale changes and unusual modded passenger attachment overrides.
- Long pet routes through loaded and unloaded terrain.
- Full-pack visual acceptance across supported camera perspectives and broader mod
  combinations beyond the bounded First Person fixture.

Automated assertions and inspected screenshots are not human gameplay QA.
