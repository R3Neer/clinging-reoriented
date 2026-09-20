# Architecture

This document describes the current **0.1.0-beta.5 prerelease architecture**. Beta.5 is a narrow Gravity Charge Peaceful-mode hotfix over beta.4; the navigation/gamefeel architecture below is otherwise unchanged.

Clinging: Reoriented separates **physical gravity**, **camera ownership**, **persistent body attitude**, **aerodynamic response**, **trajectory prediction**, **landing authority**, **impact damage**, **interaction context**, **gravity-aware mob locomotion** and the independent **Gravity Charge projectile lifecycle** instead of treating a gravity-direction write as one monolithic event.

## Authority and design rule

The server owns physical gravity, collision, effect/charge state, landing commitment, aerodynamic velocity changes, mob gravity planning/commit, safety intervention, damage and Gravity Charge acquisition/capture state. A voluntary gravity decision changes acceleration while preserving the current **world-space velocity vector**.

Presentation may interpolate physical decisions but cannot invent position, collision, damage or gravity capability. The beta.6 physical/predictive rule is deliberately shared: player landing and mob gravity transitions use the same conceptual motion primitives rather than maintaining separate approximate physics.

## Input and intent

`GravityInput` plus client input mixins produce edge-triggered gravity requests. `select_intent_v3` carries `selectionLook` for cardinal selection and `navigationHeading` for gravity-coordinate orientation transport. The server validates sequence/revision, effect ownership, input context, airborne budget, target, hierarchy and collision clearance.

Water uses `WaterDoubleTapDetector` for Space-release-Space while leaving vanilla key state intact. During owned water movement, horizontal movement is reconstructed in the camera frame: W/S use camera forward/back including pitch and A/D use camera left/right. `WorldVerticalWaterMixin` keeps Space/Shift on world +Y/-Y. Logical gravity is not the water control frame.

Climbables use DOWN vanilla, lateral ignore and mirrored UP behaviour.

During sustained Gravity Fall, the client sends sparse world-space gaze/forward samples. The server accepts only fresh, finite, monotonic samples while Gravity Fall owns the context.

## Physical gravity and camera ownership

`GravityTransition` plans gravity/yaw coordinate transport but voluntary turns do not rotate world velocity. Gravity Changer remains the gravity-coordinate authority; Clinging records whether physical and visual state is its responsibility and releases ownership when another source takes over.

`VisualTransitions` provides HOLD, LAND, SNAP and RECOVER_HOLD presentation modes. BODY_LANDING may anticipate a clear floor up to **10 ticks** out, but local-player camera/input LAND commits only inside **5 ticks** for a clear approach or inside **3 ticks** after two confirmations for an ambiguous approach. Invalidated committed LAND enters RECOVER_HOLD and eases back to the retained pre-landing quaternion over **4 ticks / 200 ms**; lifecycle/context transfer still releases ownership immediately. Ordinary tracked non-player SNAP keeps its separate 180/240 ms turn timing.

A camera HOLD created inside an already-active fluid context preserves the retained world frame for that fluid epoch; a later independent fluid entry still crosses the transfer fence and releases an older dry-flight HOLD.

The underwater camera frame is separate from logical gravity. Unsupported/free swimming converges to world-up; real gravity-relative support converges to support-up. Brief support hysteresis prevents one-tick geometry noise from flipping ownership, and neither visual state rewrites the stored gravity attribute.

## Full-sphere Gravity Fall and body attitude

`GravityFallLookState` owns a continuous local camera orientation only while the local player has active Gravity Fall presentation. Mouse deltas are applied in the camera's own screen frame, so horizontal/vertical intent keeps the same handedness through both pitch poles. The entity's ordinary yaw/pitch fields remain a vanilla-compatible representation of the same **forward vector** rather than storing out-of-range full-sphere pitch.

`GravityFallCameraMixin` composes that camera-base orientation after Gravity Changer's retained visual-gravity frame and rebuilds Minecraft's camera basis. Vanilla third-person boom distance and wall clipping therefore consume the same continuous look frame instead of a second custom orbit implementation. Switching between first and third person changes camera placement, not the meaning of look input. When Gravity Fall releases ownership, the canonical entity yaw/pitch already represents the current gaze, so exit does not snap the view.

`GravityFallState` is a server-authoritative coarse phase machine. Sustained presentation begins after 12 airborne ticks. `GravityFallVisuals`, `BodyOrientation` and `BodyRenderMath` maintain a persistent body attitude with stable zero-speed retention rather than rebuilding the body frame directly from velocity every tick.

`GravityFallAerodynamics.advanceBody` applies two bounded attitude influences:

- gaze may pull the macro body only outside a **35-degree neck deadzone**, capped at **7.5 degrees/tick**;
- velocity applies only weak **1.25 degrees/tick** weathercock stabilization and selects the closer head/feet axis, so stabilization cannot manufacture a 180-degree flip.

