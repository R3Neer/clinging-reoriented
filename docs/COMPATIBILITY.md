# Compatibility

## Required projects

| Project | Tested version | Why it is required |
|---|---:|---|
| Alex's Mobs Continued | 2.1.9 | Supplies Clinging and its potion registry. |
| CodxLib | 1.5.1 | Runtime dependency of Alex's Mobs Continued. |
| Gravity Changer Unofficial Port | 1.5.2-beta.5-mc26.2 | Owns gravity attributes, coordinate transforms, movement and collision physics. |
| Cloth Config API | 26.2.155 | Runtime dependency used by the gravity stack. |

Fabric API 0.159.0+26.2, Fabric Loader 0.19.5 and Java 25 are also required for
Minecraft 26.2. Both the client and server need the same mod version.

## Optional projects

### Alchemical Leather

Alchemical Leather 0.1.0-alpha.3 can infuse Clinging and Reorientation into
compatible humanoid boots and compatible BODY / animal armor. Reorientation remains
deliberately absent from the villager-trade economy. Alchemical Leather is optional.

### Scale Brews

Scale Brews is **not part of the current alpha.12 runtime/support target** while its
larger shared entity-collision architecture remains under active development.
Clinging's production sources and compile classpath do not depend on Scale Brews.

The transitional platform integration is isolated behind a reflective bridge and a
pseudo mixin. If Scale Brews is absent or its known API is incompatible, that bridge
disables itself while base Clinging gameplay remains available. CI first proves the
required server/client suites without Scale, then loads public beta.5 only in
isolated optional server/client lanes with `-PwithScaleBrews`.

Tiny Mounts have no special Clinging gravity mixins: externally they are ordinary
compatible living root vehicles. Scale owns gravity-awareness of movement vectors
that Scale itself generates. Alpha.12's mounted visual transport is generic too: a
compatible root mount receives the same Clinging-owned entity snap regardless of
whether Scale Brews exists.

### First Person

The optional client mixin targets First Person 2.7.2. Its contract remains narrow:
it transforms First Person's existing body offset only for visual frames/transitions
owned by Clinging. The orientation trajectory itself is not a separate First Person
implementation; camera, model and First Person all consume the same Clinging-owned
Gravity Changer visual quaternion.

The compatibility lane installs First Person 2.7.2 with Not Enough Animations
1.12.4 and runs the real client GameTest without Scale Brews. It checks unowned
Gravity Changer behavior, Clinging ownership, third-party world-space offsets and
the v2 local-player snap transition boundary. Alpha.11 changed the request-side
intent protocol to `select_intent_v3`; alpha.12 retains that protocol and adds a
separate tracked-entity visual payload for mounts/pets rather than changing the
player packet schema.

### Jump-strength modifiers

Alpha.12's sprint-landing reservation reads Minecraft's effective
`Attributes.JUMP_STRENGTH` plus `Player.getJumpBoostPower()`. Vanilla Jump Boost and
mods that expose compatible jump-strength changes therefore extend only the
near-landing prediction without requiring potion-name integration. The baseline is
one tick at normal `0.42` jump power and the horizon is capped at three ticks.

## Ownership boundaries

- Gravity Changer owns gravity attributes, coordinate conversion, movement and
  collision physics, plus presentation for changes not initiated by Clinging.
- Alex's Mobs owns Clinging and its original acquisition routes.
- Clinging: Reoriented owns voluntary turn policy, Reorientation, selection/heading
  intent separation, heading transport, underwater double-Space arbitration,
  jump-power-aware sprint-landing intent, its fixed snap presentation epochs,
  tracked mount/pet snap ownership for gravity changes it commits, fall-segment
  resets, gravity-retirement responsibility, multiplayer requests, bounded mount
  loans and pet turn trails.
- A foreign Gravity Changer write to a player or mob remains foreign. It does not
  receive a Clinging visual packet, yaw-gauge mutation or per-entity ownership epoch.
- First Person owns its model/body-offset baseline; Clinging rotates that baseline
  only inside a Clinging-owned visual frame.
- Alchemical Leather owns equipment-supplied effects.
- Scale Brews owns Scale-generated size/mount/anatomical mechanics when present; it
  is neither a required runtime dependency nor a production compile dependency.

No dependency JAR or third-party class is bundled in the production artifact.
