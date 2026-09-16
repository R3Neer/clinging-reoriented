# Player guide

This guide describes the **unreleased beta.4 navigation/gamefeel development state** of Clinging: Reoriented. **0.1.0-beta.3** remains the latest published prerelease.

The beta.4 campaign changes Gravity Fall aerodynamics, landing acquisition, water controls/camera and gravity-aware mob navigation while preserving the core rule: Clinging grants one voluntary airborne gravity decision and Reorientation removes that one-turn limit. Development builds remain prerelease software and are still subject to bug fixes and tuning.

## Controls and gravity turns

1. Leave your local gravity-relative floor by jumping, falling or being launched.
2. Release the configured jump key.
3. Look toward the world-cardinal direction that should become the next down direction.
4. Press the jump key again while airborne.

The target is selected from the **rendered** look. Holding the same press never repeats a turn. Clinging permits one successful voluntary airborne turn; Reorientation permits more. Failed, blocked and same-direction attempts do not spend Clinging's charge.

A successful turn changes gravity immediately but preserves the current world-space velocity. Gravity changes acceleration, not momentum, so reversing gravity brakes the existing motion before accelerating the other way.

## Free-flight camera and 360-degree look

Airborne gravity changes do not force the local camera into the new gravity basis. Clinging retains the world frame that was actually being rendered, while target selection continues to use that retained view.

During sustained **Gravity Fall**, look becomes full-sphere. You can pass through both poles and complete a full 360-degree loop without the horizontal or vertical mouse direction flipping on screen. First- and third-person views use the same look direction; changing view mode changes camera placement, not the meaning of your input. When Gravity Fall ends, the same viewing direction returns to ordinary vanilla camera coordinates without a snap.

## Gravity Fall body language

After **12 airborne ticks** of Clinging/Reorientation-owned physics, a sustained fall starts Gravity Fall presentation unless another context already owns the moment.

The macro body now keeps a **persistent world-space attitude** rather than treating velocity as a frame that transports the whole body every tick. Near zero speed the last reliable attitude is retained, avoiding numerical flips during gravity reversals.

Player gaze expresses body-attitude intent. The camera can move freely inside a **35-degree neck deadzone**. Once gaze leaves that cone, the body follows by at most **7.5 degrees per tick**. Velocity contributes only a weak **1.25 degrees per tick** weathercock stabilization, using whichever head/feet orientation is already closer so stabilization cannot create an artificial 180-degree flip.

Fresh Animations/EMF keeps ownership of limbs, head tracking, equipment and micro-animation. Clinging applies only the global Gravity Fall body root.

## Posture-driven aerodynamics

Gravity Fall no longer has a special **W air-diving** steering ability. Aerodynamics is a continuous consequence of the visible body attitude.

The current velocity is decomposed relative to the body's long axis:

- longitudinal momentum is retained;
- transverse momentum receives **2.5% additional drag per tick**;
- the resulting trajectory therefore bends gradually toward the body's axis while losing energy;
- head-first and feet-first alignment are symmetric;
- no thrust, lift or speed is created by the aerodynamic layer.

This gives a direct causal chain: **look → body attitude → anisotropic drag → trajectory**. Releasing W does not switch physics off; W remains ordinary movement input where the surrounding movement system gives it meaning.

## Fast-air sound

At speed >= **0.75 blocks/tick**, Gravity Fall reuses vanilla's `ELYTRA_FLYING` sound locally. It begins silent, fades in over **10 ticks**, then follows Elytra's speed-squared volume/high-speed pitch behaviour. It stops immediately when Gravity Fall ends or real Elytra flight starts.

## Landing commitment

Clinging predicts a bounded trajectory from the real body, **current world-space velocity** and gravity using the shared `AirMotion` recurrence plus landing-surface providers. Gravity Fall aerodynamics continuously changes that real velocity before later predictions, so the forecast is refreshed from the measured state each tick instead of guessing future look or posture input. A candidate floor must be physically valid support under the active gravity, and the **first real contact** on the predicted path matters.