LAND uses the shared final landing timing instead of continuing free-flight attitude steering. Fresh Animations/EMF/ETF retain ownership of internal limb/head/equipment animation. First Person uses a blended body-center/camera pivot for the avatar root; the real camera never receives the macro body transform.

## Aerodynamics, shared trajectory physics, safety, impact and mace

`GravityFallAerodynamics.applyAerodynamics` implements body-relative anisotropic drag. Velocity is decomposed into longitudinal and transverse components along the visible body's long axis; longitudinal momentum is retained and the transverse component receives **2.5% additional drag per tick**. This bends trajectory as a consequence of posture while losing energy. There is no W-specific airborne steering, angular velocity magnet, generated thrust or Elytra-style lift.

`AirMotion` is the shared next-velocity seam used by real/predicted airborne motion. `TrajectoryPrediction` advances a real AABB through a bounded horizon and asks `LandingSurfaces.sweep` for the first contact. This predictor is reused by player landing, mob transition evaluation and committed-flight monitoring. Unknown geometry is not treated as air.

`FlightSafety` caps eligible airborne world speed at 3.92 blocks/tick and guards unloaded/hard world boundaries.

`ImpactState` and `ImpactDamage` derive damage from world-space velocity actually absorbed by collision and route equivalent fall distance through vanilla block/fall handling when possible.

`DirectionalMaceFall` tracks literal displacement along the current gravity direction. A gravity change starts a fresh segment; old-axis distance and gravity strength do not contaminate smash height.

## Landing surfaces, prediction and fluids

`LandingSurfaceProvider` / `LandingSurfaces` expose bounded support and swept-contact semantics with stable identity for revalidation. Vanilla collision geometry is the base provider. `FluidContext.intersects` fences any non-empty fluid volume before a provider can create Clinging support/landing semantics. The same fluid-volume test accepts hypothetical AABBs so planners and safe fallbacks can reject a future body without moving the entity first.

Landing separates **geometric acquisition**, **player contact intent**, **body approach** and **camera/input commitment**:

- `LandingPrediction.ACQUISITION_TICKS = 40` still discovers the first physically valid feet support up to two seconds ahead using the shared `TrajectoryPrediction`/surface seam;
- `LandingPolicy` is applied only to the local-player landing interpretation after that shared hit exists: normal-speed ratio <= **0.12** is GRAZE, >= **0.30** is CLEAR, and the middle band is AMBIGUOUS; total speed below **0.12 blocks/tick** is forced to AMBIGUOUS;
- GRAZE never becomes a landing candidate. Its exact `SurfaceKey`/gravity is remembered for one tick so a matching transient vanilla `onGround` flag cannot immediately become semantic support, recharge Clinging or canonicalize the visual frame;
- if the same feet support persists beyond that one-tick graze memory, ordinary `AirChanges.grounded` wins and it becomes a real landing;
- CLEAR may enter BODY_LANDING inside 10 ticks but `LANDING_COMMITTED` waits until ETA <= 5;
- AMBIGUOUS requires two current confirmations, may enter BODY_LANDING inside 4 ticks and commits only at ETA <= 3;
- before `LANDING_COMMITTED`, Reorientation input remains legal; after commit it is discarded rather than queued;
- `LandingState` still stores stable surface identity, gravity, ETA and bounded miss hysteresis, and a hysteresis-only candidate cannot commit;
- the first swept contact remains authoritative, so a blocking shoulder/side contact cannot be ignored to select a support behind it;
- Gravity Fall reuses the candidate computed by landing state rather than performing an independent second world sweep in the same tick;
- the player-facing contact-intent layer does **not** alter the shared sweep consumed by mob transition planning.

## Gravity-aware mob navigation

The beta.4 mob system deliberately separates **high-level intent** from **locomotion**. Vanilla goals still decide why a mob wants to move; the gravity layer only expands how a mob with legitimate Clinging/Reorientation capability may satisfy that intent when ordinary navigation fails.

### Physical transition planner

`MobGravityPlanner` evaluates one complete candidate transition without mutating the mob:

**supported launch → gravity change → predicted airborne segment → first contact → habitable landing**.

It reuses `TrajectoryPrediction`, `AirMotion` and `LandingSurfaces.sweep`, evaluates the real mob AABB in the candidate orientation, fails closed on unknown geometry, rejects blocking/trapped landings and computes vanilla-equivalent impact plus robustness. Physical impossibility is a veto; damage/risk is a strongly nonlinear cost rather than an automatic impossibility.

`MobGravityLocalPlanner` puts that evaluator behind ordinary navigation. It asks one ephemeral mirror navigation for a tactical path. If ordinary walking really satisfies the goal, gravity work stops there. Otherwise it considers up to the final **four launch nodes** of the partial path and the **five alternate gravity directions**, giving an explicit maximum of **20 physical transition forecasts per local plan**. The returned approach path is truncated to the chosen launch frontier.

