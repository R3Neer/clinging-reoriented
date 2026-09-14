# Gravity Charge artwork

## Approved 2D icon

Original 16x16 RGBA pixel artwork, GPL-3.0-or-later under the project licence. The user approved the visual design on 2026-09-14 while the feature still had its provisional development name; beta.1 canonizes it as **Gravity Charge** without changing the approved artwork.

- Texture: `assets/clinging_reoriented/textures/item/gravity_charge.png`.
- GUI model: `clinging_reoriented:item/gravity_charge_gui`.
- Editable source: `docs/art/gravity-charge/gravity-charge.piskel`.
- Explicit pixel/palette source: `docs/art/gravity-charge/pixels.json`.

Design: warm ivory orthogonal projectile, with muted mauve side shadows and grouped highlights. Minecraft charge/pearl/shulker presentation was inspected as reference, but the sprite is independently drawn and is not a recolour or resized copy of a vanilla icon. Vanilla reference images are not distributed here.

The icon was checked at 16x16 with binary alpha, one connected silhouette and nearest-neighbour previews against vanilla item scale.

## Approved 3D held model

The integrated held/ground presentation is project-authored geometry derived from the same orthogonal projectile language. GUI contexts keep the dedicated 2D icon. The final 3D item model uses the project's own `clinging_reoriented:item/gravity_charge` texture.

The **launched entity** remains vanilla `minecraft:shulker_bullet`, so its entity renderer naturally uses Minecraft's own runtime resources. Those Mojang assets are not copied into this repository. See `THIRD_PARTY_NOTICES.md`.

## Integration status

As of **0.1.0-beta.1**, the release that introduces Gravity Charge and marks the project's entry into beta, the 2D GUI icon and 3D held model are integrated and covered by semantic client snapshots for inventory, first-person held, third-person held, projectile renderer and fixed 3D presentation.

These files are the editable provenance record for future revisions rather than temporary implementation instructions.