Acquisition and presentation are now separate:

- a future landing may be acquired and tracked up to **40 ticks / 2 seconds** ahead;
- acquisition keeps stable surface identity and brief hysteresis rather than rediscovering the floor from scratch each tick;
- the visible camera/body landing manoeuvre still lasts at most **10 ticks / 500 ms**;
- when the candidate crosses that final window, the remaining ETA sets the transition duration so the rotation converges **at touchdown**, not after impact.

The 10-tick local-player landing presentation remains separate from the shorter 180/240 ms tracked SNAP used by non-player entities.

During `LANDING_COMMITTED`, new gravity requests are discarded rather than queued. A candidate retained only by hysteresis cannot start or maintain the visible landing transition without current physical confirmation. If predicted support disappears while Clinging still owns flight, the exact current presentation becomes the held frame; if another subsystem takes ownership, Clinging releases obsolete landing state.

## Fluids and water

Intersecting **any non-empty fluid volume** suspends Clinging support, landing commitment/prediction and Gravity Fall body presentation. Modded fluids receive the same treatment as water and lava.

A seabed touched while the body is still submerged is not a Clinging floor and does not restore the one-turn budget. Recharge requires genuine gravity-relative support outside fluid context.

Water keeps its deliberate gravity-request gesture: press Space, release it, then press again within **250 ms**. The ordinary/held press remains swimming input.

While Clinging/Reorientation owns water movement:

- **W/S** move along camera forward/backward, including real camera pitch;
- **A/D** move along camera left/right;
- **Space** is world **+Y**;
- **Shift** is world **-Y**;
- remapping preserves the input magnitude produced by vanilla/Gravity Changer and does not add acceleration for free.

Logical gravity and the underwater visual frame are deliberately separate. While freely swimming without real support, the camera converges to a **world-up** presentation equivalent to ordinary DOWN gravity. When the body has genuine gravity-relative support, the camera converges to that support's up direction. Losing and regaining support uses brief hysteresis and a continuous transition; neither state rewrites the stored logical gravity.

A gravity turn requested while already in fluid keeps its retained camera frame for that fluid epoch. Entering a fluid later from dry free flight still clears an obsolete dry-flight HOLD.

## Climbables

World-vertical ladders, vines and scaffolding use an explicit gravity policy:

- **DOWN:** vanilla behaviour.
- **EAST/WEST/NORTH/SOUTH:** climbables are ignored for attachment, damping and climbing movement.
- **UP:** vanilla Y mechanics are mirrored; climbing impulse points toward world -Y and sliding/downward behaviour mirrors toward +Y.

## Flight safety and impact

While Clinging controls sustained airborne physics, the server caps world-space speed at **3.92 blocks/tick** while preserving vector direction. It also prevents movement into unavailable chunk columns and recovers from hard world/build-border breaches.

Clinging impact damage is based on world-space velocity actually absorbed by collision, not stale vanilla `fallDistance`. A last-second gravity change cannot erase a dangerous collision; genuine braking can reduce damage.

## Mace under directional gravity

Mace smash height is literal geometric distance travelled along the **current gravity direction** in the current fall segment. A gravity-direction change starts a fresh segment. Previous-axis distance is not inherited and gravity-strength scaling does not multiply geometric height.

## Elytra and First Person

Usable Elytra owns Space while airborne. It deploys normally instead of turning gravity, voluntary Clinging turns are rejected while gliding, and Gravity Fall yields immediately to real fall-flying.

First Person 2.7.2 + Not Enough Animations 1.12.4 is an explicit compatibility target. Gravity Fall uses a blended camera/body pivot for the avatar root; the macro root never feeds back into the real camera.

## Gravity Charge

