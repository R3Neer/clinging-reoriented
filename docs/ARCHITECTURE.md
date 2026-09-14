# Architecture

Clinging: Reoriented 0.1.0-alpha.13 separates **physical gravity**, **player camera**, **body presentation**, **landing authority** and **impact damage** instead of treating a gravity-direction write as one visual/physical event.

## Authority and design rule

The server owns physical gravity, collision, effect/charge state, landing commitment and damage. A voluntary gravity decision changes acceleration while preserving the entity's existing **world-space velocity vector**. Presentation can describe that decision, but it cannot mutate physical position, velocity, collision or damage.

The core design boundary is: **Elytra controls continuous movement; Clinging/Reorientation control acceleration discretely.** Alpha.13 introduces no camera steering, lift, boost, surface magnetism or automatic braking.

## Input and intent

`GravityInput` plus client input mixins produce edge-triggered requests. The request protocol remains `select_intent_v3`, carrying two world-space intents captured at the same edge:

- `selectionLook`: the actually rendered camera forward, used only to choose NORTH/SOUTH/EAST/WEST/UP/DOWN;
- `navigationHeading`: pitch-independent heading used for gravity-frame orientation transport where required.

The server validates sequence/revision, effect ownership, input context, Clinging airborne budget, target direction, mount hierarchy and collision clearance. It sanitizes client headings before geometry uses them.

Water uses `WaterDoubleTapDetector`, a passive rising-edge observer. Vanilla retains the key state. Sprint-jump reservation remains a bounded real-support prediction rather than a cooldown.

## Physical gravity and momentum

`GravityTransition` plans gravity/yaw coordinate transport but voluntary turns do not rotate world velocity. A 180-degree gravity reversal therefore produces physical deceleration through zero followed by acceleration in the new direction.

Gravity Changer remains the gravity-coordinate authority. Clinging tracks whether a physical/visual frame is its responsibility and revokes ownership when another source legitimately takes over.

## Free-flight camera ownership

The local player's visual path has three distinct `VisualTransitions` modes:

- `HOLD`: a successful free-flight gravity change captures the quaternion currently being displayed, applies the logical yaw gauge change and compensates it so the same world frame remains visible;
- `LAND`: a retained frame SLERPs to the canonical future-floor frame only after server landing commitment;
- `SNAP`: retained for immediate Clinging-owned cleanup/legacy local transitions and tracked non-player presentation.

The corresponding local payloads are `visual_hold_v1`, `visual_land_v1`, `visual_cancel_v1` and the immediate `visual_transition_v2`. `VisualTransitions` uses a monotonic connection-scoped sequence. A cancel caused by real landing invalidation can hold the exact current quaternion; a transfer/lifecycle cancel releases ownership completely.

Quarter LAND/SNAP timing is 180 ms and opposite half-turn timing 240 ms, quadratic ease-out. A new Reorientation decision during free flight updates physical gravity while the rendered world frame remains stable. It does not queue a series of camera turns.

## Landing surfaces and prediction

The public API consists of `LandingSurfaceProvider` and `LandingSurfaces`. Providers expose bounded support/predicted-contact semantics and stable identity suitable for revalidation. `VanillaLandingSurfaceProvider` supplies the base implementation over real collision geometry. `SweptAabb` is used for continuous contact geometry rather than endpoint-only guessing.

Provider rules are fail-closed: invalid/non-finite data, stale identity, exceptions or a contact whose gravity no longer matches cannot become a valid floor. External providers do not bypass Clinging's preflight and the public contract contains no Scale Brews classes.

`LandingPrediction` simulates only the short window needed for presentation. `LandingState` is server authority for the final landing window. A candidate must remain the same contact, retain matching gravity, pass `LandingSurfaces.revalidate`, stay within its deadline and remain physically reachable.

When ETA reaches the presentation horizon, `LandingState` marks `landingCommitted` and emits `visual_land_v1`. Input-side `LandingState.committed()` closes the one-tick gap between actual touchdown and the end-server-tick state update. Gravity requests during commitment are discarded, not queued.

If a surface invalidates while Clinging still owns physics, `LandingState.cancel(..., true)` emits a cancel that preserves the current rendered frame. Elytra, water/lava, vehicles, teleport/transfer, death or ownership loss use transfer cleanup instead because a different subsystem/context now owns presentation.

## Gravity Fall state and body root

`GravityFallState` is a server-authoritative **coarse phase machine**, not a render-quaternion authority. Sustained presentation begins after `START_AIRBORNE_TICKS = 12` when the player remains airborne and Clinging/Reorientation still owns physics. It publishes discrete START/LAND/RESUME/RESET events through `GravityFallSync`; it does not send a quaternion every tick.

