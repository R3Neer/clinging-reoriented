# Compatibility

## Required projects

| Project | Tested version | Role |
|---|---:|---|
| Alex's Mobs Continued | 2.1.9 | Supplies Clinging and its potion registry. |
| CodxLib | 1.5.1 | Runtime dependency of Alex's Mobs Continued. |
| Gravity Changer Unofficial Port | 1.5.2-beta.5-mc26.2 | Gravity attributes, coordinate transforms, directional navigation and movement/collision physics. |
| Cloth Config API | 26.2.155 | Runtime dependency used by the gravity stack. |

Fabric API 0.159.0+26.2, Fabric Loader 0.19.5 and Java 25 are also required for Minecraft 26.2. Client and server need matching Clinging: Reoriented versions.

## First Person

The supported optional target is **First Person 2.7.2** with **Not Enough Animations 1.12.4**. The local Gravity Fall body uses a blended body/camera pivot; the actual First Person camera remains independent. The pinned lane covers sustained fall, BODY_LANDING and steep look-down checkpoints.

## Fresh Animations / Player Extension / EMF / ETF

The pinned optional lane uses Fresh Animations 1.10.5, Player Extension 1.1, EMF 3.3.5 and ETF 7.2. These mods retain internal limb/head/equipment animation while Clinging applies only the macroscopic body root.

## Scale Brews

Scale Brews beta.5 is optional and test-only from Clinging's production classpath. Existing Scale/Anatomy bridges fail closed. The public `LandingSurfaceProvider` contract contains no Scale types.

## Alchemical Leather

Alchemical Leather remains optional. Compatible equipment may supply Clinging/Reorientation effects; Clinging owns the movement semantics once those effects are active.

## Gravity Changer and pet navigation

Alpha.15's pet breadcrumb pursuit intentionally cooperates with Gravity Changer rather than replacing its directional navigator.

Gravity Changer may replace a tame mob's `PathNavigation` object after the mob changes gravity. Vanilla `FollowOwnerGoal` caches navigation in a field, so Clinging refreshes that cached reference during goal admission, continuation, start/stop and tick. The breadcrumb route is therefore driven by the **current** gravity-aware navigator after every turn.

Breadcrumb targets are projected onto the pet's current gravity-relative movement plane before being handed to navigation. Clinging does not ask Gravity Changer to pathfind through an unreachable third axis, and it does not remotely teleport/rotate the pet to the owner.

If replay requires the bounded center-aligned placement used to clear the old support, that internal relocation is marked as part of the breadcrumb transaction and does not erase queued steps. An unrelated external teleport is a true lifecycle discontinuity and invalidates the route.

When no valid breadcrumb exists, or the pet lacks its own compatible effect, Clinging does not broaden `FollowOwnerGoal` and vanilla following remains unchanged. Foreign gravity ownership remains foreign.

## Fluids from other mods

`FluidContext` inspects non-empty `FluidState` volumes rather than special-casing vanilla water/lava, so modded fluids suspend support/landing/Gravity Fall consistently without per-mod integration.

## Ownership boundaries

- **Gravity Changer**: physical gravity attributes, coordinate transforms, directional navigation and general movement/collision behaviour; presentation for changes not owned by Clinging.
- **Clinging: Reoriented**: voluntary turn policy, Clinging/Reorientation budgets, retained/full-sphere camera, landing, Gravity Fall macro body, bounded aerodynamics/air-diving, safety, impact, directional mace and eligible owner-breadcrumb replay.
- **Vanilla tame AI**: normal owner-follow behaviour and path/goal semantics; Clinging only temporarily substitutes the target while a valid gravity breadcrumb is pending.
- **First Person**: camera/model baseline; Clinging may transform its body pass but not the real camera.
- **Fresh Animations/EMF/ETF**: optional internal model animation.
- **Scale Brews**: Scale-specific size/mount/anatomical mechanics.
- **Alchemical Leather**: equipment-supplied effects.

No required or optional dependency JAR is bundled in the production artifact.
