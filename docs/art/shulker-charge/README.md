# Shulker Charge artwork

## Approved 2D icon

Original 16x16 RGBA pixel artwork, GPL-3.0-or-later under the project licence. The user approved this icon on 2026-09-14.

- Texture: `assets/clinging_reoriented/textures/item/shulker_charge.png`.
- GUI model: `clinging_reoriented:item/shulker_charge_gui`.
- Editable source: `docs/art/shulker-charge/shulker-charge.piskel`.
- Explicit pixel/palette source: `docs/art/shulker-charge/pixels.json`.

Design: warm ivory orthogonal projectile, with muted mauve side shadows and grouped highlights. References inspected in the Minecraft 26.2 client included wind charge, fire charge, ender pearl and shulker projectile presentation. The sprite is drawn independently, not a recolour or resized copy of a vanilla icon. Vanilla reference images are not distributed here.

The icon was checked at 16x16 with binary alpha, one connected silhouette and nearest-neighbour previews against vanilla charge/pearl icon scale.

## Approved 3D held model

The integrated held/ground presentation is project-authored geometry derived from the same orthogonal shulker-projectile language, while GUI contexts keep the dedicated 2D icon. The definitive runtime model uses the project-owned texture `clinging_reoriented:item/shulker_charge`; it does not sample or redistribute Minecraft's shulker spark texture.

The launched projectile remains the exact vanilla `minecraft:shulker_bullet`, so Minecraft's own renderer/model/texture are naturally used for that entity at runtime and remain Mojang material. No vanilla reference image or texture is copied into this repository. See `THIRD_PARTY_NOTICES.md`.

## Integration status

As of 0.1.0-alpha.16 the 2D GUI icon and 3D held model are integrated and covered by semantic client snapshots for inventory, first-person held, third-person held, projectile renderer and fixed 3D presentation. These files remain the editable provenance record for future revisions rather than temporary implementation instructions.
