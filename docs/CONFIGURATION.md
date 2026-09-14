# Configuration

Clinging: Reoriented 0.1.0-alpha.16 exposes **no mod-owned configuration file**. The values below are fixed gameplay/presentation semantics rather than user preferences.

## Local-player camera and landing

A voluntary gravity change during free flight does **not** rotate the local camera. The currently rendered world frame is held. During a physically predicted landing, local camera LAND and Gravity Fall BODY_LANDING share a **10-tick / 500 ms** presentation window.

If committed support invalidates while Clinging still owns the flight, the exact currently displayed frame is retained. Transfer to Elytra, any intersecting non-empty fluid, vehicles, teleport/lifecycle or foreign ownership releases obsolete Clinging presentation instead.

Ordinary tracked non-player SNAP remains separate: quarter turns use the existing **180 ms** timing and opposite half turns **240 ms**.

## Gravity Fall body and air

- sustained Gravity Fall entry: **12 airborne ticks**;
- body-root entry blend: **6 ticks**;
- body look deadzone: **35 degrees**;
- maximum macro-body look follow: **7.5 degrees/tick**;
- maximum extra broadside posture drag: **1.3%/tick**;
- W air-diving redirect: at most **6 degrees/tick**, with no authority for perpendicular/backward gaze and no added thrust;
- fast-air sound admission: **0.75 blocks/tick**, with a **10-tick** fade-in;
- Clinging-controlled airborne world-speed cap: **3.92 blocks/tick**.

These values are fixed semantics. They do not turn Gravity Fall into creative flight or Elytra.

## Shulker Charge

Shulker Charge also uses fixed rules rather than configuration:

- item stack size: **64**;
- item cooldown: **0.5 seconds**;
- acquisition range: **32 blocks**;
- acquisition cone: approximately **15 degrees**;
- targetless/invalid-target reacquisition cadence: approximately every **4 ticks**;
- launched movement stays cardinal/orthogonal and uses the vanilla `SHULKER_BULLET` entity type.

A valid lock remains sticky until it becomes invalid. A directly sighted Target Block has priority during acquisition. These constants are part of the alpha.16 gameplay contract, not settings.

## Mounts and pets

Clinging-owned non-player transitions use the fixed **180/240 ms** tracked SNAP presentation. This remains distinct from the local player's 500 ms landing model. Active tracked snaps advance even while the entity is off-screen.

## Water

Normal and held Space remain vanilla swimming input. A second rising edge after a real release within **250 ms** requests Clinging/Reorientation. The detector does not consume or rewrite vanilla key state. While owned, water ascent/descent is world-vertical.

## Sprint-jump reservation

Normal effective jump power (`0.42`) reserves one tick near a predicted supported sprint landing. Stronger effective jump power expands the bounded horizon proportionally and caps it at **3 ticks**. This is derived from `JUMP_STRENGTH` plus vanilla Jump Boost rather than a Clinging setting.

## Legacy files

Earlier alphas created `config/clinging-reoriented-client.json`. Alpha.10 and later do not read or migrate it; an old file can be deleted safely or left ignored.

Unrelated Gravity Changer transitions, anchors/cores, commands and foreign mods retain their own presentation rules.
