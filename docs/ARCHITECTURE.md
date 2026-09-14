# Architecture

Clinging: Reoriented 0.1.0-alpha.15 separates **physical gravity**, **camera ownership**, **body presentation**, **landing authority**, **aerodynamic steering**, **impact damage**, **interaction context** and the independent **Shulker Charge projectile lifecycle** instead of treating a gravity-direction write as one monolithic event.

## Authority and design rule

The server owns physical gravity, collision, effect/charge state, landing commitment, aerodynamic velocity changes, safety intervention, damage and Shulker Charge acquisition/capture state. A voluntary gravity decision changes acceleration while preserving the current **world-space velocity vector**.

Presentation may interpolate that decision but cannot invent position, collision or damage. Gravity Fall air-diving is the one deliberate continuous control layer: it redirects existing momentum within a bounded server-authoritative rule and does not create speed/lift.

## Input and intent

`GravityInput` plus client input mixins produce edge-triggered gravity requests. `select_intent_v3` carries two world-space intents captured from the rendered frame:

- `selectionLook`: chooses NORTH/SOUTH/EAST/WEST/UP/DOWN;
- `navigationHeading`: pitch-independent heading used for gravity-coordinate orientation transport.

The server validates sequence/revision, effect ownership, input context, airborne budget, target, hierarchy and collision clearance.

Water uses `WaterDoubleTapDetector` for a Space-release-Space gesture while leaving vanilla key state intact. `WorldVerticalWaterMixin` makes owned swimming ascent/descent use world +Y/-Y. `ClimbablePolicy` plus climbable mixins implement DOWN vanilla, lateral ignore and mirrored UP behaviour.

During sustained Gravity Fall, `GravityFallAerodynamicsClientInitializer` sends sparse `gravity_fall_look_v1` samples containing world-space camera gaze and clamped positive forward intent. The server accepts only fresh, finite, monotonic samples while Gravity Fall owns the context.

## Physical gravity and momentum

`GravityTransition` plans gravity/yaw coordinate transport but voluntary turns do not rotate world velocity. A 180-degree reversal therefore decelerates through zero before accelerating in the new direction.

Gravity Changer remains the gravity-coordinate authority. Clinging records whether physical and visual state is its responsibility and releases ownership when another source takes over.

## Free-flight camera ownership

`VisualTransitions` has three presentation modes:

- `HOLD`: preserve the current rendered world frame across a physical gravity write;
- `LAND`: interpolate a retained frame toward the committed future floor;
- `SNAP`: immediate/legacy local cleanup plus tracked non-player presentation.

Payloads are monotonic and connection/identity fenced. A cancellation caused by invalidated landing support may hold the exact current quaternion; a lifecycle/context transfer releases ownership.

Local-player LAND uses `LandingTiming.PRESENTATION_NANOS = 500_000_000` while ordinary tracked SNAP retains its shorter turn-kind timing. Camera LAND remains independent from physical gravity, which already changed when requested.

## Full-sphere Gravity Fall camera

`GravityFallLookMixin` owns full-sphere pitch only while the local player has active Gravity Fall presentation. Vanilla `Entity.turn` normally funnels pitch through the +/-90-degree convention; the mixin updates the underlying Euler fields directly during this ownership window and normalizes only whole 360-degree turns.

`GravityFallLookState` detects active-to-inactive release. `GravityFallLookMath.vanillaEquivalent` then maps any full-sphere orientation back to an equivalent vanilla yaw/pitch pair without changing the look vector. For example, pitch +120 degrees becomes pitch +60 degrees with yaw rotated 180 degrees.

This state is camera-only. It does not change cardinal gravity selection or physical acceleration by itself.

## Landing surfaces, fluids and prediction

`LandingSurfaceProvider` / `LandingSurfaces` expose bounded support and swept-contact semantics with stable identity for revalidation. `VanillaLandingSurfaceProvider` uses actual collision geometry and `SweptAabb` provides continuous contact testing.

`FluidContext.intersects` is a shared context fence. Any non-empty fluid volume intersecting the body suspends Clinging support/landing semantics before providers can make a floor valid. Water, lava and modded fluids therefore share one rule, and submerged solid contact cannot recharge Clinging or start landing reorientation.

`LandingPrediction.MAX_TICKS` is `LandingTiming.PRESENTATION_TICKS = 10`. `LandingState` commits only while the candidate remains the same contact, matches physical gravity, revalidates and stays reachable. Input-side `LandingState.committed()` closes the touchdown/end-tick gap.

