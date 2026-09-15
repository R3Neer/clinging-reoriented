# TM: Alchemical Leather compatibility ownership

Status: **current-main implementation under adversarial validation** on branch `chatgpt/alchemical-leather-wear-compat-v2`.

This sprint ports the earlier Alchemical Leather compatibility prototype onto the current Clinging Reoriented architecture. The stale prototype branch was intentionally not merged or force-rebased because it had diverged by more than a hundred mainline commits; the semantic contract was reconstructed on a fresh branch from current `main`.

## Frozen contract

- Clinging Reoriented owns the Alchemical Leather slot declaration for `clinging_reoriented:reorientation`.
- Reorientation remains a boots effect.
- Alex's Mobs owns the `alexsmobs:clinging` registry effect; Clinging Reoriented owns only the semantic knowledge of whether its gravity-link action succeeded.
- Alchemical Leather integration is optional and linkage-safe. Clinging Reoriented must launch and behave normally without Alchemical Leather.
- A gravity-turn event is published only after the authoritative `ClingingReoriented.attempt(...)` path returns `SUCCESS`.
- Failed, blocked, unchanged, no-space, ambiguous and foreign-gravity attempts publish no turn event.
- Reorientation continuous work is published only while Reorientation owns airborne self locomotion.
- Passenger travel, moving/support-surface transport and anatomy support publish no continuous flight work.
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

## Reserved adversarial holdouts

The current-main port adds holdouts for:

- all three compatibility resources being present in the production classpath;
- no semantic owner when neither Clinging nor Reorientation is actually active;
- Clinging owning a turn when Clinging alone is active;
- Reorientation taking precedence when both effects are present;
- owned airborne Reorientation being eligible for continuous work;
- moving/support-surface state suppressing continuous work;
- removing Reorientation immediately suppressing continuous work.

The authoritative turn publisher remains injected at the single `attempt(ServerPlayer, Vec3, Vec3)` return boundary and checks for `SUCCESS`, so every non-success result remains a structural negative path rather than a list of duplicated exclusions.

## Acceptance gate

This sprint is complete only when:

1. current Clinging Reoriented standalone server/client CI passes with the new holdouts;
2. Clinging Reoriented still launches with no Alchemical Leather installed;
3. the coordinated Alchemical Leather fixture builds this current-main branch and passes its potion/wear coverage matrix;
4. the stale compatibility branch is no longer required by any fixture;
5. final documentation describes the optional integration;
6. a final adversarial review produces no production change.
