# Configuration

Clinging: Reoriented 0.1.0-alpha.13 exposes **no mod-owned configuration file**. The values below are gameplay/presentation semantics rather than user preferences.

## Local-player camera and landing

A voluntary gravity change during free flight does **not** rotate the local camera. The currently rendered world frame is held. Camera rotation is reserved for a physically predicted landing:

- 90-degree landing: **180 ms**;
- opposite 180-degree landing: **240 ms**;
- easing: quadratic ease-out (`1 - (1-t)^2`).

If a committed landing invalidates while Clinging still owns the flight, the exact currently displayed frame is retained. Transfer to Elytra, water/lava, vehicles, teleport/lifecycle or foreign ownership releases the obsolete Clinging presentation instead.

## Gravity Fall body

- sustained Gravity Fall entry: **12 airborne ticks**;
- body-root blend into velocity tracking: **6 ticks**.

These are fixed alpha.13 tuning constants. The body follows world velocity and holds the last reliable frame near zero speed. They do not add air steering or camera following.

## Mounts and pets

Clinging-owned non-player entity transitions still use the fixed **180/240 ms** tracked SNAP presentation. This is distinct from the local player's free-flight HOLD/LAND model. Active tracked snaps advance even while the entity is off-screen.

## Water

Normal and held Space remain vanilla swimming input. A second rising edge after a real release within **250 ms** requests Clinging/Reorientation. The detector does not consume or rewrite the vanilla key state. The window is fixed.

## Sprint-jump reservation

Normal effective jump power (`0.42`) reserves one tick near a predicted supported sprint landing. Stronger effective jump power expands the bounded horizon proportionally and caps it at **3 ticks**. This is derived from `JUMP_STRENGTH` plus vanilla Jump Boost rather than a Clinging setting.

## Legacy files

Earlier alphas created `config/clinging-reoriented-client.json`. Alpha.10 and later do not read or migrate it; an old file can be deleted safely or left ignored.

Unrelated Gravity Changer transitions, anchors/cores, commands and foreign mods retain their own presentation rules.
