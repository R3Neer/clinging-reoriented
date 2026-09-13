# TM implementation plan — underwater double-Space input

Temporary working document. Delete before merge.

## Frozen behavioral policy

- Outside water, Space behavior remains exactly as alpha.11.
- In water, Vanilla owns ordinary Space-to-ascend behavior.
- A gravity turn is requested only on the second rising edge of Space after a release, within 250 ms.
- The detector never consumes or rewrites Vanilla input.
- A detected double-tap gesture consumes its pair even if the gravity attempt is later rejected.
- Water never recharges Clinging; only real feet-side support on a solid block does.

## A. Input state machine

- [ ] Add a tiny pure helper for underwater double-tap detection.
- [ ] Fixed window: 250 ms measured with a monotonic millisecond clock, not client ticks.
- [ ] Inputs: active-context flag, jump-down flag, monotonic timestamp.
- [ ] On entering context, seed prior key state from current `jumpDown` and clear any partial gesture.
- [ ] First rising edge stores timestamp and returns false.
- [ ] Held key returns false indefinitely.
- [ ] After release, second rising edge within window returns true and clears the stored first edge.
- [ ] A late second edge becomes the first edge of a new pair.
- [ ] Exiting context resets detector state.
- [ ] Unit-test first tap, hold, release requirement, in-window double tap, expiry, pair consumption, context reset and enter-while-held.

## B. Client integration

- [ ] Split `ClingingClient.press()` into context-specific entry points plus one shared request sender.
- [ ] Existing air/Elytra hook calls the air entry point.
- [ ] Air entry point refuses `player.isInWater()` so a single underwater Space can never turn gravity through the old mixin path.
- [ ] Existing client end-tick passively observes `options.keyJump.isDown()`.
- [ ] Valid water context requires local player, water, active gameplay window, no screen/overlay, player alive and Clinging/Reorientation effect present.
- [ ] On detected double tap call the water entry point.
- [ ] Water entry point uses current rendered selection look and current pitch-independent navigation heading exactly like air.
- [ ] Do not suppress Vanilla movement when request is sent or rejected.
- [ ] Reset water detector on disconnect and invalid context.

## C. Server/eligibility behavior

- [ ] Preserve server-authoritative `attempt(...)` and all current rejection reasons.
- [ ] Do not add `isInWater` as a rejection in `GravityInput.available()`.
- [ ] Keep Elytra precedence behavior unchanged outside water; `elytraWins()` already yields while in water.
- [ ] Keep Clinging one-turn airborne budget unchanged.
- [ ] Reorientation remains unlimited.

## D. Clinging recharge regression coverage

- [ ] Add submerged-water GameTest with spent Clinging.
- [ ] Water plus forged `onGround=true`, without solid feet support, does not recharge.
- [ ] Solid side/body contact underwater does not recharge.
- [ ] Add real solid seabed support on the feet-side face; reconcile then recharges.
- [ ] Add/retain a non-DOWN gravity support case to prove the rule is gravity-relative.

## E. Real-client acceptance coverage

- [ ] Build an underwater client fixture with Reorientation.
- [ ] Single Space hold causes upward swimming/movement and sends no gravity request.
- [ ] Release + second press within the window sends exactly one request and changes gravity.
- [ ] Holding the second press does not repeat requests.
- [ ] A new pair can perform another underwater Reorientation turn.
- [ ] Failed/spent-Clinging double tap still leaves Space usable for swimming.
- [ ] Existing airborne client scenarios remain unchanged.

## F. Documentation/version

- [ ] Bump development version `0.1.0-alpha.11` -> `0.1.0-alpha.12`.
- [ ] Update README/GUIDE/ARCHITECTURE/VALIDATION/CHANGELOG with underwater control semantics and recharge rule.
- [ ] Keep configuration docs explicit: no key/timing JSON setting; 250 ms is fixed gameplay input semantics for now.

## G. Iterative verification

- [ ] Review this plan against current code before implementation; revise until two consecutive reviews require no policy change.
- [ ] Implement on `tm/water-double-space` only.
- [ ] Review implementation diff adversarially against the frozen plan.
- [ ] Run build + JUnit + server GameTests.
- [ ] Run default client GameTests.
- [ ] Run First Person 2.7.2 lane.
- [ ] Run optional Scale Brews server compatibility lane.
- [ ] Run optional Scale Brews client load lane.
- [ ] Perform second adversarial review after green CI.
- [ ] Delete both temporary TM documents.
- [ ] Re-run the complete CI matrix on the exact clean HEAD.
- [ ] Merge only that exact SHA, then verify post-merge `main` CI.