`ManeuverKey(currentGravity, frontierNode, targetGravity)` identifies a concrete edge. Failed/non-progressive edges are excluded temporarily without banning the same gravity from nearby launch nodes.

### General intent bridge

`MobGravityNavigation` is the runtime bridge for ordinary mob goals. Its transient phase machine is:

`IDLE → WAITING_PLAN → APPROACH → REVALIDATE → COMMITTED → LANDING_CONFIRM / RECOVERY`.

Entity and position intents remain owned by the calling vanilla goal. Reachable vanilla paths are never replaced for spectacle. Wake adapters exist only for goal types such as `MeleeAttackGoal`/`AvoidEntityGoal` that can abort in `canUse()` before ever emitting `moveTo`; they expose the missing intent to the common planner rather than reimplementing species AI.

Repeated requests during an owned gravity segment update/observe intent without overwriting the approach or returning airborne control to surface navigation. Revalidation occurs immediately before a gravity commit from the real supported position.

### Pet follow

`PetGravityFollow` consumes the same physical/local planner for `FollowOwnerGoal`. Pet following is **history-free**: runtime contains no owner breadcrumb queue, TTL, replay coordinate or historical gravity-turn instruction.

A pet uses a filtered current owner target and current geometry. Ordinary same-surface following stays vanilla. When gravity is necessary, the pet walks to the selected launch frontier, revalidates, commits one legal transition, lets physics own the flight and requires stable support before replanning. An airborne owner remains trackable through filtered/tangential targeting; owner gravity changes do not remotely command the pet to turn.

`PetGravityTeleport` is a guarded extreme-separation fallback, not hidden planner locomotion. A candidate must fit the body, be fluid/hazard free, have real support in its candidate gravity frame and expose tangent egress. External/borrowed gravity ownership cannot be stolen.

### Dynamic flight observation and reaction

`MobFlightMonitor` observes only a committed airborne segment. It uses current AABB, velocity, gravity, `AirMotion.nextVelocity` and `LandingSurfaces.sweep`; it never runs surface pathfinding or changes gravity.

`MobFlightReaction` stores material target/danger observations plus a fixed action deadline. `MobReactionTime` derives latency from the mob's **base `MOVEMENT_SPEED`**, not `deltaMovement`, fall speed or transient momentum, using a smooth bounded curve of **2–10 ticks**. Later observations may update pending data without indefinitely pushing the deadline forward; returning to a safe/non-material state cancels the pending action.

`MobFlightReactor` joins those pieces. Its dynamic monitor horizon is:

`min(20, reactionTicks + 2)`

while the exact committed `LandingSurfaces.Contact` is revalidated independently even when it lies beyond that short horizon. Reorientation may evaluate a new airborne correction only after the reaction delay and only through the same physical safety rules. Spent Clinging cannot acquire a second turn. If impact occurs before the legal reaction time, the collision is accepted as real gameplay.

### Planning budget

`MobGravityPlanningBudget` limits **new grounded gravity plans**, not vanilla navigation or already committed flight:

- maximum **32** new gravity plans per server level/tick;
- maximum **4** per **64×64 X/Z region/tick**;
- a local plan remains internally bounded to `<=20` transition forecasts;
- excess generic work moves to `WAITING_PLAN` with its intent preserved and retries on following ticks;
- `PetGravityFollow` uses the same budget rather than a private unlimited path;
- mobs without usable gravity capability do not consume a planning slot.

`MobGravityNavigation` bounds failed-edge memory to eight exclusions per executor and keeps negative/replan throttles, preventing repeated misses from turning the budget into a busy loop.

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

`MobGravity.State` holds mob gravity ownership plus transient navigation state. Gravity planning never steals external ownership. Support-to-support commits use the same legal Clinging/Reorientation capability rules as runtime: Clinging consumes its one airborne decision; Reorientation can support later legal corrections; stable support restores the normal next segment according to effect rules.

Mounted gravity remains transactional over the root/passenger hierarchy.

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

Player gravity states include GROUNDED, AIRBORNE, SUSTAINED_GRAVITY_FALL, predictive landing approach, LANDING_COMMITTED, RECOVER_HOLD presentation and context transfer. Mob gravity locomotion adds its own transient planning/execution phases without changing effect capability. These remain orthogonal to Gravity Charge projectile state, mount loans, optional semantic wear and external gravity ownership.

**0.1.0-beta.1** introduced Gravity Charge and the beta line; beta.2 hardened full-sphere camera input; beta.3 hardened performance; beta.4 shipped posture-driven aerodynamics, 40-tick landing acquisition, water controls and gravity-aware mob locomotion; beta.5 fixed Gravity Charge in Peaceful. **0.1.0-beta.6** adds the local-player contact-intent layer and recover-to-HOLD landing cancellation while preserving the shared physical predictor.
