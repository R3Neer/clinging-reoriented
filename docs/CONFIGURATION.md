# Configuration

Clinging: Reoriented currently exposes **no mod-owned configuration file**.

Gravity snap timing is intentionally part of the gameplay/presentation contract:

- perpendicular 90-degree turns: **0.18 seconds**;
- opposite 180-degree turns: **0.24 seconds**;
- easing: quadratic ease-out (`1 - (1-t)^2`).

Physical gravity changes immediately; the short transition affects presentation
only. The timings are fixed so multiplayer clients, First Person compatibility and
the turn-feel contract all use the same behavior.

Earlier alphas created `config/clinging-reoriented-client.json`. Alpha.10 and later
do not read, create, rewrite or migrate that file. An old file may be deleted; if it
remains on disk it is simply ignored.

Unrelated Gravity Changer changes, Gravity Anchor/Core transitions, commands and
other mods keep their own Gravity Changer presentation behavior. Clinging's fixed
snap policy applies only to transitions initiated by Clinging/Reorientation,
including forced retirement that Clinging itself owns.