A **Gravity Charge** is a captured shulker bullet. Hit a live shulker projectile with a melee attack or an arrow to materialize one Gravity Charge. An arrow fired by a dispenser counts. Capture is fenced to one item even if damage/interception paths race. Blocking with a shield does not mint a Gravity Charge, and ordinary impact, expiry or unrelated destruction does not drop one.

Gravity Charges stack to 64. Right-clicking launches one from the player's eye position in the current look direction and consumes the item. Manual use has a **0.5 second cooldown**. A dispenser launches the same projectile from the dispenser face and consumes exactly one item.

The item ID is **`clinging_reoriented:gravity_charge`**. The launched entity remains the exact vanilla `minecraft:shulker_bullet` type, preserving vanilla projectile impact, levitation, renderer and shulker-duplication semantics.

### Target acquisition

Acquisition is server-authoritative and uses launch intent captured at firing time:

- search range: about **32 blocks**;
- aim cone: about **15 degrees**;
- a Target Block hit directly by the central launch ray has absolute priority;
- otherwise living entities and assisted Target Blocks are ranked first by angular error, then by distance;
- living targets require line of sight when acquired;
- once a target is validly locked, a later better-looking target does not steal the lock;
- losing line of sight after lock does not by itself cancel a living target;
- if the target dies, is removed, changes dimension, or a Target Block is broken/replaced, the Gravity Charge reacquires from its current position while preserving original intent;
- with no valid target it continues forward using cardinal shulker-style movement and keeps retrying periodically.

Movement stays orthogonal/cardinal with discrete routing turns. It does not continuously curve like a homing rocket. A real collision with a Target Block is still a real projectile hit, so the block produces its normal redstone response.

A relaunched Gravity Charge can itself be recaptured by melee or arrow under the same one-item rule.

## Effects and brewing

Clinging comes from Alex's Mobs Continued and grants one successful voluntary airborne gravity decision before valid support restores it.

**Reorientation is brewed with a Gravity Charge, not a Shulker Shell.** Add a Gravity Charge to a Clinging potion to obtain Reorientation; long Clinging maps to long Reorientation, and redstone extends normal Reorientation. Vanilla splash and lingering routes remain available. Clinging remains available as a tier-two beacon power; Reorientation is not a beacon choice.

## Alchemical Leather equipment wear

Alchemical Leather is optional. When it is installed, Clinging: Reoriented supplies the semantics that only the gravity mod can know while Alchemical Leather keeps ownership of the equipment and durability system.

- **Clinging:** only a successful voluntary gravity turn counts as work. Failed, blocked, same-direction and grounded-mount actions do not count.
- **Reorientation:** a successful turn counts, and controlled airborne self-locomotion contributes gradual work while Reorientation actually owns that motion.
- Riding, passenger travel, moving/support-surface transport and anatomy support do not create continuous Reorientation work.
- A successful Reorientation-requested airborne mount turn still counts as the discrete turn event.
- Clinging: Reoriented never chooses the infused armor item or damages it directly. Alchemical Leather validates the actual active infusion, external-effect eclipse and configured JSON wear rule before applying ordinary durability damage.

Reorientation's humanoid slot declaration belongs to Clinging: Reoriented and remains **boots**. The integration is optional and linkage-safe, so none of these rules add an Alchemical Leather runtime requirement.

## Mounts, pets and gravity-aware mobs

Clinging itself does not grant mounted turning. With Reorientation, a fresh Space while a compatible root mount is airborne can turn the complete passenger hierarchy only when destination preflight succeeds for every member. Failure is atomic.

### Pets

Tamed animals with their own compatible effect no longer replay owner-turn breadcrumbs. Follow is **history-free** and uses the pet's current state, a filtered current owner target and current world geometry.

The pet prefers vanilla surface navigation whenever it already solves follow. When gravity is actually necessary, the local planner may:

1. choose a bounded launch frontier on the current support;
2. walk there using ordinary navigation;
3. stop and revalidate the complete support-to-support transition from the real position;
4. commit the gravity change only if the current forecast is still legal;
5. let real physics own the flight with no surface pathfinding in the air;
6. require stable support before planning the next segment.

