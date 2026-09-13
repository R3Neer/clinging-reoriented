# Changelog

## Unreleased

## [0.1.0-alpha.12] - 2026-09-13

### Underwater controls

- Keep Vanilla Space-to-ascend behavior intact in water: single presses and held
  Space never request Clinging/Reorientation.
- Add a passive **250 ms** double-Space gesture. After a real release, the second
  rising edge requests a gravity turn without consuming or rewriting Vanilla input.
- Consume each detected pair as one gesture, prevent held-key repeats, and reset
  partial pairs when the water/gameplay context is lost or entered while already
  holding Space.
- Route underwater requests through the same server-authoritative target selection,
  heading transport, charge, collision and response path used by airborne turns.

### Clinging recharge

- Keep water itself from restoring the one-turn Clinging budget.
- Lock recharge to genuine gravity-relative solid support on the player's feet-side
  face: body/side contact does not count, while actually standing on a seabed/floor
  block does.

### Sprint-jump and stronger jumps

- Scale the near-landing sprint-jump reservation with effective jump power instead
  of using a fixed one-tick prediction for every jump.
- Preserve alpha.11 exactly at normal `0.42` jump power, then widen the prediction
  proportionally through `JUMP_STRENGTH` plus Vanilla Jump Boost power, capped at
  **three ticks**.
- Predict the complete gravity-relative swept AABB over the selected horizon, while
  retaining the existing sprinting, airborne, descending and real-support gates.
- Keep ascending players and players without imminent support available to
  Clinging/Reorientation even with strong Jump Boost/Leaping-style modifiers.

### Mount and pet snap presentation

- Give Clinging-owned mount and pet gravity changes the same **180 ms quarter-turn /
  240 ms half-turn quadratic snap** used by players instead of exposing Gravity
  Changer's generic canonical interpolation.
- Add tracked-entity visual ownership with exact entity ID+UUID resolution,
  per-entity monotonic sequences, tracking-recipient delivery and explicit passenger
  delivery.
- Transport the mob's server body/head yaw through the same physical turn rather
  than applying a client-only cosmetic rotation.
- Make mounted hierarchies share one physical rotation. For opposite turns the
  rider's navigation heading chooses the common 180-degree axis; the root mount's
  own heading is rebased through that axis to derive its own yaw gauge.
- Let standalone pet replay derive the turn from the pet's own heading, and apply
  the same owned presentation to mob restoration/retirement.
- Keep same-direction writes inert and unrelated Gravity Changer changes entirely
  outside Clinging visual ownership.
- Advance active tracked-entity snaps each client tick even while the mob is
  off-screen, preventing an old 180/240 ms transition from replaying only when the
  entity later re-enters the renderer.

### Validation

- Add pure detector tests for underwater edge timing, release, expiry, pair
  consumption and context reset.
- Add a real-client water fixture proving single/held Space ascends without
  requests, double Space turns exactly once, repeated Reorientation pairs work, and
  a rejected spent-Clinging double tap still preserves Vanilla ascent.
- Add server GameTests for water/side-contact non-recharge and seabed recharge.
- Add jump-power GameTests for normal baseline, Jump Boost I/II monotonicity,
  boosted-only near-landing reservation, ascending/no-support cases, sideways
  gravity, gravity-strength interaction and the three-tick cap.
- Add geometry tests for rebasing multiple entity headings through one chosen
  physical quarter/half-turn plan.
- Add server tests proving successful pet/mounted owned turns publish exactly one
  visual epoch after preflight, same-direction/failed turns do not mutate visual
  state, mounted half-turns share the rider-selected axis, and foreign Gravity
  Changer writes remain foreign.
- Add focused real-client coverage for non-player snap enrollment, canonical
  completion even while the entity is not rendered, and the foreign-transition
  ownership boundary.

## [0.1.0-alpha.11] - 2026-09-13

### Heading and snap feel

- Separate the rendered `selectionLook` used to choose the gravity target from a
  pitch-independent `navigationHeading` used to preserve the player's forward
  direction.
- Send both intent vectors atomically in `select_intent_v3`. The server projects the
  heading onto the active gravity plane and falls back to authoritative yaw only
  when the client value is malformed or degenerate.
- Preserve world navigation heading through opposite gravity changes, including
  DOWN↔UP selected while looking straight up/down.
- Keep perpendicular turns on the unique physical 90-degree cross-product axis.
- Slow Clinging-owned quarter turns to **180 ms** and opposite half turns to
  **240 ms**, replacing cubic with quadratic ease-out so the correct axis remains
  visible without losing the snap feel.

### Respawn and falling

- Carry the visual transition sequence across player replacement on death/respawn,
  preventing valid post-respawn snap packets from being rejected as stale and
  accidentally exposing Gravity Changer's old canonical interpolation.
- Keep active animation ownership entity-instance scoped, so an in-flight snap from
  the dead player cannot migrate to the replacement entity.
- Start a fresh vanilla fall-distance segment after every successful Clinging-owned
  gravity direction change. Failed and unchanged attempts keep accumulated fall
  distance.
- Apply the same fall segmentation to mounted/root turns, pet replay and owned mob
  recovery.

### Validation

- Add all-pair geometry coverage for pitch-independent heading transport,
  opposite-heading preservation, vertical selection and malformed heading input.
- Add GameTests for player/mob/mount fall-distance segmentation and respawn visual
  epoch monotonicity.
- Update real-client timing tests so the old 120/180 ms endpoints must remain
  incomplete and the new 180/240 ms endpoints finish canonically.

## [0.1.0-alpha.10] - 2026-09-12

### Gravity snap and input

- Replace Gravity Changer's canonical-frame path for Clinging/Reorientation turns
  with minimal gravity transport and compensated visual ownership.
- Add the gravity-relative sprint-jump intent guard.
- Restore Clinging's exact one-voluntary-turn airborne budget, including DOWN.
- Remove the Clinging-owned camera timing JSON entirely.
- Retain alpha.9's center-aligned collision-clearance fallback.
- Alpha.11 supersedes alpha.10's 120/180 ms cubic timing and its degenerate vertical
  opposite-turn fallback while retaining the minimal-transport architecture.

## [0.1.0-alpha.8] - 2026-09-11

Documentation-sync prerelease with no gameplay/production-code changes relative to
alpha.7. The full server, default-client and First Person lanes were repeated before
publication.

## [0.1.0-alpha.7] - 2026-09-11

Correctness and ownership hardening:

- bound forced retirement to a validated local four-block search;
- preserve later/existing external gravity through explicit ownership tracking;
- make root/passenger recovery transactional;
- restore mount prior-frame loans correctly;
- harden moving-surface references against stale/replayed samples;
- isolate optional First Person and Scale integrations from required production
  dependencies.

## [0.1.0-alpha.6] - 2026-09-09

First public alpha candidate. It established airborne Space selection, one-turn
Clinging, unlimited Reorientation, momentum/obstruction rules, Elytra priority,
beacon support, mounted turns and pet route replay. Later alphas replace its original
camera-duration/configuration model.
