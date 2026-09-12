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

Scale Brews is **not part of the current alpha.9 runtime/support target** while its
larger shared entity-collision architecture is still under active development.
Clinging's production sources and compile classpath do not depend on Scale Brews.

The remaining transitional legacy platform integration is isolated behind a
reflective bridge and a pseudo mixin. Clinging detects the specific legacy API it
knows how to use; if Scale Brews is absent or that API is incompatible, the
integration disables itself while base Clinging gameplay remains available. No
Scale class is linked directly from production bytecode solely to make that
compatibility compile.

CI first builds and runs the required server/client suites with no Scale Brews JAR
present. A separate optional lane downloads the public beta.5 JAR into `test-libs`
and loads it only as a runtime fixture with `-PwithScaleBrews`, preserving
compatibility coverage without turning Scale into a build dependency.

Tiny Mounts no longer have special Clinging mixins: externally they are ordinary
compatible living root vehicles. Scale owns the gravity-awareness of the
flight/glide/pounce vectors that Scale itself generates.

When Scale's shared collision/reconciliation API reaches its G7 prerequisites,
Clinging should migrate anatomical contact/carry/reference ownership there and
delete the remaining duplicate bridge/path. That future migration is separate
from generic mounted gravity.

### First Person

The optional client mixin targets First Person 2.7.2. Its current contract is
narrow: it transforms First Person's existing body offset only for visual frames
or transitions owned by Clinging: Reoriented. Unrelated Gravity Changer gravity
retains First Person's ordinary behavior. First Person is not required.

The compatibility lane installs First Person 2.7.2 together with Not Enough
Animations 1.12.4 and runs the real client GameTest without Scale Brews. The fixture
checks an external/unowned gravity baseline, the same frame under Clinging ownership,
and an additional third-party world-space offset handler. Scale Visual Compat is no
longer required by this Clinging compatibility contract.

## Ownership boundaries

- Gravity Changer owns gravity state, physics, coordinate conversion and camera
  animation.
- Alex's Mobs owns Clinging and its original acquisition routes.
- Clinging: Reoriented owns voluntary turn policy, Reorientation, explicit
  gravity-retirement responsibility, multiplayer requests, bounded mount loans,
  pet turn trails and the presentation epochs of its own transitions.
- Alchemical Leather owns equipment-supplied effects.
- Scale Brews owns Scale-generated size/mount/anatomical mechanics when that
  project is used; it is neither a required runtime dependency nor a production
  compile dependency.

No dependency JAR or third-party class is bundled in the production artifact.
