# Compatibility

## Required projects

| Project | Tested version | Role |
|---|---:|---|
| Alex's Mobs Continued | 2.1.9 | Supplies Clinging and its potion registry. |
| CodxLib | 1.5.1 | Runtime dependency of Alex's Mobs Continued. |
| Gravity Changer Unofficial Port | 1.5.2-beta.5-mc26.2 | Gravity attributes, coordinate transforms, movement/collision physics. |
| Cloth Config API | 26.2.155 | Runtime dependency used by the gravity stack. |

Fabric API 0.159.0+26.2, Fabric Loader 0.19.5 and Java 25 are also required for Minecraft 26.2. Client and server need matching Clinging: Reoriented versions.

**0.1.0-beta.6** is the current prerelease. It keeps the shared beta.4/5 physics and adds a local-player landing contact-intent layer so a tangential feet graze is not treated as planted support.

## Gravity Charge and vanilla projectile semantics

Gravity Charge adds **no new dependency**. A launched Gravity Charge keeps the exact vanilla `minecraft:shulker_bullet` entity type. The project layers Gravity Charge state and routing onto that entity rather than registering a parallel projectile type, so vanilla projectile impact, Levitation, renderer and shulker-duplication checks continue to observe the expected type.

The item itself is project-owned: GUI icon, 3D geometry and `gravity_charge` texture are original GPL-3.0-or-later assets. Minecraft's ShulkerBullet renderer/model/texture remain vanilla runtime resources and are not copied or redistributed by this repository.

Reorientation brewing uses Gravity Charge instead of Shulker Shell. Alex's Mobs Continued remains the source of the Clinging potion/effect used as the base input.

## First Person

The supported optional target is **First Person 2.7.2** with **Not Enough Animations 1.12.4**. The local avatar macro root uses a blended body-center/camera pivot during Gravity Fall; that root never feeds back into the actual camera.

Beta.6 keeps full-sphere camera ownership separate from persistent body attitude and posture-driven aerodynamics. First Person still owns the camera/model baseline; Clinging transforms only its permitted macro body pass.

## Fresh Animations / Player Extension / EMF / ETF

The reproducible optional lane uses Fresh Animations 1.10.5, Fresh Animations Player Extension 1.1, Entity Model Features 3.3.5 and Entity Texture Features 7.2. Fixtures are checksum-pinned in CI. These mods retain ownership of limbs, head tracking, equipment and internal animation; Clinging applies only the macroscopic Gravity Fall root.

The beta.4 persistent body-attitude layer does not take over Fresh Animations' internal pose channels. Its root orientation and anisotropic aerodynamics remain outside those limb/head/equipment responsibilities.

## Scale Brews

Scale Brews is **not a required dependency**. Production compile classpath remains Scale-free. CI loads public **beta.5** only in isolated optional server/client lanes.

Existing transitional Scale/Anatomy bridges remain fail-closed. Tiny Mounts are ordinary compatible living root vehicles to Clinging's mounted-gravity code; Scale owns movement mechanics that Scale itself creates.

`LandingSurfaceProvider` deliberately contains no Scale Brews types. A concrete Scale-specific landing-surface adapter belongs in a consumer/integration layer.

Gravity Charge likewise contains no Scale-specific production path. Its targeting/routing operate on ordinary Minecraft entity/block contracts; the optional Scale lane verifies that this runtime does not alter acquisition/retry semantics.

Gravity-aware mob planning uses ordinary `Mob`, `PathNavigation`, AABB and landing-surface contracts. It does not gain Scale-specific speculative geometry or compile-time Scale APIs.

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

Water presentation/control remains a Clinging-owned policy rather than a per-water-mod adapter: camera-relative WASD, world-vertical Space/Shift, world-up free-swim presentation and support-up presentation when genuinely supported. Logical gravity remains separate.

## Gravity-aware mobs and vanilla AI ownership

Beta.4 adds no optional AI dependency and does not replace Minecraft's high-level goal ownership.

- vanilla goals still decide **why** a mob follows, pursues, flees or moves toward a position;
- ordinary `PathNavigation` gets first refusal;
- the Clinging planner only expands locomotion when a mob legitimately has Clinging/Reorientation capability and the ordinary route cannot satisfy the live intent;
- `MeleeAttackGoal` / `AvoidEntityGoal` wake adapters expose otherwise-lost intents but do not become species-specific combat/fear AIs;
- pet follow is history-free and no longer depends on owner gravity breadcrumbs;
- external gravity ownership is never stolen;
- committed gravity flight does not run surface pathfinding;
- a short vanilla navigation jump during `APPROACH` preserves the already-owned route/intent for at most 20 ticks, but no planning or gravity commit is allowed until real support returns.

Planning remains bounded independently of optional mods: at most 20 physical transition forecasts per local plan, 32 new grounded gravity plans per level/tick and 4 per 64×64 region/tick.

## Localization

`en_us` and Spanish-from-Spain `es_es` are first-class player-facing locales. CI enforces exact key parity and non-empty Spanish values. Gravity Charge is **«Carga de gravedad»** in `es_es`.

## Ownership boundaries

- **Minecraft vanilla**: ShulkerBullet entity type, projectile collision/Levitation, projectile renderer/resources, Target Block hit/redstone semantics and shulker-duplication mechanics; high-level mob goal intent and ordinary pathfinding semantics.
- **Clinging: Reoriented**: Gravity Charge item/capture/acquisition/cardinal routing plus voluntary gravity policy, camera HOLD/full-sphere Gravity Fall, local-player landing contact intent/commitment/recovery, persistent body attitude, posture-driven aerodynamics, safety, impact, mace, owned mount transitions and bounded gravity-aware mob locomotion. The player-facing graze heuristic is layered after shared landing geometry and does not alter mob planning physics. When Alchemical Leather is present, Clinging also owns publication of successful-turn and controlled-Reorientation semantic facts.
- **Gravity Changer**: physical gravity attributes, coordinate transforms and general movement/collision behaviour; directional `PathNavigation` semantics and presentation for foreign gravity changes.
- **Vanilla / registered landing providers**: collision/support facts subject to shared fluid fencing.
- **First Person**: camera/model baseline; Clinging may transform its body pass but not the real camera.
- **Fresh Animations/EMF/ETF**: internal model animation; Clinging owns only macro root orientation while active.
- **Scale Brews**: Scale-specific size/mount/anatomical mechanics.
- **Alchemical Leather**: infusion storage/source arbitration, armor ownership, wear-rule interpretation, fractional accounting and durability application.

No required or optional dependency JAR is bundled in the production artifact.