`GravityFallVisuals`, `BodyOrientation` and `BodyRenderMath` reconstruct the macroscopic body frame client-side. The body head-feet axis follows **world velocity** with a six-tick entry blend. Near degenerate speed it retains the last reliable frame. Orientation transport uses minimal rotation/parallel-transport style continuity rather than rebuilding from a global up vector, avoiding accidental barrel rolls.

When a valid floor enters the short body-landing horizon, the body converges toward that floor frame. BODY_LANDING is presentation and can start from a valid prediction before the stricter camera `LANDING_COMMITTED` threshold. Losing that candidate can RESUME sustained body tracking without forcing a camera event.

`GravityFallRenderMixin` applies the macro transform around the logical body center rather than the feet, preventing the model from orbiting around its own pivot. It does not replace limb animation. Fresh Animations/EMF/ETF remain owners of their internal animation layers.

First Person compatibility shares the same visual gravity/camera state but the macro body root is prevented from feeding back into the camera.

## Control frame

Alpha.13 adds no air steering. Movement magnitude remains whatever the existing player/physics stack produces. `VisualMovementFrame` / `VisualMovementMixin` translate movement intent using the frame that the player actually sees so camera retention does not make W/A/S/D secretly obey an unrelated logical gravity basis.

## Impact lifecycle

`ImpactState` arms a short physical lifecycle while Clinging/Reorientation owns or has just owned the relevant fall. `Entity.move` instrumentation records the world-space start and intended movement. `ImpactDamage` compares intended movement with actual post-collision displacement through `ImpactPhysics.absorbedVelocity`.

Only movement blocked in its travel direction contributes to absorbed impact speed. The combined blocked vector is handled as one event. `ImpactPhysics.vanillaEquivalentFallDistance` maps the absorbed speed back onto vanilla fall kinematics. `ImpactDamage` then prefers the impacted block's `fallOn` callback and otherwise calls vanilla fall damage.

While armed, ordinary `fallDistance` accumulation is suppressed/reset so it cannot double-count the same event. `afterMove` is the arbitrary-normal fallback for cases such as a late gravity turn where Gravity Changer's gravity-relative `onGround` no longer describes the surface that actually blocked world motion. A per-move sequence fence prevents the ordinary hook and fallback from charging twice.

This architecture intentionally supersedes alpha.11/alpha.12's gravity-turn fall-distance segmentation as the primary damage model.

## Gravity ownership and lifecycle

Player state in `PlayerData` separates physical ownership, effective visual-frame ownership, airborne/landing state and Gravity Fall epoch. Teleport/transfer clears transient landing presentation before spatial context changes. Respawn/replacement carries connection-level monotonic epochs without migrating an obsolete entity-instance animation.

Remote player Gravity Fall uses discrete snapshots/epochs so a tracker entering mid-fall reconstructs current coarse presentation without per-tick packets. UUID/epoch checks reject stale identity.

## Mounts and pets

Mounted gravity remains transactional over the root/passenger hierarchy. `MountedGravity` creates one physical plan; opposite mounted turns use one rider-selected 180-degree axis and rebase each entity's own heading through that plan.

Non-player `MobGravity` presentation remains a tracked SNAP, not local free-flight HOLD/LAND. `entity_visual_transition` includes entity ID, UUID, target, yaw delta, turn kind and a per-mob monotonic sequence. Client resolution requires both current entity identity and fresh sequence. `VisualTransitions.tickAll()` advances owned transitions even outside the renderer/frustum.

`GravityBreadcrumbs` keeps bounded pet route replay and lifecycle clearing. S05 exercises replay through vanilla `FollowOwnerGoal` rather than teleport-only fixtures.

## Optional integrations

Required production code does not compile against Scale Brews. Existing legacy Scale/Anatomy reflection and pseudo mixin paths remain isolated and fail closed. The alpha.13 landing-surface API is deliberately independent from those bridges; a concrete Scale provider is out of scope.

First Person is mixin-gated and owns its own camera/model baseline. Fresh Animations/Player Extension, EMF and ETF are optional test fixtures, not runtime dependencies. Clinging only owns its macro transform.

## State summary

The observable conceptual states are:

1. `GROUNDED`: real gravity-relative support;
2. `AIRBORNE`: physical gravity can change while the camera frame remains retained;
3. `SUSTAINED_GRAVITY_FALL`: body root follows world velocity;
4. `LANDING_COMMITTED`: camera landing snap is active and voluntary gravity input is blocked.

These are deliberately orthogonal to effect acquisition, mount loans, pet breadcrumbs and external gravity ownership.
