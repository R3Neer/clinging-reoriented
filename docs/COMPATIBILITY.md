# Compatibility

## Required projects

| Project | Tested version | Role |
|---|---:|---|
| Alex's Mobs Continued | 2.1.9 | Supplies Clinging and its potion registry. |
| CodxLib | 1.5.1 | Runtime dependency of Alex's Mobs Continued. |
| Gravity Changer Unofficial Port | 1.5.2-beta.5-mc26.2 | Gravity attributes, coordinate transforms, movement/collision physics. |
| Cloth Config API | 26.2.155 | Runtime dependency used by the gravity stack. |

Fabric API 0.159.0+26.2, Fabric Loader 0.19.5 and Java 25 are also required for Minecraft 26.2. Client and server need matching Clinging: Reoriented versions.

## Shulker Charge and vanilla projectile semantics

Shulker Charge adds **no new dependency**. A launched Charge keeps the exact vanilla `minecraft:shulker_bullet` entity type. The project layers Charge state and routing onto that entity rather than registering a parallel projectile type, so vanilla projectile impact, levitation and shulker-duplication checks continue to observe the expected type.

The inventory icon is original GPL-3.0-or-later project artwork. The 3D held model references Minecraft's `entity/shulker/spark` texture at runtime; that Mojang texture is not copied into or redistributed by this repository.

Reorientation brewing now uses Shulker Charge instead of Shulker Shell. Alex's Mobs Continued remains the source of the Clinging potion/effect used as the base input.

## First Person

The supported optional target is **First Person 2.7.2** with **Not Enough Animations 1.12.4**.

The local avatar macro root uses a blended body-center/camera pivot during Gravity Fall, preventing steep look-down clipping without solving the problem by hiding the body. That root never feeds back into the actual camera.

The pinned lane covers ordinary gravity ownership, sustained Gravity Fall, BODY_LANDING and steep look-down checkpoints.

## Fresh Animations / Player Extension / EMF / ETF

The reproducible optional lane uses:

- Fresh Animations 1.10.5
- Fresh Animations Player Extension 1.1
- Entity Model Features 3.3.5
- Entity Texture Features 7.2

Fixtures are checksum-pinned in CI. These mods retain ownership of limbs, head tracking, equipment and internal animation. Clinging applies only the macroscopic Gravity Fall root around that result, including sustained velocity alignment, bounded look-follow and 500 ms BODY_LANDING.

The projects remain optional and are not bundled.

## Scale Brews

Scale Brews is **not a required dependency**. Production compile classpath remains Scale-free. CI loads public **beta.5** only in isolated optional server/client lanes.

Existing transitional Scale/Anatomy reflection and pseudo-mixin bridges remain fail-closed. Tiny Mounts are ordinary compatible living root vehicles to Clinging's mounted-gravity code; Scale owns movement mechanics that Scale itself creates.

`LandingSurfaceProvider` deliberately contains no Scale Brews types. A concrete Scale-specific landing-surface adapter belongs in a consumer/integration layer rather than making the base API depend on Scale internals.

Shulker Charge likewise contains no Scale-specific production path. Its targeting and routing operate on ordinary Minecraft entity/block contracts, and the adversarial compatibility lane verifies that Scale's optional runtime does not change Charge acquisition/retry semantics.

## Alchemical Leather

Alchemical Leather remains optional. Compatible equipment may supply Clinging/Reorientation effects; Clinging owns gravity selection, camera/landing, Gravity Fall, aerodynamics, impact and mace semantics once those effects are active.

## Fluids from other mods

Fluid handling is deliberately generic. `FluidContext` inspects non-empty `FluidState` volumes rather than special-casing vanilla water/lava, so modded fluids suspend Clinging support/landing/Gravity Fall in the same way without per-mod integration.

## Jump-strength modifiers

Sprint-landing reservation reads effective `JUMP_STRENGTH` plus vanilla Jump Boost power. Compatible modifiers influence only the bounded one-to-three-tick sprint-jump reservation.

## Ownership boundaries

- **Minecraft vanilla**: ShulkerBullet entity type, projectile collision/levitation, Target Block hit/redstone semantics and shulker-duplication mechanics.
- **Gravity Changer**: physical gravity attributes, coordinate transforms and general movement/collision behaviour; presentation for changes not owned by Clinging.
- **Clinging: Reoriented**: voluntary turn policy, Clinging budget/Reorientation, Shulker Charge capture/acquisition/cardinal routing, camera HOLD/full-sphere Gravity Fall look, 500 ms landing commitment, macro body root, bounded air-diving/aerodynamics, flight safety, impact lifecycle, directional mace accounting, owned mount/pet transitions and lifecycle fencing.
- **Vanilla / registered landing providers**: collision/support facts, subject to the shared fluid-context fence. Providers cannot choose input policy, camera ownership or placement.
- **First Person**: camera/model baseline. Clinging may transform its body pass but cannot transform the real camera.
- **Fresh Animations/EMF/ETF**: optional internal model animation. Clinging owns only macro root orientation while active.
- **Scale Brews**: Scale-specific size/mount/anatomical mechanics and any future adapter it chooses to provide.
- **Alchemical Leather**: equipment-supplied effects.

A foreign gravity write remains foreign. No required or optional dependency JAR is bundled in the production artifact.
