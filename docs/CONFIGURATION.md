# Configuration

Clinging: Reoriented currently exposes **no mod-owned configuration file**.

Gravity snap timing is intentionally part of the gameplay/presentation contract:

- perpendicular 90-degree turns: **0.18 seconds**;
- opposite 180-degree turns: **0.24 seconds**;
- easing: quadratic ease-out (`1 - (1-t)^2`).

Physical gravity changes immediately; the short transition affects presentation
only. The timings are fixed so multiplayer clients, tracked mounts/pets, First
Person compatibility and the turn-feel contract all use the same behavior. Active
Clinging-owned entity snaps also advance while the tracked entity is temporarily
off-screen, so looking away cannot postpone a 180/240 ms event until the next time
the mob is rendered.

Underwater input arbitration is fixed gameplay semantics too. A normal press or
hold of Space remains Vanilla swimming/ascending; Clinging/Reorientation requests a
turn only on a second Space rising edge after a release and within **250 ms** of the
first. The detector passively observes the key and never consumes or rewrites the
Vanilla input state. This window is not configurable in alpha.12.

Sprint-landing protection is also fixed. Normal effective jump power (`0.42`) keeps
one tick of near-landing reservation. Stronger effective jump power expands the
horizon proportionally as `jumpPower / 0.42`, clamped from **1 to 3 ticks**. The
value is derived from `JUMP_STRENGTH` plus Vanilla Jump Boost power rather than a
Clinging-specific setting. It protects only a predicted supported landing while
sprinting and descending; it is not a general cooldown or input delay.

Earlier alphas created `config/clinging-reoriented-client.json`. Alpha.10 and later
do not read, create, rewrite or migrate that file. An old file may be deleted; if it
remains on disk it is simply ignored.

Unrelated Gravity Changer changes, Gravity Anchor/Core transitions, commands and
other mods keep their own Gravity Changer presentation behavior. Clinging's fixed
snap policy applies only to transitions initiated or retired by
Clinging/Reorientation, including owned mount loans, pet replay and forced retirement.
