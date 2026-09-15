# Configuration

Clinging: Reoriented 0.1.0-beta.2 exposes **no mod-owned configuration file**. The values below are fixed gameplay/presentation semantics rather than user preferences.

## Local-player camera and landing

A voluntary gravity change during free flight does **not** rotate the local camera. During sustained Gravity Fall, look input remains screen-relative through full-sphere pole crossings in both first and third person. During a physically predicted landing, local camera LAND and Gravity Fall BODY_LANDING share a **10-tick / 500 ms** presentation window. Invalidated support retains the exact current frame; context transfer releases obsolete presentation.

Ordinary tracked non-player SNAP remains separate: quarter turns use **180 ms** and opposite half turns **240 ms**.

## Gravity Fall body and air

- sustained Gravity Fall entry: **12 airborne ticks**;
- body-root entry blend: **6 ticks**;
- body look deadzone: **35 degrees**;
- maximum macro-body look follow: **7.5 degrees/tick**;
- maximum extra broadside posture drag: **1.3%/tick**;
- W air-diving redirect: at most **6 degrees/tick**, no authority for perpendicular/backward gaze and no added thrust;
- fast-air sound admission: **0.75 blocks/tick**, with **10-tick** fade-in;
- Clinging-controlled airborne world-speed cap: **3.92 blocks/tick**.

## Gravity Charge

Gravity Charge uses fixed rules rather than configuration:

- registry ID: **`clinging_reoriented:gravity_charge`**;
- item stack size: **64**;
- manual-use cooldown: **0.5 seconds**;
- acquisition range: **32 blocks**;
- acquisition cone: approximately **15 degrees**;
- targetless/invalid-target retry cadence: approximately every **4 ticks**;
- launched movement stays cardinal/orthogonal and uses vanilla `SHULKER_BULLET` entity type;
- a valid lock remains sticky until invalid;
- a directly sighted Target Block has absolute acquisition priority.

These constants are part of the beta gameplay contract, not settings.

## Localization

The player-facing locale files are `en_us` and `es_es`. CI requires exact key parity and non-empty Spanish strings. The final item key is `item.clinging_reoriented.gravity_charge`; its Spanish display name is **«Carga de gravedad»**.

## Mounts and pets

Clinging-owned non-player transitions use fixed **180/240 ms** tracked SNAP presentation. Active tracked snaps advance even while the entity is off-screen.

## Water

Normal and held Space remain vanilla swimming input. A second rising edge after a real release within **250 ms** requests Clinging/Reorientation. While owned, water ascent/descent is world-vertical. A retained camera frame created by a gravity turn while already inside fluid survives that fluid epoch; a later separate fluid entry still clears an older dry-flight HOLD.

## Sprint-jump reservation

Normal effective jump power (`0.42`) reserves one tick near predicted supported landing. Stronger effective jump power expands the bounded horizon and caps it at **3 ticks**.

## Legacy files

Earlier alphas created `config/clinging-reoriented-client.json`. Alpha.10 and later do not read or migrate it; an old file can be deleted safely or left ignored.

Unrelated Gravity Changer transitions, anchors/cores, commands and foreign mods retain their own presentation rules.
