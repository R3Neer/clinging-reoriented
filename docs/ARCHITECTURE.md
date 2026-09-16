# Architecture

Clinging: Reoriented 0.1.0-beta.3 separates **physical gravity**, **camera ownership**, **body presentation**, **landing authority**, **aerodynamic steering**, **impact damage**, **interaction context** and the independent **Gravity Charge projectile lifecycle** instead of treating a gravity-direction write as one monolithic event. Beta.3 preserves the beta.2 gameplay/camera architecture while hardening hot paths and bounded recovery work.

## Authority and design rule

The server owns physical gravity, collision, effect/charge state, landing commitment, aerodynamic velocity changes, safety intervention, damage and Gravity Charge acquisition/capture state. A voluntary gravity decision changes acceleration while preserving the current **world-space velocity vector**.

Presentation may interpolate that decision but cannot invent position, collision or damage. Gravity Fall air-diving redirects existing momentum within a bounded server-authoritative rule and does not create speed/lift.

## Input and intent

`GravityInput` plus client input mixins produce edge-triggered gravity requests. `select_intent_v3` carries `selectionLook` for cardinal selection and `navigationHeading` for gravity-coordinate orientation transport. The server validates sequence/revision, effect ownership, input context, airborne budget, target, hierarchy and collision clearance.

Water uses `WaterDoubleTapDetector` for Space-release-Space while leaving vanilla key state intact. `WorldVerticalWaterMixin` makes owned swimming ascent/descent world +Y/-Y. Climbables use DOWN vanilla, lateral ignore and mirrored UP behaviour.

During sustained Gravity Fall, the client sends sparse world-space gaze/forward samples. The server accepts only fresh, finite, monotonic samples while Gravity Fall owns the context.

## Physical gravity and camera ownership

`GravityTransition` plans gravity/yaw coordinate transport but voluntary turns do not rotate world velocity. Gravity Changer remains the gravity-coordinate authority; Clinging records whether physical and visual state is its responsibility and releases ownership when another source takes over.

`VisualTransitions` provides HOLD, LAND and SNAP presentation modes. Local-player LAND uses a shared **500 ms / 10 tick** window; ordinary tracked non-player SNAP keeps shorter 180/240 ms turn timing. Cancellation caused by invalidated support may hold the exact current quaternion; lifecycle/context transfer releases ownership. A camera HOLD created inside an already-active fluid context preserves the retained world frame for that fluid epoch; a later independent fluid entry still crosses the transfer fence and releases an older dry-flight HOLD.

## Full-sphere Gravity Fall and body root

`GravityFallLookState` owns a continuous local camera orientation only while the local player has active Gravity Fall presentation. Mouse deltas are applied in the camera's own screen frame, so horizontal/vertical intent keeps the same handedness through both pitch poles. The entity's ordinary yaw/pitch fields remain a vanilla-compatible representation of the same **forward vector** rather than storing out-of-range full-sphere pitch.

`GravityFallCameraMixin` composes that camera-base orientation after Gravity Changer's retained visual-gravity frame and rebuilds Minecraft's camera basis. Vanilla third-person boom distance and wall clipping therefore consume the same continuous look frame instead of a second custom orbit implementation. Switching between first and third person changes camera placement, not the meaning of look input. When Gravity Fall releases ownership, the canonical entity yaw/pitch already represents the current gaze, so exit does not snap the view.

`GravityFallState` is a server-authoritative coarse phase machine. Sustained presentation begins after 12 airborne ticks. `GravityFallVisuals`, `BodyOrientation` and `BodyRenderMath` reconstruct the body client-side from world velocity, with a six-tick entry blend and stable zero-speed retention.

During SUSTAIN, look may pull the macro body only outside a 35-degree deadzone, capped at 7.5 degrees/tick. LAND disables this steering and uses the shared 500 ms timing. Fresh Animations/EMF/ETF retain ownership of internal limb/head/equipment animation.

