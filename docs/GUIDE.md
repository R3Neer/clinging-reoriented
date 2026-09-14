# Player guide

This guide describes Clinging: Reoriented **0.1.0-alpha.13**.

## Controls

1. Leave your local gravity-relative floor by jumping, falling or being launched.
2. Release the configured jump key.
3. Look toward the cardinal direction that should become the next down direction.
4. Press the jump key again while airborne.

The nearest cardinal direction to the **rendered** look is selected. Holding the same press never repeats a turn. Clinging permits one successful voluntary airborne turn; Reorientation permits more. Failed, blocked and same-direction attempts do not spend Clinging's charge.

Grounded Space remains normal jump/mount behavior. Creative flight, spectator mode, sleeping, usable Elytra/gliding, pending forced retirement and incompatible foreign ownership block voluntary gravity selection.

## What a gravity turn now feels like

A successful turn changes gravity immediately but leaves the current world velocity untouched. If you were moving east and choose UP as gravity, you still move east at that instant; only subsequent acceleration changes. Choosing opposite gravity does not reverse you by impulse. You decelerate through zero and then accelerate back.

The camera does **not** rotate just because the gravity direction changed. During free flight, the world frame you were actually seeing is retained. Reorientation can therefore change acceleration repeatedly without the camera being dragged through every logical gravity frame. You can still aim the retained camera to choose later directions because target selection uses the rendered look.

## Landing commitment

Clinging predicts a short bounded trajectory using the real entity body, velocity, physical gravity and collision geometry. A candidate floor must be able to provide real support under the feet for the active gravity. Side contacts are not silently promoted to floors.

When a valid landing is close enough, the camera begins its landing snap so completion occurs around touchdown: **180 ms** for a 90-degree frame difference and **240 ms** for an opposite 180-degree frame. A late landing starts immediately and may finish slightly after contact rather than accelerating the camera violently.

Once this begins, the landing is `LANDING_COMMITTED`. New Clinging/Reorientation gravity requests during that window are discarded and are not replayed after landing. Vanilla jump is not queued or stolen by that rule.

If the predicted support is destroyed, moved or otherwise invalidated while Clinging still owns the flight, the landing commitment is cancelled and the exact currently displayed quaternion becomes the new held frame. There is no snap-back to either the pre-landing or canonical gravity frame. Teleport, death, Elytra, water, vehicles and ownership transfer instead release the obsolete Clinging presentation so the new context can own rendering.

## Gravity Fall body language

After **12 airborne ticks** of Clinging/Reorientation-owned physics, a sustained fall starts Gravity Fall presentation unless an incompatible state or imminent landing already owns the moment. The macro body root blends for **6 ticks** toward the actual world velocity direction.

This means the body tells the trajectory, not the selected gravity. Immediately changing gravity by 90 or 180 degrees does not jerk the avatar to a new body axis. As acceleration bends the velocity, the body follows that curve. Near zero velocity it holds the last reliable frame to avoid numerical flips; once the reversed motion becomes real, the body turns with it.

Approaching support begins BODY_LANDING, which moves the body toward the future floor frame. BODY_LANDING can begin from a physically predicted floor even before the stricter camera landing commitment threshold. The camera remains independent.

With Fresh Animations Player Extension, FA/EMF keeps the limb pose, head tracking, equipment and micro-animation. Clinging applies only the global body-root transform.

## Movement controls

Alpha.13 adds no new air steering. Existing movement magnitudes remain intact. W/A/S/D are interpreted against the visual frame the player is actually seeing rather than blindly against a hidden logical gravity frame, keeping input readable while the camera is retained.

Elytra remains higher priority. Entering fall-flying cancels incompatible Gravity Fall/landing presentation and gives movement/presentation back to Elytra.

## Impact damage

While the Clinging impact lifecycle is armed, vanilla `fallDistance` is not the physical source of truth. The mod observes each world-space move, compares intended movement with movement actually permitted by collision, and derives the blocked/absorbed velocity component.

That speed is converted to a vanilla-equivalent fall distance and routed through the existing block/fall-damage pipeline. Hay, slime, water, immunities, enchantment handling and block callbacks therefore remain relevant instead of being replaced by an unrelated damage formula.

