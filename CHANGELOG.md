# Changelog

## [0.1.0-alpha.6] - 2026-09-09

First public alpha candidate.

### Gameplay

- Use a fresh airborne Space press to choose the nearest cardinal gravity direction
  from the rendered look.
- Limit Clinging to one successful turn per airborne stretch and add the unlimited
  Reorientation effect and potion family.
- Preserve world momentum, reject obstructed destination boxes and distinguish
  successful and failed attempts with sounds.
- Give usable Elytra priority over gravity turns.
- Add tier-two Clinging beacon support, mounted Reorientation and passive mob effect
  lifetime.
- Let eligible following pets replay bounded positional gravity trails.

### Presentation and compatibility

- Add configurable client camera duration, one second by default.
- Add optional compatibility with Alchemical Leather, Scale Brews, First Person and
  Scale Visual Compat.
- Keep Gravity Changer authoritative for movement, collision and camera transforms.

### Validation

- Pass 39 server GameTests, 10 JUnit tests and two real-client suites.
- Retain dedicated negative coverage without Scale Brews.
- Record outstanding multiplayer, long-session and full-pack visual QA explicitly.
