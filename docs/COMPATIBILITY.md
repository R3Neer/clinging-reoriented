# Compatibility

## Required projects

| Project | Tested version | Role |
|---|---:|---|
| Alex's Mobs Continued | 2.1.9 | Supplies Clinging and its potion registry. |
| CodxLib | 1.5.1 | Runtime dependency of Alex's Mobs Continued. |
| Gravity Changer Unofficial Port | 1.5.2-beta.5-mc26.2 | Gravity attributes, coordinate transforms, movement/collision physics. |
| Cloth Config API | 26.2.155 | Runtime dependency used by the gravity stack. |

Fabric API 0.159.0+26.2, Fabric Loader 0.19.5 and Java 25 are also required for Minecraft 26.2. Client and server need matching Clinging: Reoriented versions.

## First Person

The supported optional target is **First Person 2.7.2** with **Not Enough Animations 1.12.4**. Alpha.13 keeps one camera authority: Gravity Fall can rotate the rendered body root and BODY_LANDING can converge it toward support, but neither transform is allowed to feed back into First Person's camera.

The pinned client lane covers normal Gravity Fall and BODY_LANDING checkpoints as well as ownership boundaries. First Person owns its own baseline model/body offset; Clinging transforms only the state it legitimately owns.

## Fresh Animations / Player Extension / EMF / ETF

Alpha.13 has a reproducible compatibility lane with:

- Fresh Animations 1.10.5
- Fresh Animations Player Extension 1.1
- Entity Model Features 3.3.5
- Entity Texture Features 7.2

The fixtures are checksum-pinned in CI. Fresh Animations keeps ownership of limbs, head tracking, equipment and internal animation. Clinging's Gravity Fall code applies only the macroscopic root orientation. CI preserves snapshots for sustained fall, landing midpoint and final landing frame.

These projects remain optional and are not bundled.

## Scale Brews

Scale Brews is **not a required dependency**. Production compile classpath remains Scale-free. CI loads public **beta.5** only in isolated optional server/client lanes.

Existing transitional Scale/Anatomy reflection and pseudo-mixin bridges remain fail-closed. Tiny Mounts are ordinary compatible living root vehicles to Clinging's mounted-gravity code; Scale owns movement mechanics that Scale itself creates.

Alpha.13's new `LandingSurfaceProvider` API deliberately contains no Scale Brews classes. Concrete integration that declares Scale-specific surfaces valid for Clinging landing prediction is outside alpha.13 and should live in a consumer/adapter layer rather than making the base API depend on Scale internals.

## Alchemical Leather

Alchemical Leather remains optional. Its compatible equipment may supply Clinging/Reorientation effects, while Clinging continues to own the gravity-selection, landing and impact semantics once those effects are active.

## Jump-strength modifiers

Sprint-landing reservation reads effective `JUMP_STRENGTH` plus vanilla Jump Boost power. Compatible modifiers therefore influence only the bounded one-to-three-tick near-landing reservation without potion-name integration.

## Ownership boundaries

- **Gravity Changer**: physical gravity attributes, coordinate transforms and general movement/collision behavior; presentation for changes not owned by Clinging.
- **Clinging: Reoriented**: voluntary turn policy, one-turn Clinging budget, Reorientation, free-flight camera HOLD, landing commitment/LAND, Gravity Fall body root, visual-frame controls, impact lifecycle, owned mount/pet transitions, recovery and lifecycle fencing.
- **Vanilla / registered landing providers**: collision/support facts. Providers cannot choose input policy, camera ownership or placement.
- **First Person**: its camera/model baseline; Clinging cannot feed the Gravity Fall body root into the camera.
- **Fresh Animations/EMF/ETF**: optional internal model animation; Clinging owns only macro root orientation while active.
- **Scale Brews**: Scale-specific size/mount/anatomical mechanics and any future adapter it chooses to provide.
- **Alchemical Leather**: equipment-supplied effects.

A foreign gravity write remains foreign. No required or optional dependency JAR is bundled in the production artifact.
