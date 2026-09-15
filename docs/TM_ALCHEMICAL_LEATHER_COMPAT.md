# TM: Alchemical Leather compatibility ownership

Status: **implementation converged; final documentation gate pending** on branch `chatgpt/alchemical-leather-wear-compat-v2`.

This sprint ports the earlier Alchemical Leather compatibility prototype onto the current Clinging Reoriented architecture. The stale prototype branch was intentionally not merged or force-rebased because it had diverged by more than a hundred mainline commits; the semantic contract was reconstructed on a fresh branch from current `main`.

## Frozen contract

- Clinging Reoriented owns the Alchemical Leather slot declaration for `clinging_reoriented:reorientation`.
- Reorientation remains a boots effect.
- Alex's Mobs owns the `alexsmobs:clinging` registry effect; Clinging Reoriented owns only the semantic knowledge of whether its gravity-link action succeeded.
- Alchemical Leather integration is optional and linkage-safe. Clinging Reoriented must launch and behave normally without Alchemical Leather.
- A gravity-turn event is published only after the authoritative `ClingingReoriented.attempt(...)` path returns `SUCCESS`.
- Failed, blocked, unchanged, no-space, ambiguous and foreign-gravity attempts publish no turn event.
- Reorientation continuous work is published only while Reorientation owns airborne self locomotion.
- Passenger travel, moving/support-surface transport, Anatomy support, fluid locomotion, Elytra and independent player flight publish no continuous Reorientation work.
- The compatibility adapter reports semantic facts only. It never chooses an armor stack, mutates durability or knows Alchemical Leather balance/accounting state.

## Resource ownership

Clinging Reoriented ships:

- `data/clinging_reoriented/alchemical_leather/effect_slots/reorientation.json`
- `data/clinging_reoriented/alchemical_leather/wear_rules/reorientation.json`
- `data/alexsmobs/alchemical_leather/wear_rules/clinging.json`

Reorientation uses 30 work per durability point, with `0.05` work per controlled-flight tick and `2.0` work per successful gravity turn. Clinging uses the same 30-work denominator with `4.0` work per successful gravity turn. Balance remains data-owned rather than hard-coded into the event publisher.

## Linkage boundary

`AlchemicalLeatherCompat` is a normal optional initializer. It first checks Fabric Loader for `alchemical_leather`, then reflectively resolves the minimal public `InfusionWearApi.emit(...)` method. If the mod or API is absent/incompatible, the bridge stays inert. No Alchemical Leather production class appears in Clinging Reoriented's compile-time type graph.

A failed reflective event call disables the bridge for the remainder of that process rather than destabilizing gravity gameplay.

## Adversarial execution

The current-main port added reserved holdouts for:

- all three compatibility resources being present in the production classpath;
- no semantic owner when neither Clinging nor Reorientation is actually active;
- Clinging owning a turn when Clinging alone is active;
- Reorientation taking precedence when both effects are present;
- owned airborne Reorientation being eligible for continuous work;
- moving/support-surface state suppressing continuous work;
- removing Reorientation immediately suppressing continuous work.

The authoritative turn publisher remains injected at the single `attempt(ServerPlayer, Vec3, Vec3)` return boundary and checks for `SUCCESS`, so every non-success result remains a structural negative path. The current mounted implementation returns `SUCCESS` for a valid airborne Reorientation turn and `MOUNT_ACTION` for the grounded mount action, preserving the intended discrete-event distinction without a second special hook.

### Production defect found during final adversarial review

The first current-main implementation used Clinging physics ownership plus airborne/support checks to decide whether continuous Reorientation work should be published. A final adversarial pass found that this was still too broad: Reorientation could remain marked as physics-owned while **Elytra**, a **non-empty fluid context** or **independent player flight** actually owned locomotion. That would have charged armor for work Reorientation did not perform.

Production commit `300350e333f1d9e1205b1e0faae68b7fbdeb3714` corrected the eligibility predicate. Continuous wear is now rejected for Elytra fall-flying, fluid intersection, player flight, passengers, dead/spectator/sleeping players, support-surface transport and Anatomy support.

Holdout commit `f6c928e9a6d11ae4d4f7857b35e1179b8e81771e` permanently exercises those exclusions with real Elytra and fluid states in addition to the earlier ownership/support cases.

## Functional convergence evidence

PR #30 head `f6c928e9a6d11ae4d4f7857b35e1179b8e81771e` passed complete workflow run **#874** (`35030691657`) after the production fix and expanded holdouts.

The green matrix includes:

- English/Spanish localization parity;
- build and unit tests without optional Scale Brews;
- server GameTests without optional Scale Brews or Alchemical Leather;
- default client GameTests;
- First Person ownership GameTests;
- optional Scale Brews server compatibility GameTests;
- optional Scale Brews client load lane;
- exact VanillaPlus Fresh Animations fixture;
- Fresh Animations Gravity Fall client lane;
- semantic visual snapshot validation;
- artifact/diagnostic upload.

No further Clinging production change was required after the Elytra/fluid/foreign-flight fix and its reserved holdouts. The remaining closeout is documentation-only, followed by the same full matrix on the exact documentation-complete head and the coordinated Alchemical Leather cross-mod gate.

## Acceptance gate

1. Current Clinging Reoriented standalone/server/client/optional visual matrix: **PASS** on run `35030691657`.
2. Clinging Reoriented launches with no Alchemical Leather installed: **PASS**, proven by the ordinary standalone lanes in that same run.
3. Coordinated Alchemical Leather fixture builds this current-main branch and validates the real cross-mod wear path: **in progress on the Alchemical Leather S05 branch**.
4. The stale compatibility branch is no longer required by any final fixture: **PASS**; the new branch is built from current `main`.
5. Final player/architecture/compatibility/validation documentation describes the optional integration: **PASS** on the branch, pending exact documentation-head CI.
6. Final adversarial review after the production fix found no additional Clinging production defect: **PASS**.