Practical consequences:

- a late gravity change cannot erase a dangerous collision that still occurs at high speed;
- reversing gravity early enough to physically brake can genuinely reduce or remove damage;
- tangential travel contributes little or nothing;
- a single multi-axis collision is resolved once;
- ownership expiry immediately before impact cannot be used to delete the dangerous motion already in progress.

## Water

Water keeps vanilla Space-to-ascend. A single press or held Space is swimming input only. To request gravity in water, press Space, release it, then press again within **250 ms**. Only the second rising edge requests the turn, and that press still reaches vanilla swimming.

Entering water while Space is already held cannot synthesize a first tap. Leaving the valid gameplay context, opening a UI, losing focus, death/respawn or disconnect clears a partial pair. Water itself never restores a spent Clinging charge; true gravity-relative feet support such as the seabed does.

Gravity Fall is not presented while swimming or in lava.

## Sprint-jump intent

Near a supported sprint landing, Space is reserved for vanilla's next jump rather than mistaken for Clinging/Reorientation. Normal effective jump power (`0.42`) keeps a one-tick horizon. Stronger `JUMP_STRENGTH` plus vanilla Jump Boost expands only this bounded prediction, capped at three ticks. Ascending, non-sprinting or unsupported players are not globally locked out.

## Effects and brewing

Clinging comes from Alex's Mobs Continued and grants one successful voluntary airborne gravity decision before real support restores it. Add a shulker shell to a Clinging potion to brew Reorientation, which removes the airborne turn limit. Redstone, gunpowder and dragon's breath retain their normal extension/splash/lingering routes. Clinging remains available as a tier-two beacon power; Reorientation is not a beacon choice.

## Elytra

Usable Elytra owns Space while airborne. It deploys normally instead of turning gravity, and voluntary turns are rejected while gliding. Elytra retains its own kinetic/fall behavior; alpha.13's collision-impact path does not duplicate Elytra damage.

## Mounts

Clinging itself does not grant mounted turning. With Reorientation, a fresh Space while the compatible root mount is airborne can turn the complete passenger hierarchy only when the destination preflight succeeds for every member. Failure is atomic.

Mount/rider heading transport and ownership loans remain as before. Non-player entities use Clinging's tracked **180/240 ms snap** rather than the local player's free-flight camera hold. Their presentation is UUID/sequence fenced and advances even off-screen.

## Pets and passive mobs

Mobs do not choose new gravity autonomously. A tamed animal using vanilla follow-owner behavior can replay bounded owner turn breadcrumbs when it reaches them and has its own compatible gravity effect. Sitting pets do not replay. Trails are bounded by count/time and lifecycle events clear them.

Pet replay derives its own physical heading transport and uses tracked owned snap presentation. Foreign gravity writes do not become Clinging-owned merely because the mob also has an effect.

## Recovery and lifecycle

When Clinging-owned gravity must retire, the mod first attempts DOWN in place and then a deterministic validated local search within four blocks. If no safe placement exists, retirement remains pending rather than teleporting to a distant checkpoint. Voluntary turns are blocked until cleanup succeeds or a lifecycle discontinuity invalidates the pending state.

Teleport, dimension transfer, death/respawn, disconnect, water/lava entry, Elytra and foreign ownership explicitly clear or transfer transient landing/Gravity-Fall presentation. Respawn keeps visual epochs monotonic across replacement player entities so stale packets cannot masquerade as new state.

## Landing-surface API

Other mods can register a `LandingSurfaceProvider` through Clinging's public API. Providers may expose bounded valid support/contact and a stable key for revalidation. They do not choose gravity, camera behavior, player placement or input policy. Invalid, stale, exceptional or non-finite provider results fail closed.

Vanilla collision geometry is the base provider. No Scale Brews classes appear in this API; a future concrete Scale adapter belongs outside the public Clinging contract.

## When a turn does nothing

Check that the entity is genuinely airborne (or that the water double-tap completed), the target differs from current gravity, the effect still exists, Clinging's one-turn charge is not spent, no landing commitment/retirement/foreign owner is blocking selection, Elytra does not own the input, and the destination hierarchy has clearance.
