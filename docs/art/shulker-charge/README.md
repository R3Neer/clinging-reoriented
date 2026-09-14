# Shulker Charge: approved 2D icon

Original 16x16 RGBA pixel artwork, GPL-3.0-or-later under the project licence. The user approved this icon on 2026-09-14. It is ready for GUI integration; the required 3D hand renderer remains a separate integration task.

Texture: `assets/clinging_reoriented/textures/item/shulker_charge.png`.
GUI model: `clinging_reoriented:item/shulker_charge_gui`.
Editable source: `docs/art/shulker-charge/shulker-charge.piskel` and explicit pixel/palette data in `pixels.json`.

Design: warm ivory orthogonal projectile, with muted mauve side shadows and grouped highlights. References inspected in the Minecraft 26.2 client: wind_charge, fire_charge, ender_pearl and entity/shulker/spark textures. The actual ShulkerBulletModel is three intersecting slabs (8x8x2, 2x8x8, 8x2x8), informing the cross-like silhouette. The new sprite is drawn independently, not a recolour or resized copy of a vanilla icon. Vanilla reference images are not distributed here.

At the inspected branch commit 8f79fbd, the shulker feature is specified but not registered. Integration should select this generated model in GUI context and retain/adapt the vanilla projectile model in hand as SC-002 requires. No gameplay registration or universal flat item definition is added by this artwork delivery.

Checked: 16x16, binary alpha, one connected silhouette, nearest-neighbour previews against vanilla charge/pearl icons. Visual design approved by the user. In-game presentation remains pending.