When the context changes to fluid, Elytra, vehicle, teleport/lifecycle or foreign ownership, retained landing presentation is released rather than frozen as if support had merely disappeared.

## Gravity Fall phase and body root

`GravityFallState` is a server-authoritative **coarse phase machine**. Sustained presentation begins after `START_AIRBORNE_TICKS = 12` while physics ownership remains valid. It publishes START/LAND/RESUME/RESET through `GravityFallSync`; it does not stream body quaternions every tick.

`GravityFallVisuals`, `BodyOrientation` and `BodyRenderMath` reconstruct the body client-side. The head-feet axis primarily follows **world velocity** with a six-tick entry blend and retains the last reliable frame at degenerate speed.

During SUSTAIN, look may pull the macro body only outside `LOOK_DEADZONE_RADIANS = 35 degrees`, with at most 7.5 degrees of follow per tick. During LAND, this steering is disabled and body interpolation uses the shared 10-tick/500 ms landing timing.

`GravityFallRenderMixin` applies only the macro root. Fresh Animations/EMF/ETF remain owners of internal limb/head/equipment animation.

## First Person pivot isolation

First Person renders a local avatar translated relative to the real camera. Rotating the Gravity Fall root around the ordinary body center caused look-down clipping; rotating exactly around the camera hid too much body.

`BodyRenderMath.localCameraPivot` expresses the camera point in the already-rotated avatar frame. `firstPersonPivot` blends body center toward that exact camera pivot with `FIRST_PERSON_CAMERA_PIVOT_BLEND = 0.72`. `GravityFallRenderMixin` uses that pivot only during the First Person body pass. The real camera never receives the avatar macro transform.

## Aerodynamics and air-diving

`GravityFallAerodynamics` contains pure bounded rules used by the server and mirrored visually by the client:

- body look-follow: 35-degree deadzone, max 7.5 degrees/tick;
- posture drag: factor 1.0 when streamlined, down to 0.987 when perfectly broadside;
- W air-diving: max 6 degrees/tick, gated by positive `dot(velocityDirection, look)`;
- redirect is a great-circle bend of the velocity direction and preserves its magnitude before drag;
- speed below 0.20 blocks/tick is not redirected.

`GravityFallAerodynamicsInitializer` runs after `GravityFallInitializer`, so the current tick's Gravity Fall phase is resolved before drag/steering. LAND, fluids, Elytra, passengers, death and lost physics ownership shut the layer off.

## Flight safety

`FlightSafety` is server-authoritative. It caps Clinging-controlled airborne world speed to **3.92 blocks/tick** while preserving vector direction, both before movement and at end-of-tick intervention points.

It also tracks the last valid loaded/bounded position. Motion toward an unavailable chunk frontier is held with capped momentum retained until the next destination is available. Hard world/build-border violations are projected back inside and outward momentum is discarded. Safety intervention clears `ImpactState` so rescue is not itself interpreted as a damaging collision.

## Impact lifecycle

`ImpactState` arms a short physical lifecycle while Clinging/Reorientation owns or has just owned the relevant fall. Movement instrumentation records intended world displacement; `ImpactDamage` compares it with actual post-collision displacement through `ImpactPhysics.absorbedVelocity`.

Only blocked movement in the travel direction contributes. The combined blocked vector is handled once, converted to a vanilla-equivalent fall distance and preferably routed through the impacted block's vanilla `fallOn` path.

This supersedes old gravity-turn fall-distance segmentation as the primary Clinging damage model.

## Directional mace fall height

Vanilla/Gravity Changer `fallDistance` is unsuitable for mace smash semantics when gravity changes axis or strength. `DirectionalMaceFall` therefore tracks literal positional displacement projected onto the **current gravity direction**.

A direction change/new fall begins a fresh segment. `DirectionalMaceMixin` redirects every relevant 26.2 mace `fallDistance` field read to that geometric value. The mixin handles the actual bytecode owners separately: `LivingEntity` for smash eligibility/damage routines and `Entity` for knockback.

## Shulker Charge lifecycle

`ShulkerCharges` registers one stackable item and gives it vanilla `ProjectileItem` dispenser behaviour. `ShulkerChargeItem` launches a `ShulkerChargeBullet` for both manual and dispenser use. That Java subtype is creation-time plumbing only: its `EntityType` remains the exact vanilla `EntityTypes.SHULKER_BULLET`. Its only override prevents generic dispenser shooting from overwriting the cardinal routing already initialized by the Charge.

