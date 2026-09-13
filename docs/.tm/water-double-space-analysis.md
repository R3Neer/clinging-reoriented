# TM analysis — underwater double-Space input

Temporary working document. Delete before merge.

## User-facing goal

Reorientation/Clinging should remain usable in water without stealing Vanilla's normal Space-to-ascend control. In water, one press/hold of Space must remain ordinary swimming; a deliberate double press may request a gravity turn.

## Existing input architecture

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
6. Leaving water, losing the player/context, opening UI/overlay, losing focus, death/respawn or disconnect invalidates a partial water double tap.
7. Entering water while Space is already held must not synthesize a first press.
8. The detector is client presentation/input state only; server authority and all normal rejection reasons remain unchanged.

## Detector design

Use passive polling of `Minecraft.options.keyJump.isDown()` from the existing client tick. Never call `consumeClick()` and never rewrite the key state.

Maintain a small detector state:

- whether the detector currently owns an active water context;
- previous sampled `jumpDown` value;
- timestamp of the first water rising edge, or none.

On context entry, initialize `previousDown` to the current physical key state and clear the timestamp. On every valid water tick:

- rising edge = `jumpDown && !previousDown`;
- first rising edge stores `now` and does nothing to Clinging;
- next rising edge within 250 ms clears the stored edge and returns `doubleTap=true`;
- if the window expired, the new edge replaces the old first edge;
- no edge while held.

Use monotonic milliseconds (`Util.getMillis()` or equivalent) in production and expose the state machine as a pure helper accepting `nowMs` for deterministic tests.

## Interaction with the existing air path

`JumpInputMixin` must not accidentally turn on a single underwater press if Vanilla happens to traverse the fall-flying call site. Split intent entry points:

- air path: refuse water and preserve current airborne behavior;
- water-double path: only invoked by the detector after the second water rising edge.

Both routes should converge on the same request construction/sending code so selection look, navigation heading, sequence/revision handling and result sounds remain identical.

## Clinging recharge semantics

No new recharge rule is required. Existing `AirChanges.grounded()` requires `onGround()` plus geometric support on the feet-side face for the active gravity. Water has no block collision support and therefore cannot recharge Clinging by itself.

Add explicit regressions proving:

- merely being submerged with `onGround` forged does not recharge;
- touching a solid block with the side/body but not the feet-side face does not recharge;
- standing on a real solid seabed block while submerged does recharge;
- the same support-face semantics remain gravity-relative rather than world-DOWN-specific.

## Versioning

Alpha.11 is already published. This behavior change therefore targets development version `0.1.0-alpha.12`.

## Acceptance invariants

- Single Space press/hold underwater preserves Vanilla ascent and sends no gravity request.
- Double Space press underwater sends exactly one gravity request.
- Reorientation can perform repeated underwater turns via separate double-tap gestures.
- Spent Clinging remains spent throughout free swimming and body/side contact.
- Spent Clinging recharges only after true feet-side support on a solid block.
- Failed gravity requests never interfere with swimming input.
- Existing air, sprint-jump, Elytra, First Person, Scale Brews and snap behavior remain unchanged.
