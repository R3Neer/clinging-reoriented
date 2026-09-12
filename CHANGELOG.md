# Changelog

## Unreleased

### Gravity snap and heading

- Replace Clinging/Reorientation's duration-only patch of Gravity Changer's
  canonical-frame SLERP with a Clinging-owned minimal gravity transport.
- Use the single 90-degree cross-product axis for perpendicular gravity changes and
  the current horizontal heading as the 180-degree axis for opposite changes,
  falling back to the current right axis when heading is degenerate.
- Transport the logical world look through the same rotation, preserve pitch and
  commit only the required wrapped yaw delta under the canonical target frame.
- Present quarter turns as fixed **120 ms** snaps and opposite turns as fixed
  **180 ms** snaps using cubic ease-out; unrelated Gravity Changer animations keep
  upstream behavior.
- Start an interrupted Reorientation transition from the frame currently displayed
  instead of queueing or snapping back to an intermediate canonical frame.

### Input intent

- Reserve airborne Space for an imminent sprint landing when the player is sprinting
  toward support predicted to be reached on the next gravity-relative simulation
  step, preventing ordinary sprint-jump chains from becoming accidental turns.
- Apply the same gravity-direction-agnostic reservation in client precheck and
  server authority, including Gravity Changer gravity-strength scaling.
- Restore Clinging's exact one-voluntary-turn airborne budget: after the charge is
  spent, DOWN is rejected just like every other voluntary target. Reorientation
  remains unlimited and forced retirement to DOWN remains independent of charge.

### Configuration and presentation ownership

- Remove the Clinging-owned camera timing JSON setting entirely. Alpha.10 no longer
  reads or creates `config/clinging-reoriented-client.json`; legacy files are inert.
- Keep camera, third-person model and First Person on the same Clinging-owned visual
  gravity quaternion instead of implementing separate orientation trajectories.

### Gameplay and collision clearance

- Retain alpha.9's center-aligned fallback when the old feet pivot alone makes a
  rotated player box clip the floor or wall being left behind.
- Preserve the physical body's world-space center for that fallback and keep genuine
  obstruction rejection atomic.

### Compatibility and build isolation

- Keep Scale Brews out of production compileClasspath and resolve transitional
  legacy Scale hooks reflectively behind optional fail-closed integration.
- Keep Scale beta.5 and First Person 2.7.2 coverage in isolated CI lanes rather than
  production dependencies.

### Validation

- Add JUnit coverage for every cardinal gravity pair, transported heading, canonical
  endpoint identity, fixed snap timing and opposite-direction degeneracies.
- Add server regressions for gravity-relative sprint-jump reservation, altered
  gravity strength and the restored one-turn Clinging budget including DOWN.
- Update real-client and First Person fixtures for visual-transition protocol v2.

## [0.1.0-alpha.8] - 2026-09-11

Documentation-sync prerelease. There are no gameplay or production-code behavior
changes relative to alpha.7.

### Documentation and packaging

- Replace pre-publication candidate wording with the actual released state.
- Update current-version references while preserving alpha.7 as historical hardening
  evidence.
- Repeat the full server, default-client and First Person CI lanes before publication.

## [0.1.0-alpha.7] - 2026-09-11

Correctness and ownership hardening without adding a new gameplay feature set.

### Gravity and recovery

- Bound forced player retirement to DOWN in place or a validated local relocation
  within four blocks; remove remote `lastSafeDown` and full-column recovery.
- Keep an obstructed retirement pending and retry periodically without allowing new
  voluntary turns.
- Track mob gravity ownership explicitly so Clinging retires only frames it created
  or borrowed and preserves later/existing external gravity.
- Restore the mount's previous frame when a Reorientation loan ends.
- Make root/passenger turns and recovery transactional across the complete passenger
  hierarchy.

### Presentation and networking

- Scope the then-configurable camera timing to transitions initiated by Clinging or
  Reorientation instead of globally patching Gravity Changer.
- Give First Person the same Clinging-owned visual boundary.
- Add bounded, one-shot latest-interval validation to moving-surface references.

### Compatibility

- Treat mounted gravity generically for compatible living root vehicles.
- Remove Tiny-Mount-specific movement shims from Clinging.
- Keep Scale Brews outside the required runtime/support target.

### Validation

- Expand required server/JUnit/client coverage and add adversarial holdouts for
  retirement, ownership, passenger recovery and stale support references.

## [0.1.0-alpha.6] - 2026-09-09

First public alpha candidate.

### Gameplay

- Use a fresh airborne Space press to choose the nearest cardinal gravity direction
  from the rendered look.
- Limit Clinging to one successful turn per airborne stretch and add unlimited
  Reorientation.
- Preserve world momentum, reject obstructed destination boxes, give usable Elytra
  priority and add beacon, mount and pet-route behavior.

### Presentation and compatibility

- Introduce the original client camera-duration option and optional compatibility
  with Alchemical Leather, Scale Brews and First Person. Later alphas replace this
  presentation model.

### Validation

- Pass the initial required server, JUnit and client suites and record outstanding
  multiplayer/full-pack QA explicitly.