First Person uses a blended body-center/camera pivot for the avatar root; the real camera never receives the macro body transform.

## Aerodynamics, safety, impact and mace

`GravityFallAerodynamics` contains bounded body look-follow, posture drag and W air-diving: max 6 degrees/tick, positive velocity/look dot gate, preserved speed before drag and no generated thrust. `FlightSafety` caps eligible airborne world speed at 3.92 blocks/tick and guards unloaded/hard world boundaries.

`ImpactState` and `ImpactDamage` derive damage from world-space velocity actually absorbed by collision and route equivalent fall distance through vanilla block/fall handling when possible.

`DirectionalMaceFall` tracks literal displacement along the current gravity direction. A gravity change starts a fresh segment; old-axis distance and gravity strength do not contaminate smash height.

## Landing surfaces and fluids

`LandingSurfaceProvider` / `LandingSurfaces` expose bounded support and swept-contact semantics with stable identity for revalidation. Vanilla collision geometry is the base provider. `FluidContext.intersects` fences any non-empty fluid volume before a provider can create Clinging support/landing semantics. The same fluid-volume test also accepts hypothetical AABBs so planners and safe fallbacks can reject a future body without moving the entity first.

`LandingPrediction.MAX_TICKS` is the 10-tick presentation window. `LandingState` commits only while the same candidate remains valid, matches physical gravity, revalidates and stays reachable.

## Gravity Charge lifecycle

`GravityCharges` registers `clinging_reoriented:gravity_charge`, stack size 64 and a 0.5 second use cooldown. `GravityChargeItem` implements `ProjectileItem` for both manual and dispenser use.

`GravityChargeBullet` is **creation-time plumbing only**. It extends `ShulkerBullet` while retaining the exact vanilla `EntityTypes.SHULKER_BULLET`; its `shoot` override prevents generic dispenser code from overwriting the cardinal routing already initialized by Gravity Charge.

Persistent Gravity Charge state lives on the common vanilla `ShulkerBullet` through `ShulkerBulletMixin` and the `GravityChargeProjectile` duck interface. The mixin stores launched identity, normalized original launch intent, optional Target Block lock, retry cadence and exactly-once capture fence. NBT keys use the final `GravityCharge*` names.

Capture is server-authoritative in `hurtServer`. Only melee and `AbstractArrow` interception can materialize a Gravity Charge. The capture fence is armed before the drop, so mixed/racing interceptors cannot duplicate the item. Shield, ordinary impact, expiry and unrelated destruction do not mint a charge.

`GravityChargeTargeting` owns **selection only**, never projectile movement. Acquisition is bounded to 32 blocks and a 15-degree cone. A Target Block hit by the central ray has absolute priority. Otherwise living entities and assisted Target Blocks are ranked by angular error then distance. Living entities require initial line of sight. A valid lock remains sticky.

When a target becomes invalid, the mixin clears that lock and retries from the projectile's **current position** while reusing original intent, normally on the four-tick retry cadence. With no target it enters cardinal free flight. Entity targets reuse vanilla `selectNextMoveDirection`; Target Blocks use a small orthogonal router and are ultimately activated by real projectile collision.

Because runtime type remains vanilla `SHULKER_BULLET`, vanilla projectile damage, Levitation, renderer and shulker duplication see the expected entity type. There is no custom entity type or compatibility shim pretending to be one.

`Reorientation.initialize` uses `GravityCharges.ITEM` as the Clinging -> Reorientation brewing ingredient for normal and long Clinging. The old Shulker Shell recipe is deliberately not registered.

## Assets and localization

The Gravity Charge GUI icon, held-model geometry and item texture are original GPL-3.0-or-later project assets under `assets/clinging_reoriented/.../gravity_charge*` and `docs/art/gravity-charge/`. The item model uses the project's own `gravity_charge` texture. The launched entity's renderer remains vanilla only because its EntityType is vanilla; no Mojang texture/model is copied into the project.

