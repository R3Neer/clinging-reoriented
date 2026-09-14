# Changelog

## Unreleased

## [0.1.0-beta.1] - 2026-09-14

### Beta milestone

- Promote Clinging: Reoriented from alpha to **beta** with **Gravity Charge** as the milestone feature.
- Canonize the final public/internal name before first release: `Gravity Charge`, Spanish `Carga de gravedad`, registry ID `clinging_reoriented:gravity_charge` and `GravityCharge*` implementation/test names. The provisional `Shulker Charge`/`shulker_charge` name was never published and has no compatibility alias.
- Keep release strictness unchanged: beta.1 is published only from the exact `main` commit whose complete CI matrix succeeds.

### Gravity Charge

- Add the stackable Gravity Charge, materialized by intercepting a shulker bullet with melee or an arrow, including dispenser-fired arrows.
- Fence capture to exactly one item across mixed/racing interceptors; shields, ordinary impact, expiry and unrelated destruction do not create drops.
- Allow captured and relaunched Gravity Charges to be captured again under the same one-drop rule.
- Launch from player look or dispenser facing with one-item consumption and a 0.5 second manual-use cooldown.
- Keep the runtime projectile as the exact vanilla `SHULKER_BULLET` entity type so vanilla impact, Levitation, renderer and shulker-duplication semantics remain intact.

### Targeting and routing

- Add server-authoritative bounded acquisition at roughly 32 blocks / 15 degrees.
- Give a Target Block on the direct launch ray absolute acquisition priority; otherwise rank eligible entities and assisted Target Blocks by angular error then distance.
- Keep valid locks sticky instead of retargeting to a later better-scoring candidate.
- Reacquire from the projectile's current position when a target dies, disappears, changes dimension or a Target Block becomes invalid, while preserving original launch intent.
- Continue cardinal free flight when no target is available and retry periodically rather than switching to continuous curved homing.
- Route Target Blocks through discrete orthogonal shulker-style movement and real projectile impact so normal redstone response remains authoritative.

### Brewing, presentation and localization

- Replace Shulker Shell with Gravity Charge as the Clinging -> Reorientation brewing ingredient for normal and long variants.
- Integrate the approved original 16x16 GUI icon and project-authored 3D held model using the project's own `gravity_charge` texture.
- Keep the launched projectile's vanilla renderer through its retained vanilla entity type; no Mojang model/texture is redistributed.
- Rename semantic snapshots to `gravity-charge-*` for inventory, first-person held, third-person held, projectile renderer and fixed 3D presentation.
- Ship matching `en_us` and Spanish-from-Spain `es_es` keys and make CI reject language-key divergence or empty Spanish values.

### Validation and release

- Add adversarial Gravity Charge holdouts for real dispenser/redstone semantics, sticky locks, late acquisition, dimension invalidation/reacquisition, mixed arrow/melee races, concurrent charges and repeated targetless retries.
- Harden the GameTest fixture so the complete 32-block flight corridor remains `ENTITY_TICKING`; no Scale-specific production workaround is used.
- Revalidate Gravity Charge after alpha.15's pet gravity-breadcrumb release across required server tests, default client, First Person, Scale Brews server/client, Fresh Animations, localization parity and semantic snapshot validation.
- Publish beta.1 only from the exact successful `main` CI artifact rather than rebuilding for release.

## [0.1.0-alpha.15] - 2026-09-14

### Pet gravity-breadcrumb pursuit

- Make tame pets with their own compatible Clinging/Reorientation effect pursue pending owner gravity breadcrumbs through vanilla `FollowOwnerGoal`, including while the owner is inside vanilla's ten-block follow start dead zone.
- Project each airborne breadcrumb onto the pet's **current gravity-relative movement plane** instead of asking ordinary pathfinding to reach an impossible 3D point through the air.
- Use the follow goal's own bounded stopping semantics for arrival, then replay the recorded gravity turn only when the pet actually reaches that projected place.
- After replay, release navigation while the pet is unsupported so real gravity physics owns the fall; once the pet finds support in the new frame, the still-pending breadcrumb queue can resume.
- Refresh `FollowOwnerGoal`'s cached `PathNavigation` across its lifecycle because Gravity Changer replaces the navigation instance after a gravity change.
- Permit one center-aligned, collision-preflighted internal relocation when rotating at the current feet position would intersect the old support; this internal placement preserves the breadcrumb transaction and later queued turns.
- Keep the system fail-closed: sitting pauses pursuit without consuming the step, an external pet teleport invalidates the route, stale/foreign-dimension breadcrumbs are skipped, effect-free pets retain vanilla following, and foreign gravity ownership is never stolen.
- Add projection/unit coverage plus GameTests for the vanilla dead zone, real scheduled wolf traversal, bounded arrival, grounded replay, ballistic goal release, navigation replacement, ordered queue preservation, sitting pause/resume and external teleport invalidation.

## [0.1.0-alpha.14] - 2026-09-14

### Gravity Fall control and camera

- Add full-sphere local camera pitch during sustained Gravity Fall, including pole crossing and full 360-degree vertical loops.
- Canonicalize full-sphere look back to an equivalent vanilla yaw/pitch pair on Gravity Fall exit without changing the viewing direction.
- Add bounded macro-body look-follow outside a 35-degree neck deadzone, capped at 7.5 degrees per tick while preserving velocity as the primary body frame.
- Add posture-dependent aerodynamic drag: no extra drag when streamlined and at most 1.3% additional drag per tick when perfectly broadside.
- Add W air-diving that bends existing momentum toward gaze by at most 6 degrees per tick, gated by the positive velocity/look dot product and preserving speed before drag. No free thrust or lift is added.
- Add Elytra-style fast-air feedback during Gravity Fall at >=0.75 blocks/tick, with a 10-tick fade-in plus vanilla speed-squared volume/high-speed pitch behaviour.

