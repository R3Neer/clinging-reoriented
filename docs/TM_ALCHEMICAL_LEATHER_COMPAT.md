# TM: Alchemical Leather compatibility ownership

Status: **temporary implementation specification** for branch `chatgpt/alchemical-leather-wear-compat`.

This sprint migrates first-party Alchemical Leather compatibility for Clinging/Reorientation into Clinging Reoriented itself while preserving complete standalone behavior when Alchemical Leather is absent.

## Frozen requirements

- Clinging Reoriented owns the Alchemical Leather slot declaration for `clinging_reoriented:reorientation`.
- Reorientation remains a boots effect.
- Clinging itself remains an Alex's Mobs effect; Clinging Reoriented may publish semantic successful-turn information for it but does not redefine Alex's Mobs registry ownership.
- Optional Alchemical Leather linkage must be isolated so the mod launches and behaves identically without Alchemical Leather.
- Publish a semantic event only after a successful gravity turn; failed, blocked, unchanged, no-space or ambiguous attempts publish nothing.
- Publish controlled Reorientation flight work only while Reorientation itself owns active self locomotion; passive mounts, moving platforms/Living Platforms, pistons/flying machines and foreign transport do not count.
- A successful Reorientation-requested turn of a compatible airborne mount publishes the discrete turn event but ordinary mounted travel publishes no continuous flight work.
- The event publisher does not select armor, mutate durability or know Alchemical Leather balance values.
- Clinging wear semantics are discrete successful gravity-link events only.
- Reorientation wear semantics are controlled self-flight time plus successful turns; balance remains data-owned by the Alchemical Leather compatibility rule.
- Existing gameplay, gravity ownership, visual transitions, landing, safety and movement behavior must remain unchanged when compatibility is absent or when no infused armor is involved.

## Iterative TM phases

1. Add optional compatibility resource ownership and linkage-safe adapter.
2. Insert semantic events at already-authoritative success points only.
3. Add unit/GameTests proving success-only publication and no publication from passive transport/failure paths.
4. Adversarial review for mounted turns, moving supports, lifecycle transitions and missing Alchemical Leather.
5. Update README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION after behavior converges.