Player-facing translation files are `en_us.json` and `es_es.json`. CI requires exact key parity and non-empty Spanish values. The final item key is `item.clinging_reoriented.gravity_charge` → `Gravity Charge` / `Carga de gravedad`.

## Gravity ownership and lifecycle

`PlayerData` separates physical ownership, visual frame, airborne/landing state, Gravity Fall epoch/look/aero state, mace fall segment and safety frontier state. Teleports/transfers clear transient spatial state before context changes. Respawn/replacement preserves monotonic epochs without migrating obsolete entity-instance animation.

Mounted gravity remains transactional over the root/passenger hierarchy. Pet following is **history-free**: `MobGravityPlanner` forecasts one physical support-to-support transition without mutation, `MobGravityLocalPlanner` combines one gravity-aware tactical path with at most five transition forecasts, and `PetGravityFollow` executes only the next edge through APPROACH → REVALIDATE → COMMITTED → LANDING_CONFIRM/RECOVERY. Failed `ManeuverKey` edges are excluded temporarily instead of being retried forever. `FollowGravityMixin` delegates only special gravity segments; ordinary same-surface follow stays vanilla.

Powered pets no longer use vanilla's DOWN-only teleport blindly. `PetGravityTeleport` first keeps ordinary navigation alive and only treats extreme separation as a fallback condition. A teleport candidate must fit the real body, be fluid/hazard free, have real support in the candidate gravity frame and expose at least one tangent egress. Current gravity is preferred; external/borrowed ownership cannot be stolen. There is no owner breadcrumb queue, TTL, replay coordinate or historical turn recording in runtime state.

## Alchemical Leather semantic-wear boundary

Alchemical Leather is an optional integration and never enters Clinging: Reoriented's compile-time production type graph. `AlchemicalLeatherCompat` checks Fabric Loader first and reflectively resolves only `InfusionWearApi.emit(...)`; missing or incompatible API linkage leaves the bridge inert rather than changing gravity behaviour.

Clinging owns only the semantic facts that are authoritative inside this mod:

- `AlchemicalWearMixin` observes the single authoritative `ClingingReoriented.attempt(ServerPlayer, Vec3, Vec3)` return boundary and publishes `clinging_reoriented:gravity_turn` only for `SUCCESS`;
- the event owner is derived from actual active potion effects, preferring Reorientation when both Reorientation and Alex's Mobs Clinging are present and inventing no owner when neither is active;
- the successful airborne mounted-Reorientation path also returns `SUCCESS`, so it contributes the discrete turn event, while grounded mount actions return `MOUNT_ACTION` and do not;
- an end-server-tick publisher reports `clinging_reoriented:controlled_flight_tick` only while Reorientation is active, Clinging owns physics, the player is airborne, is not a passenger, is not grounded on a moving/support surface and is not supported through Anatomy.

The bridge never selects equipment, reads Alchemical Leather infusion components or applies durability. Compatibility JSON owned by Clinging: Reoriented maps Reorientation to boots and assigns balance to semantic events. Alchemical Leather independently proves that the corresponding infused item is equipped and effective, handles external-effect eclipse, accumulates fractional work and applies ordinary durability damage.

## Optional integrations

Production code does not compile against Scale Brews or Alchemical Leather. First Person is mixin-gated. Fresh Animations/FA Player Extension/EMF/ETF are pinned optional test fixtures. Gravity Charge adds no new runtime dependency.

## State summary

The major gravity states are GROUNDED, AIRBORNE, SUSTAINED_GRAVITY_FALL, LANDING_COMMITTED and context transfer. These remain orthogonal to effect acquisition, Gravity Charge projectile state, mount loans, the pet follow executor and external gravity ownership.

**0.1.0-beta.1** introduced Gravity Charge and the beta line. **0.1.0-beta.2** replaced the pole-singular Gravity Fall look representation while keeping gameplay authority unchanged. **0.1.0-beta.3** preserves those semantics while reducing hot-path allocation/CPU work and bounding rare recovery searches.
