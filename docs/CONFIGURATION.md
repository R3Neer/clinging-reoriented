# Configuration

Clinging: Reoriented exposes **no mod-owned configuration file**. **0.1.0-beta.6** is the current prerelease; the values below describe its fixed navigation/gamefeel semantics, not user preferences.

## Local-player camera and landing

A voluntary gravity change during free flight does **not** rotate the local camera. During sustained Gravity Fall, look input remains screen-relative through full-sphere pole crossings in both first and third person.

Landing uses fixed acquisition plus contact-intent thresholds:

- acquisition horizon: **40 ticks / 2 seconds**;
- clear BODY_LANDING approach horizon: **10 ticks / 500 ms**;
- clear camera/input commitment: ETA <= **5 ticks / 250 ms**;
- ambiguous BODY_LANDING approach: two confirmed observations and ETA <= **4 ticks / 200 ms**;
- ambiguous camera/input commitment: two confirmed observations and ETA <= **3 ticks / 150 ms**;
- graze threshold: normal impact speed <= **12%** of total speed;
- clear threshold: normal impact speed >= **30%** of total speed;
- speed below **0.12 blocks/tick**: ambiguous regardless of angle;
- matching physical ground after a predicted graze is suppressed for **1 tick**; persistent support is accepted afterward;
- cancelled committed LAND returns to the retained pre-landing HOLD over **4 ticks / 200 ms**.

The predictor is refreshed every tick from the real current body/velocity/gravity state. Hysteresis alone cannot commit a landing, and player-facing graze classification does not alter the shared collision geometry used by mob planners. Context transfer releases obsolete ownership immediately.

Ordinary tracked non-player SNAP remains separate: quarter turns use **180 ms** and opposite half turns **240 ms**.

## Gravity Fall body and air

Fixed current values:

- sustained Gravity Fall entry: **12 airborne ticks**;
- body look deadzone: **35 degrees**;
- maximum macro-body gaze follow: **7.5 degrees/tick**;
- passive velocity/weathercock stabilization: **1.25 degrees/tick**;
- additional transverse aerodynamic drag: **2.5%/tick**;
- special W air-diving redirect: **none** in beta.4;
- fast-air sound admission: **0.75 blocks/tick**, with **10-tick** fade-in;
- Clinging-controlled airborne world-speed cap: **3.92 blocks/tick**.

Aerodynamics is body-relative and continuous: longitudinal momentum is retained while the transverse component receives the extra drag. It does not add thrust or Elytra-style lift.

## Gravity-aware mob planning

The mob planner also uses fixed structural budgets rather than player configuration:

- ordinary vanilla navigation is always attempted first;
- one local gravity plan evaluates at most **4 launch nodes × 5 alternate gravity directions = 20 physical transition forecasts**;
- at most **32 new grounded gravity plans per server level/tick**;
- at most **4 new grounded gravity plans per 64×64 X/Z region/tick**;
- excess planning enters `WAITING_PLAN` and retries later while preserving the live AI intent;
- failed-maneuver memory is bounded to **8 entries per executor**;
- negative-result replanning keeps its bounded cooldown rather than retrying every tick.

During a committed gravity flight there is no surface pathfinding. Dynamic monitoring uses:

`min(20, reactionTicks + 2)`

where reaction latency is derived from the mob's **base movement-speed attribute** and bounded to **2–10 ticks**. Falling faster does not grant faster reflexes.

A short vanilla navigation jump during `APPROACH` keeps the already-owned route/intent for at most **20 ticks**. No new planning or gravity commit is allowed while unsupported; a longer loss of support fails and cools down that maneuver.

These limits are gameplay/performance contracts, not tunable difficulty knobs.

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

## Localization

The player-facing locale files are `en_us` and `es_es`. CI requires exact key parity and non-empty Spanish strings. The item key is `item.clinging_reoriented.gravity_charge`; its Spanish display name is **«Carga de gravedad»**.

## Mounts and pets

Clinging-owned non-player visual transitions keep the fixed **180/240 ms** tracked SNAP presentation. Active tracked snaps advance even while the entity is off-screen.

Pet gravity follow has no breadcrumb, route-depth or owner-replay configuration. The beta.4 runtime is history-free and uses current/filtered owner intent plus current world geometry. Pet planning consumes the same global/regional budget as general mob navigation.

## Water

Normal and held Space remain vanilla swimming input. A second rising edge after a real release within **250 ms** requests Clinging/Reorientation.

While Clinging/Reorientation owns water movement:

- W/S follow camera forward/back including pitch;
- A/D follow camera left/right;
- Space is world **+Y**;
- Shift is world **-Y**.

Free swimming converges visually to world-up; real gravity-relative support converges to support-up. This presentation does not rewrite logical gravity. A retained camera frame created by a gravity turn while already inside fluid survives that fluid epoch; a later separate fluid entry still clears an older dry-flight HOLD.

## Sprint-jump reservation

Normal effective jump power (`0.42`) reserves one tick near predicted supported landing. Stronger effective jump power expands the bounded horizon and caps it at **3 ticks**.

## Legacy files

Earlier alphas created `config/clinging-reoriented-client.json`. Alpha.10 and later do not read or migrate it; an old file can be deleted safely or left ignored.

Unrelated Gravity Changer transitions, anchors/cores, commands and foreign mods retain their own presentation rules.
