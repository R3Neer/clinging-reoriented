# TM analysis — underwater input and jump-aware landing grace

Temporary working document. Delete before merge.

## User-facing goals

1. Reorientation/Clinging should remain usable in water without stealing Vanilla's normal Space-to-ascend control. In water, one press/hold of Space must remain ordinary swimming; a deliberate double press may request a gravity turn.
2. Sprint-jump protection near landing must scale with jump power. A Leaping/Jump Boost player should not accidentally reorient simply because a stronger jump makes the fixed alpha.11 landing-grace prediction too short.

## Existing water input architecture

- Outside water, `JumpInputMixin` wraps `LocalPlayer.tryToStartFallFlying()` and calls `ClingingClient.press()` only when Vanilla did not start gliding.
- This is intentionally tied to the airborne fresh-Space path and is not a general key-edge detector.
- `ClingingClient.press()` currently sends one gravity-selection request after local eligibility checks. It does not consume the Vanilla key state.
- `GravityInput.available()` does not reject water by itself; Elytra priority explicitly yields while in water, so a server-authoritative turn can already succeed underwater if a request reaches it.

## Required underwater input semantics

1. First Space press in water is never delayed, cancelled or consumed by Clinging. Vanilla sees it normally and can ascend.
2. Holding Space never counts as repeated taps and never spams gravity requests.
3. A second rising edge after a real release, within a fixed 250 ms window, requests Clinging/Reorientation.
4. The second press is still visible to Vanilla, so ascent continues even when the gravity attempt succeeds or fails.
5. The two presses are consumed as one gesture: after a detected double tap, a third rapid press starts a new pair rather than immediately triggering again.
6. Leaving water, losing the player/context, opening UI/overlay, losing focus, death/respawn, dimension/world replacement or disconnect invalidates a partial water double tap.
7. Entering water while Space is already held must not synthesize a first press.
8. The detector is client presentation/input state only; server authority and all normal rejection reasons remain unchanged.

## Water detector design

Use passive polling of `Minecraft.options.keyJump.isDown()` from the existing client tick. Never call `consumeClick()` and never rewrite the key state.

Maintain a small detector state:

- whether the detector currently owns an active water context;
- previous sampled `jumpDown` value;
- timestamp of the first water rising edge, or none;
- current `LocalPlayer` and `ClientLevel` identity so respawn/dimension replacement cannot inherit a partial gesture.

On context entry, initialize `previousDown` to the current physical key state and clear the timestamp. On every valid water tick:

- rising edge = `jumpDown && !previousDown`;
- first rising edge stores `now` and does nothing to Clinging;
- next rising edge within 250 ms clears the stored edge and returns `doubleTap=true`;
- if the window expired, the new edge replaces the old first edge;
- no edge while held.

Use a monotonic millisecond clock and expose the state machine as a pure helper accepting `nowMs` for deterministic tests.

## Interaction with the existing air path

`JumpInputMixin` must not accidentally turn on a single underwater press if Vanilla happens to traverse the fall-flying call site. Split intent entry points:

- air path: refuse water and preserve current airborne behavior;
- water-double path: only invoked by the detector after the second water rising edge.

Both routes converge on the same request construction/sending code so selection look, navigation heading, sequence/revision handling and result sounds remain identical.

## Clinging recharge semantics

No new recharge rule is required. Existing `AirChanges.grounded()` requires `onGround()` plus geometric support on the feet-side face for the active gravity. Water has no block collision support and therefore cannot recharge Clinging by itself.

Explicit regressions must prove:

- merely being submerged with `onGround` forged does not recharge;
- touching a solid block with the side/body but not the feet-side face does not recharge;
- standing on a real solid seabed block while submerged does recharge;
- the same support-face semantics remain gravity-relative rather than world-DOWN-specific.

## Jump-power-aware sprint-landing grace

Alpha.11's `sprintLandingJumpReserved` reserves a fresh Space only when the player is sprinting, descending toward the active gravity floor and the next predicted movement intersects support. The prediction currently uses:

- `toward = velocity dot gravityUnit`;
- one-tick gravity acceleration from Gravity Changer;
- travel clamped to `[0.10, 0.60]` blocks.

That exactly protects the normal-jump case, but the grace horizon is fixed. Minecraft's effective jump power is based on the normal 0.42 player jump strength plus Jump Boost's public jump-power contribution; stronger jumps produce a larger/faster landing arc. A fixed one-tick/0.60-block reservation can therefore stop matching the player's sprint-jump rhythm under Leaping.

The policy should scale only upward from alpha.11's baseline:

- normal effective jump power `0.42` => grace horizon exactly `1.0` tick, preserving current behavior;
- stronger jump power => `graceTicks = effectiveJumpPower / 0.42`;
- cap the horizon at `3.0` ticks so pathological/custom effect levels cannot reserve Space from many blocks away;
- weaker/custom jump power never reduces the existing one-tick protection.

Use the player's `Attributes.JUMP_STRENGTH` value when that attribute is present, otherwise the vanilla player baseline `0.42`, then add `player.getJumpBoostPower()`. This captures ordinary Leaping and compatible jump-strength modifiers without keying policy to one potion name.

Predict gravity-relative travel over the fractional grace horizon using the same current toward velocity and Gravity Changer acceleration. Preserve alpha.11 exactly at horizon 1:

`predictedTravel = toward*h + acceleration*h*(h+1)/2`

and cap travel to `0.60*h`. At `h=1` this reduces to the existing `toward + acceleration`, clamped to 0.60. Probe collision along the downward sweep rather than only at the endpoint so the longer boosted horizon cannot tunnel through a support block.

The predicate remains intentionally narrow: it still requires sprinting, descending, and actual predicted support. Jump power never creates a blanket period where gravity turns are disabled.

## Versioning

Alpha.11 is already published. These behavior changes target development version `0.1.0-alpha.12`.

## Acceptance invariants

- Single Space press/hold underwater preserves Vanilla ascent and sends no gravity request.
- Double Space press underwater sends exactly one gravity request.
- Reorientation can perform repeated underwater turns via separate double-tap gestures.
- Spent Clinging remains spent throughout free swimming and body/side contact.
- Spent Clinging recharges only after true feet-side support on a solid block.
- Failed gravity requests never interfere with swimming input.
- Normal jump power retains alpha.11 sprint-landing reservation behavior.
- Jump Boost/Leaping increases only the near-landing reservation horizon, proportionally and with a hard cap.
- Sprinting while ascending or without predicted support remains available to Clinging/Reorientation regardless of jump power.
- Existing air, Elytra, First Person, Scale Brews and snap behavior remain unchanged.
