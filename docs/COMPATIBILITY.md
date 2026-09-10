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

Alchemical Leather 0.1.0-alpha.2 can infuse Clinging and Reorientation into
compatible humanoid boots and into compatible BODY / animal armor. Humanoid
boots keep Alchemical Leather's effect-slot rule. BODY armor stores one potion
bundle and is not constrained by the humanoid slot table.

Normal/splash infusion clocks advance only while equipped; lingering infusions
remain stable while worn. The alpha.2 integration fixture uses this Clinging:
Reoriented alpha.6 release with real leather horse armor and wolf armor and
verifies equip/unequip effect ownership. Alchemical Leather remains optional.

### Scale Brews

The audited alpha.6 integration uses Scale Brews 0.1.0-beta.4. Eligible larger
entities offer six world-AABB surfaces; landing records the actual support and
inherits its translation while contact continues. Jumping ends continuous transport
while retaining gravity and the reference.

Scale Brews beta.5 contains a newer shared anatomical/gravity prototype, but that
path remains gated and disabled in ordinary gameplay. This project does not claim
animated-model collision compatibility as a released feature.

### First Person and Scale Visual Compat

The optional client mixin was exercised with First Person 2.7.2 and Scale Visual
Compat 0.1.0. It rotates and scales the existing body offset to match Gravity
Changer's camera. These mods are not required.

## Ownership boundaries

- Gravity Changer owns gravity state, physics, coordinate conversion and camera
  animation.
- Alex's Mobs owns Clinging and its original acquisition routes.
- Clinging: Reoriented owns voluntary turn policy, Reorientation, multiplayer
  requests, effect lifetime, mount transfer and pet turn trails.
- Alchemical Leather owns equipment-supplied effects.
- Scale Brews owns size eligibility and entity-surface policy.

No dependency JAR or third-party class is bundled in the production artifact.
