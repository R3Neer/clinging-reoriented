# Architecture

Clinging: Reoriented 0.1.0-alpha.15 separates **physical gravity**, **camera ownership**, **body presentation**, **landing authority**, **aerodynamic steering**, **impact damage**, **pet route ownership** and **interaction context** instead of treating a gravity-direction write as one monolithic event.

## Authority and design rule

The server owns physical gravity, collision, effect/charge state, landing commitment, aerodynamic velocity changes, flight safety, damage and pet breadcrumb replay. Voluntary gravity changes preserve world-space velocity; air-diving is a bounded server-authoritative redirection of existing momentum rather than a thrust system.

Gravity Changer remains the coordinate/gravity authority. Clinging owns only state created by Clinging/Reorientation and releases cleanly when another source legitimately takes over.

## Player input, camera and Gravity Fall

`GravityInput` produces edge-triggered gravity requests using rendered `selectionLook` plus pitch-independent `navigationHeading`. Water retains the passive Space-release-Space gesture; owned water movement uses world +Y/-Y. Climbables use DOWN vanilla, lateral ignore and mirrored UP policy.

Sustained Gravity Fall begins after 12 airborne ticks. `GravityFallVisuals` reconstructs a velocity-owned body frame client-side. Look-follow has a 35-degree deadzone and 7.5-degree/tick cap; posture adds at most 1.3% extra broadside drag. `gravity_fall_look_v1` carries bounded world gaze/forward intent and the server may redirect existing momentum by at most 6 degrees/tick when W is held and look/velocity have positive alignment.

`GravityFallLookMixin` owns full-sphere pitch only while the local player has active Gravity Fall presentation. Exit canonicalizes to an equivalent vanilla yaw/pitch pair without changing gaze. First Person uses a 0.72 blend from body center toward the true local camera pivot so the macro root avoids clipping without hiding the body or rotating the real camera.

## Landing, fluids and safety

`LandingSurfaceProvider` / `LandingSurfaces` provide bounded support/contact facts. `FluidContext` is a shared fence: intersecting any non-empty fluid prevents support/landing/Gravity Fall semantics, including over a solid seabed.

Camera LAND and BODY_LANDING share `LandingTiming.PRESENTATION_TICKS = 10` / 500 ms. Tracked non-player SNAP retains the shorter turn-kind timings.

`FlightSafety` caps eligible airborne world speed to 3.92 blocks/tick, holds at unavailable chunk frontiers and recovers from hard world/build-border breaches. Safety interventions clear `ImpactState` so rescue is not itself a collision.

## Impact and directional mace

Impact damage derives from world-space velocity actually absorbed by collision and maps it back through vanilla-compatible fall handling. `DirectionalMaceFall` separately tracks literal positional travel projected onto the current gravity direction. Gravity changes start a fresh mace segment; gravity-strength scaling does not scale mace geometry.

## Mounts and pet breadcrumb routing

Mounted gravity remains transactional over a root/passenger hierarchy. Non-player visual gravity remains tracked SNAP, fenced by entity identity and monotonic sequence.

`GravityBreadcrumbs` stores a bounded per-owner route of gravity-change `Step`s with sequence, dimension, position, direction and time. `LIMIT = 64`; `TTL = 1200` ticks. Expired/wrong-dimension steps are skipped and owner lifecycle pruning removes obsolete trails.

A tame pet may pursue the oldest valid step only when it has a compatible effect, remains in the same live server level as its owner, is not unable to move to the owner, and `MobGravity.canReplayBreadcrumb` permits Clinging ownership. With no eligible step, vanilla owner following is untouched.

### Movement-plane projection

Ordinary pathfinding cannot sensibly walk to an owner breadcrumb floating above a different gravity plane. `GravityBreadcrumbs.projectedTarget` therefore freezes the coordinate along the pet's **current gravity axis** and copies the other two coordinates from the breadcrumb:

- X gravity -> move on YZ;
- Y gravity -> move on XZ;
- Z gravity -> move on XY.

`FollowOwnerGoal` is allowed to start for that pending route even inside vanilla's usual owner-distance dead zone. The goal targets the projected point and retries path authoring every 10 ticks. Arrival uses `max(pet.bbWidth, stopDistance)` clamped to 0.5..2 blocks, matching the goal's practical stopping semantics closely enough that a completed vanilla path cannot stall just outside a narrower trigger.

### Replay and ballistic handoff

On arrival, navigation stops and `MobGravity.replay` applies the recorded gravity direction. If rotation at the current feet position is collision-clear, it turns in place. Otherwise it may try exactly one center-aligned placement produced by Gravity Changer geometry, subject to the same tree/passenger collision preflight as other owned turns.

That internal center-aligned placement sets `breadcrumbRelocating`, so `MovingSurface.teleported` does **not** interpret it as an external discontinuity and does not erase the route. An actual external teleport calls `GravityBreadcrumbs.forget`, advances the pet past existing steps, releases route ownership and stops any Clinging-authored path.

After successful replay the pet clears only the consumed route marker. If another step remains queued, it is preserved. While unsupported, `GravityBreadcrumbs.canPursue` is false, `FollowOwnerGoal` releases control and directional gravity physics owns the fall. Once support is regained in the new frame, the next pending step can reactivate pursuit.

### Navigation replacement and vanilla coexistence

Gravity Changer replaces the mob's `PathNavigation` after a gravity change. Vanilla `FollowOwnerGoal` caches navigation, so `FollowGravityMixin` marks that field mutable and refreshes it from `tamable.getNavigation()` in `canUse`, `canContinueToUse`, `start`, `stop` and `tick`. This prevents the goal from driving a stale pre-turn navigator.

The mixin overrides vanilla follow admission/continuation only while a valid gravity breadcrumb is pending. If the pet has no compatible effect or no pending route, vanilla owner-follow rules remain unchanged. Sitting pauses and stops the authored path without consuming the pending step; standing can reacquire it. Foreign gravity ownership, passengers/vehicles, dead entities and invalid owner contexts fail closed.

Clinging's one-air-turn rule is still respected by pet replay through `airUsed`; a pet with Reorientation can replay further airborne turns.

## Optional integrations and validation

Production code remains Scale-free. First Person, Fresh Animations/FA Player Extension/EMF/ETF and Scale Brews stay optional pinned CI fixtures. Pet routing depends only on vanilla tame/follow goals plus Gravity Changer's public gravity/navigation behaviour; it does not add per-pet-mod adapters.

The alpha.15 pet suite covers all three movement planes/six directions, the vanilla follow dead zone, real scheduled wolf walking to an airborne breadcrumb, bounded arrival, center-aligned replay, ballistic release, navigation replacement, queue preservation, sitting pause/resume, effect-free behaviour and external teleport invalidation.