An airborne owner remains a live objective. The pet tracks a filtered/projection-based anchor without copying the owner's gravity change or launching blindly at the owner's instantaneous 3D position. Sitting, leash/mount restrictions, foreign gravity ownership and other vanilla follow boundaries remain authoritative.

### General mob goals

The same gravity locomotion layer is available to ordinary mobs that legitimately have Clinging/Reorientation. It does **not** replace high-level AI:

- `FollowOwnerGoal` still decides to follow;
- melee goals still decide to pursue a valid target;
- avoid/flee goals still decide to escape;
- ordinary position goals still choose their destination.

Vanilla navigation always gets the first chance. Only when the normal route cannot satisfy the intent can the gravity planner evaluate local support-to-support transitions. One local plan uses one mirror path plus at most four launch nodes × five alternate gravities, so it performs at most **20 physical transition forecasts**.

Grounded planning is further budgeted per server level: at most **32 new gravity plans per tick globally** and **4 per 64×64 X/Z region per tick**. Excess work waits in `WAITING_PLAN` while preserving the goal's intent rather than pretending a partial vanilla path succeeded.

### Dynamic flight and reaction

During a committed special flight, the mob does not run surface pathfinding. A short volumetric monitor follows the current AABB, velocity and gravity and revalidates the exact committed landing independently.

Material target/world changes start a reaction timer derived from the mob's **base movement-speed attribute**, not its current falling speed. The delay is bounded to **2–10 ticks**. Small target jitter does not continually restart strategy, and changing observations do not push the reaction deadline forward forever.

- **Reorientation** may make an additional airborne correction only after the reaction delay and only if the new physical forecast is legal.
- **Clinging** with its airborne turn already spent cannot invent a second change; it must continue physics/recovery until support restores the normal capability.
- A danger that disappears before the deadline cancels the pending action.
- A block placed too late can be hit before the mob is allowed to react. That collision is intended gameplay, not a planner failure.

## Languages

The mod's player-facing strings ship in English (`en_us`) and Spanish from Spain (`es_es`). CI enforces exact key parity. The Spanish names include **«Carga de gravedad»**, **«Reorientación»**, **«Poción de reorientación»**, **«Poción arrojadiza de reorientación»**, **«Poción persistente de reorientación»** and **«Flecha de reorientación»**.

## Recovery and lifecycle

When Clinging-owned gravity must retire, the mod first attempts DOWN in place and then a deterministic validated local search within four blocks. If no safe placement exists, retirement remains pending instead of teleporting to a distant checkpoint.

Teleport, dimension transfer, death/respawn, disconnect, Elytra and foreign ownership explicitly clear or transfer transient landing/Gravity Fall state. Fluid boundaries suspend landing/Gravity Fall body semantics while preserving only the camera HOLD that was deliberately created by a turn inside the current fluid epoch. Respawn keeps visual epochs monotonic so stale packets cannot become new presentation state.

## Landing-surface API

Other mods can register a `LandingSurfaceProvider`. Providers may expose bounded support/predicted contact plus a stable revalidation key. They do not choose gravity, camera behaviour, placement or input policy. Invalid, stale, exceptional or non-finite provider results fail closed.

Vanilla collision geometry is the base provider. Scale Brews types remain outside the public Clinging API.

## Installation

Client and server need matching versions plus Minecraft 26.2, Java 25, Fabric Loader 0.19.5+, Fabric API 0.159.0+26.2+, Alex's Mobs Continued 2.1.9, CodxLib 1.5.1+, Gravity Changer Unofficial Port 1.5.2-beta.5-mc26.2 and Cloth Config API.

**0.1.0-beta.3 is the latest published prerelease.** The beta.4 behaviour documented above is currently unreleased development. Back up important worlds before testing development builds.