All persistent Charge state lives on the common vanilla `ShulkerBullet` through `ShulkerBulletMixin` and the `ShulkerChargeProjectile` duck interface. The state records whether a bullet was launched as a Charge, the normalized original launch intent, optional Target Block lock, reacquisition cadence and the one-drop capture fence. Save/load persists launched state, intent and block target; vanilla already persists its entity target.

Capture is server-authoritative in `hurtServer`. Only melee and `AbstractArrow` damage paths can materialize a Charge, and `clinging$captured` fences mixed/racing interceptors to one item. Normal impact/expiry and unrelated damage do not mint an item.

`ShulkerChargeTargeting` owns **selection only**, never movement. Acquisition is bounded to 32 blocks and a 15-degree cone. The central ray gives a directly hit Target Block absolute priority. Living entities require initial line of sight; assisted entities/blocks are ranked by angular error then squared distance. A valid lock is never replaced merely because another candidate later scores better.

When a locked target becomes invalid, `ShulkerBulletMixin` clears only that lock and retries from the Charge's **current position** while reusing the original intent, normally after the four-tick reacquisition interval. With no target it enters cardinal free flight. Entity targets reuse vanilla `selectNextMoveDirection`; Target Blocks use a small orthogonal router that follows the same cardinal shulker language and lets the final projectile collision activate the real block. Continuous curved homing is deliberately absent.

Because the runtime type is vanilla `SHULKER_BULLET`, vanilla projectile hit/levitation behaviour and shulker duplication continue to see the expected entity type. There is no separate custom projectile type to bridge back into those mechanics.

`Reorientation.initialize` makes Shulker Charge the Clinging -> Reorientation ingredient for both normal and long Clinging; redstone extends normal Reorientation. The old Shulker Shell recipe is not registered.

## Gravity ownership and lifecycle

`PlayerData` separates physical ownership, visual frame, airborne/landing state, Gravity Fall epoch/look/aero state, mace fall segment and safety frontier state. Teleports/transfers clear transient spatial state before context changes. Respawn/replacement preserves monotonic epochs without migrating obsolete entity-instance animation.

Remote Gravity Fall uses discrete semantic epochs rather than per-tick quaternion packets. UUID/sequence checks reject stale tracked identity.

## Mounts and pets

Mounted gravity remains transactional over the root/passenger hierarchy. Non-player `MobGravity` presentation remains tracked SNAP rather than local HOLD/LAND/full-sphere camera behaviour. Entity ID + UUID + monotonic sequence fences presentation, and `VisualTransitions.tickAll()` advances owned animations off-screen.

`GravityBreadcrumbs` keeps bounded pet route replay and lifecycle clearing. While a valid step is pending, the existing vanilla `FollowOwnerGoal` temporarily targets that step projected onto the pet's current gravity-relative movement plane. Arrival uses that goal's bounded stop distance (capped at two blocks), so a path that vanilla considers complete cannot stall just outside a narrower body-width trigger. The pet then replays the turn; if rotating at the current feet position intersects the old support, replay may use the same single center-aligned, collision-preflighted placement allowed to players. That internal placement preserves the breadcrumb transaction, while external teleports still invalidate old steps. The pet releases navigation while unsupported and resumes with Gravity Changer's current directional navigation after landing. With no eligible step, vanilla owner following remains unchanged.

## Optional integrations

Production code does not compile against Scale Brews. Scale compatibility remains test/runtime isolated and the public landing API contains no Scale classes.

First Person is mixin-gated. Fresh Animations/FA Player Extension/EMF/ETF are pinned optional test fixtures. Clinging owns only the macro transform around their animation.

Shulker Charge adds no new runtime dependency. Its 2D icon/texture and 3D item geometry are project-owned assets; only the launched exact vanilla ShulkerBullet uses Minecraft's own renderer/model/texture at runtime.

## State summary

The major gravity states are:

1. `GROUNDED`: real gravity-relative support outside fluid context;
2. `AIRBORNE`: gravity can change while the camera frame remains retained;
3. `SUSTAINED_GRAVITY_FALL`: velocity-owned body + bounded look-follow/aerodynamics + optional W momentum redirection;
4. `LANDING_COMMITTED`: 500 ms camera/body landing presentation with voluntary gravity input blocked;
5. context transfer: fluid/Elytra/vehicle/teleport/lifecycle/foreign ownership releases incompatible presentation.

These states remain orthogonal to effect acquisition, Shulker Charge projectile state, mount loans, pet breadcrumbs and external gravity ownership.
