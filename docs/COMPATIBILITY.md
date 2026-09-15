# Compatibility

## Required projects

| Project | Tested version | Role |
|---|---:|---|
| Alex's Mobs Continued | 2.1.9 | Supplies Clinging and its potion registry. |
| CodxLib | 1.5.1 | Runtime dependency of Alex's Mobs Continued. |
| Gravity Changer Unofficial Port | 1.5.2-beta.5-mc26.2 | Gravity attributes, coordinate transforms, movement/collision physics. |
| Cloth Config API | 26.2.155 | Runtime dependency used by the gravity stack. |

Fabric API 0.159.0+26.2, Fabric Loader 0.19.5 and Java 25 are also required for Minecraft 26.2. Client and server need matching Clinging: Reoriented versions.

## Gravity Charge and vanilla projectile semantics

Gravity Charge adds **no new dependency**. A launched Gravity Charge keeps the exact vanilla `minecraft:shulker_bullet` entity type. The project layers Gravity Charge state and routing onto that entity rather than registering a parallel projectile type, so vanilla projectile impact, Levitation, renderer and shulker-duplication checks continue to observe the expected type.

The item itself is project-owned: GUI icon, 3D geometry and `gravity_charge` texture are original GPL-3.0-or-later assets. Minecraft's ShulkerBullet renderer/model/texture remain vanilla runtime resources and are not copied or redistributed by this repository.

Reorientation brewing uses Gravity Charge instead of Shulker Shell. Alex's Mobs Continued remains the source of the Clinging potion/effect used as the base input.

## First Person

The supported optional target is **First Person 2.7.2** with **Not Enough Animations 1.12.4**. The local avatar macro root uses a blended body-center/camera pivot during Gravity Fall; that root never feeds back into the actual camera.

## Fresh Animations / Player Extension / EMF / ETF

The reproducible optional lane uses Fresh Animations 1.10.5, Fresh Animations Player Extension 1.1, Entity Model Features 3.3.5 and Entity Texture Features 7.2. Fixtures are checksum-pinned in CI. These mods retain ownership of limbs, head tracking, equipment and internal animation; Clinging applies only the macroscopic Gravity Fall root.

## Scale Brews

Scale Brews is **not a required dependency**. Production compile classpath remains Scale-free. CI loads public **beta.5** only in isolated optional server/client lanes.

Existing transitional Scale/Anatomy bridges remain fail-closed. Tiny Mounts are ordinary compatible living root vehicles to Clinging's mounted-gravity code; Scale owns movement mechanics that Scale itself creates.

`LandingSurfaceProvider` deliberately contains no Scale Brews types. A concrete Scale-specific landing-surface adapter belongs in a consumer/integration layer.

Gravity Charge likewise contains no Scale-specific production path. Its targeting/routing operate on ordinary Minecraft entity/block contracts; the optional Scale lane verifies that this runtime does not alter acquisition/retry semantics.

## Alchemical Leather

Alchemical Leather is optional and is never a compile-time dependency of Clinging: Reoriented. The optional adapter resolves Alchemical Leather's small public wear API reflectively only when `alchemical_leather` is loaded; if the API is absent or incompatible, the bridge disables itself without changing gravity gameplay.

Clinging: Reoriented owns the semantic facts that only this mod can know:

- a `clinging_reoriented:gravity_turn` event is published only after the authoritative gravity-attempt path returns `SUCCESS`;
- Clinging uses successful voluntary turns as discrete work;
- Reorientation uses successful turns plus controlled airborne self-locomotion;
- passenger travel, moving/support-surface transport and anatomy support do not publish continuous Reorientation work.

The owning effect is resolved from the player's real active effects. Reorientation takes precedence when both Reorientation and Alex's Mobs Clinging are present; no event owner is invented when neither effect is active.

Compatibility balance is data-owned. Clinging: Reoriented ships Reorientation's boots slot plus wear rules for Reorientation and Alex's Mobs Clinging. Alchemical Leather remains responsible for deciding which equipped infused item actually owns an effect, handling stronger/equal external-effect eclipse, accumulating fractional work and applying ordinary item durability damage. Clinging never selects or damages armor itself.

The current implementation and TM evidence are documented in [TM_ALCHEMICAL_LEATHER_COMPAT.md](TM_ALCHEMICAL_LEATHER_COMPAT.md).

## Fluids and jump modifiers

Fluid handling is deliberately generic: non-empty `FluidState` volume suspends Clinging support/landing/Gravity Fall without per-mod integration. Sprint-landing reservation reads effective `JUMP_STRENGTH` plus vanilla Jump Boost power and remains bounded to one-to-three ticks.

## Localization

`en_us` and Spanish-from-Spain `es_es` are first-class player-facing locales. CI enforces exact key parity and non-empty Spanish values. Gravity Charge is **«Carga de gravedad»** in `es_es`.

## Ownership boundaries

- **Minecraft vanilla**: ShulkerBullet entity type, projectile collision/Levitation, projectile renderer/resources, Target Block hit/redstone semantics and shulker-duplication mechanics.
- **Clinging: Reoriented**: Gravity Charge item/capture/acquisition/cardinal routing plus voluntary gravity policy, camera HOLD/full-sphere Gravity Fall, landing commitment, body root, air-diving/aerodynamics, safety, impact, mace and owned mount/pet transitions. When Alchemical Leather is present, Clinging also owns publication of successful-turn and controlled-Reorientation semantic facts.
- **Gravity Changer**: physical gravity attributes, coordinate transforms and general movement/collision behaviour; presentation for foreign changes.
- **Vanilla / registered landing providers**: collision/support facts subject to shared fluid fencing.
- **First Person**: camera/model baseline; Clinging may transform its body pass but not the real camera.
- **Fresh Animations/EMF/ETF**: internal model animation; Clinging owns only macro root orientation while active.
- **Scale Brews**: Scale-specific size/mount/anatomical mechanics.
- **Alchemical Leather**: infusion storage/source arbitration, armor ownership, wear-rule interpretation, fractional accounting and durability application.

No required or optional dependency JAR is bundled in the production artifact.
