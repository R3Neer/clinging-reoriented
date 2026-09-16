# Design archive

The documents in this directory are **historical pre-implementation design records**, not the normative description of the current beta.4 development runtime.

They are intentionally retained because they record the reasoning that led to the NAV campaign, including rejected assumptions and intermediate designs. Statements such as “SIN IMPLEMENTACIÓN”, “APARCADO”, breadcrumb-era comparisons, W air-diving discussion or earlier owner-airborne policy describe the state **when that design note was written** and must not be read as current behaviour.

Current normative sources are:

- [`../../README.md`](../../README.md) — public development overview;
- [`../GUIDE.md`](../GUIDE.md) — current player-facing behaviour;
- [`../ARCHITECTURE.md`](../ARCHITECTURE.md) — current runtime architecture;
- [`../sprints/NAV-S03-anticipated-landing.md`](../sprints/NAV-S03-anticipated-landing.md) — 40-tick landing acquisition / 10-tick presentation;
- [`../sprints/NAV-S04-water-controls-camera.md`](../sprints/NAV-S04-water-controls-camera.md) — camera-relative water controls and visual frame;
- [`../sprints/NAV-S05-gravity-mob-planner.md`](../sprints/NAV-S05-gravity-mob-planner.md) — bounded general mob planner;
- [`../sprints/NAV-S06-pet-follow-executor.md`](../sprints/NAV-S06-pet-follow-executor.md) and [`NAV-S06-general-mob-goals.md`](../sprints/NAV-S06-general-mob-goals.md) — history-free pet follow and general vanilla-goal integration;
- [`../sprints/NAV-S07-dynamic-reaction.md`](../sprints/NAV-S07-dynamic-reaction.md) — dynamic target/world reaction;
- [`../sprints/NAV-S08-efficiency-adversarial.md`](../sprints/NAV-S08-efficiency-adversarial.md) — planning/monitor budgets and adversarial closure;
- [`../sprints/NAV-S09-convergence.md`](../sprints/NAV-S09-convergence.md) — final convergence gate.

## Resolution map

| Historical design note | Implemented/resolved by |
| --- | --- |
| `PARKED-GAMEFEEL-AERODYNAMICS-WATER.md` | NAV-S02 aerodynamics + NAV-S04 water |
| `PARKED-GAMEFEEL-FREEFALL-LANDING.md` | NAV-S03 anticipated landing |
| `PET-GRAVITY-FOLLOW-REDESIGN.md` | NAV-S05/S06 history-free pet planner/executor |
| `MOB-GRAVITY-NAVIGATION-REDESIGN.md` | NAV-S05/S06 general mob navigation |
| `MOB-GRAVITY-NAVIGATION-REACTION-TIME.md` | NAV-S07 reaction model + NAV-S08 monitor budget |

Git history remains the source of chronology. New behavioural changes should update the normative docs/sprint records above rather than silently editing these historical design snapshots into a different document.
