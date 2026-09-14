# Changelog

## Unreleased

## [0.1.0-alpha.13] - 2026-09-14

### Free-flight camera and landing

- Keep the local player's actually rendered world frame stable through voluntary airborne gravity changes instead of rotating the camera on every Clinging/Reorientation decision.
- Add server-authoritative short-horizon landing prediction over real entity dimensions, velocity, gravity and collision geometry.
- Start the existing 180 ms quarter-turn / 240 ms half-turn quadratic presentation only when a valid future floor becomes imminent.
- Add `LANDING_COMMITTED`: new gravity requests during the landing window are discarded rather than queued.
- Revalidate committed support and preserve the exact partial visual quaternion when a real surface invalidation cancels the landing, avoiding snap-back.

### Gravity Fall body and controls

- Add sustained Gravity Fall presentation after 12 airborne ticks with a six-tick body-root blend.
- Orient the macro body from real world velocity, not camera or logical gravity, with stable zero-speed retention and minimal-rotation continuity through curved/reversing trajectories.
- Add BODY_LANDING convergence toward predicted support while keeping the camera independent.
- Keep W/A/S/D relative to the retained visual frame without adding new air steering.
- Explicitly release Gravity Fall/landing presentation at Elytra, water/lava, teleport, vehicle, death/respawn and foreign-ownership boundaries.
- Preserve First Person camera ownership and Fresh Animations/EMF internal limb animation while Clinging applies only a macro root transform.

### Impact physics

- Replace Clinging's gravity-turn `fallDistance` segmentation as the primary damage model with collision-absorbed world velocity.
- Convert absorbed impact speed to vanilla-equivalent fall distance and delegate back to vanilla block/fall callbacks where possible.
- Preserve damage through a late gravity turn when the old floor still absorbs high-speed motion, while allowing genuine opposite-gravity braking to reduce damage.
- Keep tangential collisions harmless and sequence-fence multi-axis/dual-hook handling so one physical impact is charged once.

### Landing surface API

- Add public `LandingSurfaceProvider` and `LandingSurfaces` contracts plus a vanilla collision provider.
- Require bounded, stable, gravity-bound contacts and fail closed on stale, invalid, exceptional or non-finite provider data.
- Keep the public API independent of Scale Brews; concrete Scale landing integration remains outside this release.

### Hardening and compatibility

- Harden remote Gravity Fall epochs and effective visual-frame ownership across tracking/retracking and player replacement.
- Release retained camera/landing state before teleports and when Clinging loses physical ownership.
- Preserve independent mount/pet ownership while pets replay real follow-owner breadcrumbs behind a mounted owner.
- Pin CI coverage for First Person 2.7.2 + Not Enough Animations 1.12.4 and Fresh Animations 1.10.5 + Player Extension 1.1 + EMF 3.3.5 + ETF 7.2.
- Keep Scale Brews beta.5 in isolated optional server/client lanes with no production compile dependency.

### Validation

- Expand the required server suite to **86 GameTests** and add cross-domain adversarial holdouts for six-turn momentum/camera continuity, late-turn arbitrary-normal impact, invalidated landing, zero-speed jitter, Elytra/water lifecycle, mount/pet ownership, respawn/teleport and retracking.
- Preserve default, First Person and Fresh Animations screenshot matrices separately in CI.
- Validate required PNG checkpoints, dimensions and SHA-256 manifest entries automatically; assert semantically distinct visual checkpoints are not byte-identical.

## [0.1.0-alpha.12] - 2026-09-13

- Added passive 250 ms underwater double-Space gravity selection without consuming vanilla ascent.
- Restricted Clinging recharge to real gravity-relative feet support, including seabed support but not free swimming/body contact.
- Made near-landing sprint-jump reservation scale with effective jump power from one to three ticks.
- Added generic tracked 180/240 ms Clinging-owned snap presentation for mounts and pets, common-axis mounted half-turns and off-screen transition advancement.
- Added server/client coverage for underwater controls, jump-strength interaction and tracked entity presentation.

## [0.1.0-alpha.11] - 2026-09-13

- Separated rendered target selection from pitch-independent navigation heading.
- Preserved heading through opposite gravity changes and established 180/240 ms quadratic snap timing.
- Carried visual sequence state across respawn and started fresh fall-distance segments after owned gravity changes. Alpha.13 supersedes that last damage strategy with collision-absorbed impact physics.

## [0.1.0-alpha.10] - 2026-09-12

- Introduced Clinging-owned minimal snap transport, gravity-relative sprint-jump protection, strict one-turn Clinging semantics and removal of the old camera-timing JSON.

## [0.1.0-alpha.8] - 2026-09-11

Documentation-sync prerelease with no gameplay changes relative to alpha.7.

## [0.1.0-alpha.7] - 2026-09-11

Ownership/recovery hardening: bounded retirement, external gravity ownership, transactional passenger recovery, prior-frame mount loans, moving-surface replay fences and isolated optional integrations.

## [0.1.0-alpha.6] - 2026-09-09

First public alpha candidate with airborne Space selection, one-turn Clinging, unlimited Reorientation, Elytra priority, beacon support, mounted turns and pet route replay.