### Landing and presentation

- Expand local landing prediction/presentation to a shared 10-tick / 500 ms manoeuvre for camera LAND and BODY_LANDING.
- Keep ordinary tracked non-player SNAP timing separate at the existing shorter turn-kind durations.
- Harden First Person look-down by rotating the Gravity Fall avatar root around a blended camera/body pivot, avoiding clipping while keeping the body intentionally visible and the real camera independent.

### World context and controls

- Add a generic `FluidContext` fence: any intersecting non-empty fluid volume suspends Clinging support, landing and Gravity Fall semantics, including modded fluids.
- Stop submerged seabed/body contact from restoring Clinging charge or triggering gravity-relative landing while the player is still inside fluid.
- Keep underwater double-Space gravity selection, but make owned water movement world-vertical: Space = +Y and Shift = -Y regardless of gravity.
- Define world-vertical climbable policy: DOWN vanilla, lateral gravity ignores climbables, UP mirrors vanilla Y climbing/sliding semantics.

### Safety, impact and mace

- Add server-authoritative 3.92 blocks/tick world-speed cap during eligible Clinging flight while preserving velocity direction.
- Add loaded-chunk frontier holding/resume and hard world/build-border recovery; safety corrections clear armed impact state.
- Add directional mace fall accounting from literal geometric distance along the current gravity axis. Gravity turns begin a fresh segment and gravity strength no longer scales mace height.
- Redirect all relevant Minecraft 26.2 mace fall-distance reads using their actual `LivingEntity`/`Entity` bytecode owners.

### Compatibility and validation

- Extend First Person 2.7.2 + Not Enough Animations 1.12.4 holdouts with steep look-down Gravity Fall/body-landing snapshots.
- Preserve Fresh Animations 1.10.5 + FA Player Extension 1.1 + EMF 3.3.5 + ETF 7.2 internal animation ownership under the new macro body/aerodynamics layer.
- Keep Scale Brews beta.5 in isolated optional server/client lanes with no production compile dependency.
- Add dedicated full-sphere +120/-120/360 and canonical-exit client holdouts, directional mace GameTests, generic-fluid GameTests and aerodynamics/air-diving unit tests.
- Require the prerelease to be created from the exact successful `main` CI artifact rather than a second build.

## [0.1.0-alpha.13] - 2026-09-14

### Free-flight camera and landing

- Keep the local player's actually rendered world frame stable through voluntary airborne gravity changes instead of rotating the camera on every Clinging/Reorientation decision.
- Add server-authoritative short-horizon landing prediction over real entity dimensions, velocity, gravity and collision geometry.
- Add `LANDING_COMMITTED`: new gravity requests during the landing window are discarded rather than queued.
- Revalidate committed support and preserve the exact partial visual quaternion when a real surface invalidation cancels the landing, avoiding snap-back.

### Gravity Fall body and controls

- Add sustained Gravity Fall presentation after 12 airborne ticks with a six-tick body-root blend.
- Orient the macro body from real world velocity, not camera or logical gravity, with stable zero-speed retention and minimal-rotation continuity through curved/reversing trajectories.
- Add BODY_LANDING convergence toward predicted support while keeping the camera independent.
- Keep W/A/S/D relative to the retained visual frame without adding new air steering.
- Explicitly release Gravity Fall/landing presentation at Elytra, water/lava, teleport, vehicle, death/respawn and foreign-ownership boundaries.
- Preserve First Person camera ownership and Fresh Animations/EMF internal limb animation while Clinging applies only a macro root transform.

### Impact physics and landing API

- Replace Clinging's gravity-turn `fallDistance` segmentation with collision-absorbed world velocity as the primary damage model.
- Add public `LandingSurfaceProvider` and `LandingSurfaces` contracts plus a vanilla collision provider.
- Keep the public API independent of Scale Brews; concrete Scale landing integration remains outside this release.

### Validation

- Expand required server coverage to 86 GameTests and cross-domain adversarial holdouts.
- Preserve default, First Person and Fresh Animations screenshot matrices separately in CI.

## [0.1.0-alpha.12] - 2026-09-13

- Added passive 250 ms underwater double-Space gravity selection without consuming vanilla ascent.
- Restricted Clinging recharge to real gravity-relative feet support, including seabed support but not free swimming/body contact.
- Made near-landing sprint-jump reservation scale with effective jump power from one to three ticks.
- Added generic tracked 180/240 ms Clinging-owned snap presentation for mounts and pets, common-axis mounted half-turns and off-screen transition advancement.

## [0.1.0-alpha.11] - 2026-09-13

- Separated rendered target selection from pitch-independent navigation heading.
- Preserved heading through opposite gravity changes and established 180/240 ms quadratic snap timing.
- Carried visual sequence state across respawn.

## [0.1.0-alpha.10] - 2026-09-12

- Introduced Clinging-owned minimal snap transport, gravity-relative sprint-jump protection, strict one-turn Clinging semantics and removal of the old camera-timing JSON.

## [0.1.0-alpha.8] - 2026-09-11

Documentation-sync prerelease with no gameplay changes relative to alpha.7.

## [0.1.0-alpha.7] - 2026-09-11

Ownership/recovery hardening: bounded retirement, external gravity ownership, transactional passenger recovery, prior-frame mount loans, moving-surface replay fences and isolated optional integrations.

## [0.1.0-alpha.6] - 2026-09-09

First public alpha candidate with airborne Space selection, one-turn Clinging, unlimited Reorientation, Elytra priority, beacon support, mounted turns and pet route replay.
