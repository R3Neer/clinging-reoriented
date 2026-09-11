# Clinging: Reoriented hardening analysis

Status: **TEMPORARY — DELETE ON SUCCESS**.

## 1. Current-state classification

### Keep and harden in Clinging

- `ClingingReoriented`: voluntary selection, player ownership/reconciliation and retirement policy.
- `AirChanges`: airborne charge semantics.
- `MobGravity` / `MountedGravity`: effect/rider gravity policy, but ownership and recovery need rework.
- `GravityInput`, `LookDirection`, `GravityBreadcrumbs`, brewing/beacon behavior: retain unless a regression is found.
- Client input/reply/state protocol for Clinging turn intent: retain.

### Rework now

- `ClingingReoriented.retire`: replace remote `lastSafeDown` and full-column search with bounded local recovery + pending retry.
- `PlayerData`: remove remote-safe-position semantics; keep only state needed by the bounded lifecycle.
- `MobGravity.State`: replace `effectSeen` as an ownership proxy with explicit ownership/borrow semantics and prior-frame restoration where required.
- `MobGravity.restore`: make candidate placement atomic for the complete passenger hierarchy.
- `CameraDurationMixin`: stop globally replacing Gravity Changer timing; gate by a Clinging-owned visual transition lifecycle.
- `FirstPersonOffsetMixin`: use the same visual ownership boundary instead of acting as a global Gravity Changer compatibility patch.
- `MovingSurface`/`MoveReference`: add minimal causal anti-replay hardening only.

### Move to Scale Brews, with compatibility overlap

- `ScaleFlightGravityMixin`
- `ScaleWolfGravityMixin`
- `ScaleChickenGravityMixin`

These express Tiny Mount behavior under non-DOWN gravity and therefore belong to Scale Brews' mount subsystem. Clinging retains old-version shims only until Scale exposes a native capability marker and a released version containing it is available.

### Delete later at Scale G7

Subject to the final Scale public API and successful consumer tests, Clinging should lose ownership of responsibilities represented by `MovingSurface`, `EntityCollisionMixin`, `OutgoingMoveMixin`, Scale-specific movement baselines/reconciliation, `ScalePhysicsMixin`, and most of the current Scale/Anatomy bridges. Exact symbol deletion is decided against the final API, not precommitted by name.

## 2. Design decisions already accepted

1. Player forced retirement is local-only (≤4 blocks), otherwise pending/retry.
2. Gravity ownership is explicit and external gravity is preserved.
3. Camera timing and First Person correction are local to Clinging-owned visual transitions.
4. Tiny Mount arbitrary-gravity behavior belongs in Scale Brews, outside the entity-collision solver.
5. Scale Brews G7 is the final owner of shared entity-surface physics/reconciliation.

## 3. Coordination problem with Scale Brews

Scale `chatgpt-editing` is currently executing the mandatory S00 foundation audit. Its target architecture already contains a single-owner gravity adapter under the collision work, but Tiny Mounts also needs the same effective gravity frame. Creating a second mount-only gravity registry would produce two authorities.

The desired long-term topology is:

```text
Gravity Changer
      ↓ optional Scale integration
Scale gravity-frame service
      ├── collision system
      └── Tiny Mounts

Clinging
      ├── owns input/effect/charge/policy
      └── consumes Scale public collision API at G7
```

Scale must remain loadable without Gravity Changer. The integration therefore must be optional/gated, not a required dependency.

## 4. Release compatibility consequence

A new Clinging prerelease cannot assume an unreleased Scale main build. Therefore Clinging should keep beta.5-compatible Tiny Mount shims while detecting a future native Scale capability to avoid double handling. Once a Scale prerelease containing native gravity-aware Tiny Mounts exists and is validated, a later Clinging cleanup may remove those shims entirely.

## 5. Iterative convergence record

- **Pass 1:** split fixes into player recovery, mob ownership, passenger atomicity, visual ownership, moving-surface networking and Scale migration.
- **Pass 2 change:** rejected building a full new Clinging movement-reference protocol because Scale G4/G7 already owns the permanent solution; reduced this to bounded anti-replay hardening.
- **Pass 3 change:** separated Tiny Mount gravity from entity collisions; it belongs to Scale `mount`, not `collision`.
- **Pass 4 change:** added a native-capability/shim overlap because Clinging prerelease must remain compatible with released Scale beta.5 even after Scale main receives the new behavior.
- **Pass 5 change:** identified the need for one transversal Scale gravity-frame authority so future collision and Tiny Mount code do not register competing providers.
- **Pass 6:** full requirements/ownership/dependency review produced no further changes. Plan considered converged for implementation preparation.
