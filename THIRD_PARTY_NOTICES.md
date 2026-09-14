# Third-party projects

Clinging: Reoriented interoperates with the projects below. Their code and assets are not redistributed in this repository or embedded in the production JAR unless explicitly stated otherwise.

- [Minecraft](https://www.minecraft.net/) by Mojang Studios.
- [Fabric Loader and Fabric API](https://fabricmc.net/) by the Fabric project.
- [Alex's Mobs Continued](https://modrinth.com/mod/alexs-mobs-continued), which supplies the Clinging effect and potion used by this mod.
- [Gravity Changer Unofficial Port](https://modrinth.com/mod/gravity-changer-unofficial-port), which supplies cardinal-gravity physics and camera transforms.
- [CodxLib](https://modrinth.com/mod/codxlib) and [Cloth Config API](https://modrinth.com/mod/cloth-config), required by the runtime dependency stack.
- [Alchemical Leather](https://github.com/R3Neer/alchemical-leather), [Scale Brews](https://github.com/R3Neer/scale-brews), First Person and Scale Visual Compat, which are optional integrations.

## Shulker Charge artwork and vanilla references

The Shulker Charge 2D inventory icon, its texture and the project-authored 3D item model geometry are original Clinging: Reoriented assets and are licensed **GPL-3.0-or-later** with the project. Editable source for the icon is retained under `docs/art/shulker-charge/`.

The launched Charge intentionally remains the exact vanilla `minecraft:shulker_bullet` entity type, so Minecraft's ShulkerBullet renderer/model/texture are used by Minecraft itself at runtime. Those Mojang assets and the vanilla reference images inspected during design are **not redistributed** by this repository.

All names and trademarks belong to their respective owners. Refer to each project's distribution for its license and terms.
