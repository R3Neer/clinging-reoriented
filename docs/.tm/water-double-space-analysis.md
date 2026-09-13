# TM analysis — underwater input, jump-aware landing grace and mob snaps

Temporary working document. Delete before merge.

## User-facing goals

1. Reorientation/Clinging should remain usable in water without stealing Vanilla's normal Space-to-ascend control. In water, one press/hold of Space must remain ordinary swimming; a deliberate double press may request a gravity turn.
2. Sprint-jump protection near landing must scale with jump power. A Leaping/Jump Boost player should not accidentally reorient simply because a stronger jump makes the fixed alpha.11 landing-grace prediction too short.
3. Mounts and pets whose gravity is changed by Clinging/Reorientation must use the same short minimal snap presentation as the player instead of Gravity Changer's generic canonical interpolation.

## Underwater input

The existing airborne `JumpInputMixin` is intentionally tied to the fresh-Space/Elytra path, not general key polling. Underwater input therefore passively observes `keyJump.isDown()` and detects rising edges without consuming or rewriting Vanilla input.

Required semantics:

- first Space press/hold in water remains pure Vanilla ascent;
- after a real release, a second rising edge within 250 ms requests a turn;
- holding never repeats;
- the detected pair is consumed even if server authority rejects the request;
- entering water while already holding Space cannot synthesize a first tap;
- leaving water, UI/focus loss, death/respawn, player replacement, level replacement or disconnect invalidates a partial pair;
- client selection/heading capture and server authority remain the same as air.

The implemented detector is a pure helper using a monotonic millisecond clock, with client integration scoped to the exact `LocalPlayer` and `ClientLevel` identity.

## Clinging recharge semantics

No new recharge rule is required. Existing `AirChanges.grounded()` requires `onGround()` plus geometric support on the feet-side face for the active gravity. Water has no block collision support and therefore cannot recharge Clinging by itself.

Explicit regressions must prove:

- merely being submerged with `onGround` forged does not recharge;
- touching a solid block with the side/body but not the feet-side face does not recharge;
- standing on a real solid seabed block while submerged does recharge;
- the same support-face semantics remain gravity-relative rather than world-DOWN-specific.

## Jump-power-aware sprint-landing grace

Alpha.11's `sprintLandingJumpReserved` reserves a fresh Space only when the player is sprinting, descending toward the active gravity floor and a one-step prediction reaches support. The prediction uses current gravity-relative downward speed plus Gravity Changer acceleration and clamps travel to 0.60 blocks.

That baseline is correct for normal jumping but feels too short under Leaping. Stronger jump power changes the cadence and range over which players queue the next sprint jump, while the fixed one-tick grace remains unchanged.

Policy:

- normal effective jump power `0.42` => grace horizon exactly `1.0` tick, preserving alpha.11;
- stronger jump power => `graceTicks = effectiveJumpPower / 0.42`;
- cap at `3.0` ticks;
- weaker/custom jump power never reduces the existing one-tick protection;
- effective power uses `Attributes.JUMP_STRENGTH` when present plus `Player.getJumpBoostPower()`, so Jump Boost and compatible jump-strength modifiers participate without potion-name special cases;
- predict gravity-relative travel over horizon `h` as `toward*h + acceleration*h*(h+1)/2`, clamp max travel to `0.60*h`, and collision-test the complete swept AABB from current to predicted body;
- sprinting, airborne, descending and actual predicted support remain mandatory, so stronger jump power never creates a blanket lockout.

## Why mounts/pets still use the old Gravity Changer turn

The snap mixin itself is generic: `GravitySnapMixin` can override the `GravityRotationAnimation` of any entity. The missing piece is ownership/networking.

`VisualTransitions.begin(...)` is currently activated only by the player-only `visual_transition_v2` payload. `MountedGravity` sends that payload to the rider after `MobGravity.borrow(...)`, so the rider's camera/body gets the alpha.11 snap but the root mount's own `GravityRotationAnimation` is never enrolled. Pet `MobGravity.replay(...)` changes the pet's gravity without sending any visual transition at all. Gravity Changer therefore owns those entity animations and uses its generic long canonical-frame path.

The fix should not duplicate camera geometry. It should generalize visual ownership to non-player entities:

- add an entity-scoped visual-transition payload carrying entity id + UUID, target, yaw delta, turn kind and per-mob sequence;
- send it to players tracking the mob plus any ServerPlayer passengers before the physical gravity commit;
- client resolves the exact entity and starts `VisualTransitions` ownership on that entity's existing Gravity Changer animation;
- keep local-player visual sequence handling connection-scoped for the respawn invariant; entity sequences are tracked independently per entity UUID and reset on disconnect;
- apply the same yaw-gauge transform to the mob on server and client so its world heading/body pose is transported rather than merely cosmetically animated.

### Common physical rotation for mounted hierarchies

For a mounted Reorientation turn the rider already defines the intended physical transition. In 90-degree turns the axis is unique. In 180-degree turns it is the rider's navigation heading.

The mount must not independently choose a different 180-degree axis from its own heading, or rider and mount can visibly rotate around different axes while attached. Instead:

1. build the rider's normal transition plan;
2. use that plan's axis/kind as the one physical rotation for the mounted gravity change;
3. rebase the root mount's own navigation heading through that same physical rotation to calculate the mount-specific `yawDelta`;
4. send the mount's entity visual payload and apply that delta before committing root gravity;
5. rider and mount therefore share one physical snap while each preserves its own local/world facing semantics.

A pet replay has no rider-defined physical transition, so the pet's own gravity-relative heading selects the 180-degree axis and the ordinary `GravityTransition.plan(...)` is sufficient.

Forced/owned mob retirement and rider-loan restoration should use the same entity snap because they are still Clinging-owned gravity changes. Foreign Gravity Changer writes remain untouched.

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
- Sprinting while ascending or without predicted support remains available regardless of jump power.
- Mount roots and pets use the same 180/240 ms quadratic minimal snap instead of Gravity Changer's generic interpolation.
- Rider and root mount use the same physical rotation axis for one mounted turn.
- Pet standalone/replay turns preserve the pet's own heading using the same geometry policy.
- Foreign Gravity Changer transitions stay upstream.
- Existing player heading, respawn, fall, First Person and Scale Brews behavior remains unchanged.
