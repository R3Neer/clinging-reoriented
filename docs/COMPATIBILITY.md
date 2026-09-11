# Compatibility

## Required projects

| Project | Tested version | Why it is required |
|---|---:|---|
| Alex's Mobs Continued | 2.1.9 | Supplies Clinging and its potion registry. |
| CodxLib | 1.5.1 | Runtime dependency of Alex's Mobs Continued. |
| Gravity Changer Unofficial Port | 1.5.2-beta.5-mc26.2 | Owns gravity attributes, coordinate transforms, movement and camera rotation. |
| Cloth Config API | 26.2.155 | Runtime dependency used by the gravity stack. |

Fabric API 0.159.0+26.2, Fabric Loader 0.19.5 and Java 25 are also required for
Minecraft 26.2. Both the client and server need the same mod version.

## Optional projects

### Alchemical Leather

Alchemical Leather 0.1.0-alpha.3 can infuse Clinging and Reorientation into
compatible humanoid boots and into compatible BODY / animal armor. Humanoid
boots keep Alchemical Leather's effect-slot rule. BODY armor stores one potion
bundle and is not constrained by the humanoid slot table.

Normal/splash infusion clocks advance only while equipped; lingering infusions
remain stable while worn. Alchemical Leather remains optional. Its alpha.3
villager-trade system deliberately does not sell Reorientation, which does not
change direct infusion compatibility.

### Scale Brews

Scale Brews is **not part of the alpha.7 runtime/support target** while its larger
shared entity-collision architecture is still under active development. Alpha.7
CI intentionally validates Clinging without loading Scale Brews at runtime.

The repository still compiles its transitional anatomical bridge against the
public beta.5 API so the future G7 migration remains buildable, but this is not a
claim of current released gameplay support. Tiny Mounts no longer have special
Clinging mixins: externally they are ordinary compatible living root vehicles.
Scale owns the gravity-awareness of the flight/glide/pounce vectors that Scale
itself generates.

When Scale's shared collision/reconciliation API reaches its G7 prerequisites,
Clinging should migrate anatomical contact/carry/reference ownership there and
delete the remaining duplicate bridge/path. That future migration is separate
from generic mounted gravity.

### First Person

The optional client mixin targets First Person 2.7.2. Its contract in alpha.7 is
narrow: it transforms First Person's existing body offset only for visual frames
or transitions owned by Clinging: Reoriented. Unrelated Gravity Changer gravity
must retain First Person's ordinary behavior. First Person is not required.

The alpha.7 default CI does not install First Person; therefore its current runtime
fixture is tracked separately from the core release evidence rather than being
implied by a green default build.

## Ownership boundaries

- Gravity Changer owns gravity state, physics, coordinate conversion and camera
  animation.
- Alex's Mobs owns Clinging and its original acquisition routes.
- Clinging: Reoriented owns voluntary turn policy, Reorientation, explicit
  gravity-retirement responsibility, multiplayer requests, bounded mount loans,
  pet turn trails and the presentation epochs of its own transitions.
- Alchemical Leather owns equipment-supplied effects.
- Scale Brews owns Scale-generated size/mount/anatomical mechanics when that
  project is used; it is not an alpha.7 runtime dependency.

No dependency JAR or third-party class is bundled in the production artifact.
