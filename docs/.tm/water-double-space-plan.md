# TM implementation plan — underwater input and jump-aware landing grace

Temporary working document. Delete before merge.

## Frozen behavioral policy

- Outside water, Space behavior remains as alpha.11 except for a stronger-jump-aware sprint-landing reservation.
- In water, Vanilla owns ordinary Space-to-ascend behavior.
- A gravity turn is requested only on the second rising edge of Space after a release, within 250 ms.
- The water detector never consumes or rewrites Vanilla input.
- A detected double-tap gesture consumes its pair even if the gravity attempt is later rejected.
- Water never recharges Clinging; only real feet-side support on a solid block does.
- Sprint-jump reservation remains gravity-relative and support-predicted, but its near-landing grace scales upward with effective jump power.
- Normal jump power must preserve alpha.11's current one-tick behavior exactly.

## A. Underwater input state machine

- [x] Add a tiny pure helper for underwater double-tap detection.
- [x] Fixed window: 250 ms measured with a monotonic millisecond clock, not client ticks.
- [x] Inputs: active-context flag, jump-down flag, monotonic timestamp.
- [x] On entering context, seed prior key state from current `jumpDown` and clear any partial gesture.
- [x] First rising edge stores timestamp and returns false.
- [x] Held key returns false indefinitely.
- [x] After release, second rising edge within window returns true and clears the stored first edge.
- [x] A late second edge becomes the first edge of a new pair.
- [x] Exiting context resets detector state.
- [x] Scope partial gesture to exact `LocalPlayer` and `ClientLevel` identity.
- [x] Unit-test first tap, hold, release requirement, in-window double tap, expiry, pair consumption, context reset and enter-while-held.

## B. Water client integration

- [x] Split `ClingingClient.press()` into context-specific entry points plus one shared request sender.
- [x] Existing air/Elytra hook calls the air entry point.
- [x] Air entry point refuses `player.isInWater()` so a single underwater Space can never turn gravity through the old mixin path.
- [x] Existing client end-tick passively observes `options.keyJump.isDown()`.
- [x] Valid water context requires local player, water, active gameplay window, no screen/overlay, player alive and Clinging/Reorientation effect present.
- [x] On detected double tap call the water entry point.
- [x] Water entry point uses current rendered selection look and current pitch-independent navigation heading exactly like air.
- [x] Do not suppress Vanilla movement when request is sent or rejected.
- [x] Reset water detector on disconnect and invalid context.

## C. Server/eligibility behavior

- [x] Preserve server-authoritative `attempt(...)` and all current rejection reasons.
- [x] Do not add `isInWater` as a rejection in `GravityInput.available()`.
- [x] Keep Elytra precedence behavior unchanged outside water; `elytraWins()` already yields while in water.
- [x] Keep Clinging one-turn airborne budget unchanged.
- [x] Reorientation remains unlimited.

## D. Clinging recharge regression coverage

- [x] Add submerged-water GameTest with spent Clinging.
- [x] Water plus forged `onGround=true`, without solid feet support, does not recharge.
- [x] Solid side/body contact underwater does not recharge.
- [x] Add real solid seabed support on the feet-side face; reconcile then recharges.
- [x] Retain the all-direction landing suite to prove the rule is gravity-relative.

## E. Real-client underwater acceptance coverage

- [x] Build an underwater client fixture with Reorientation.
- [x] Single Space hold causes upward swimming/movement and sends no gravity request.
- [x] Release + second press within the window sends exactly one request and changes gravity.
- [x] Holding the second press does not repeat requests.
- [x] A new pair can perform another underwater Reorientation turn.
- [x] Failed/spent-Clinging double tap still leaves Space usable for swimming.
- [x] Existing airborne client scenarios remain unchanged.

## F. Jump-power-aware sprint-landing grace

- [ ] Add package-visible helpers in `GravityInput` for effective jump power / landing grace so policy is directly testable.
- [ ] Effective jump power:
  - [ ] use `Attributes.JUMP_STRENGTH` value when the player has that attribute;
  - [ ] otherwise use vanilla player baseline `0.42`;
  - [ ] add public `Player.getJumpBoostPower()` so ordinary Leaping contributes exactly as Minecraft defines it;
  - [ ] reject non-finite/negative derived values back to the safe baseline.
- [ ] `landingGraceTicks = clamp(effectiveJumpPower / 0.42, 1.0, 3.0)`.
- [ ] Preserve the existing normal-jump predicate exactly at `h=1`.
- [ ] Extend predicted gravity-relative travel over fractional horizon `h`:
  - [ ] `toward*h + acceleration*h*(h+1)/2`;
  - [ ] clamp minimum travel at `0.10` as today;
  - [ ] scale maximum travel from `0.60` to `0.60*h`.
- [ ] Build the complete swept AABB between the current deflated body and its predicted gravity-relative destination and reserve Space if that sweep collides. This prevents longer boosted horizons from tunneling through thin support collision shapes.
- [ ] Keep all original gates: sprinting, airborne, descending toward floor, current body collision-free, actual support predicted.
- [ ] Tests:
  - [ ] baseline player has exactly 1.0 grace tick and retains current near/distant behavior;
  - [ ] Jump Boost I/II increases grace monotonically;
  - [ ] construct a gap that baseline does not reserve but Jump Boost II does;
  - [ ] boosted ascending sprint remains unreserved;
  - [ ] boosted distant/no-support case remains unreserved;
  - [ ] sideways gravity uses the same jump-power scaling;
  - [ ] existing Gravity Changer gravity-strength scaling remains valid alongside jump-power scaling;
  - [ ] pathological boost levels are capped at 3.0 ticks.

## G. Documentation/version

- [x] Bump development version `0.1.0-alpha.11` -> `0.1.0-alpha.12`.
- [x] Update README/GUIDE/ARCHITECTURE/VALIDATION/CHANGELOG with underwater control semantics and recharge rule.
- [x] Keep configuration docs explicit: no key/timing JSON setting; 250 ms is fixed gameplay input semantics for now.
- [ ] Update docs with jump-power-aware sprint-landing grace and Leaping behavior.

## H. Iterative verification

- [x] Initial water plan reviewed twice before implementation.
- [ ] Because jump-power scaling is a new requirement, re-review this revised plan until two consecutive reviews require no policy change before touching production sprint-landing code.
- [ ] Implement remaining work on `tm/water-double-space` only.
- [ ] Review implementation diff adversarially against the revised frozen plan.
- [ ] Run build + JUnit + server GameTests.
- [ ] Run default client GameTests.
- [ ] Run First Person 2.7.2 lane.
- [ ] Run optional Scale Brews server compatibility lane.
- [ ] Run optional Scale Brews client load lane.
- [ ] Perform second adversarial review after green CI.
- [ ] Delete both temporary TM documents.
- [ ] Re-run the complete CI matrix on the exact clean HEAD.
- [ ] Merge only that exact SHA, then verify post-merge `main` CI.
