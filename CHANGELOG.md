# Changelog

## Unreleased

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

### Validation

- Add pure detector tests for edge timing, release, expiry, pair consumption and
  context reset.
- Add a real-client water fixture proving single/held Space ascends without requests,
  double Space turns exactly once, repeated Reorientation pairs work, and a rejected
  spent-Clinging double tap still preserves Vanilla ascent.
- Add server GameTests for water/side-contact non-recharge and seabed recharge.

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
