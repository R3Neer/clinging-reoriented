# SC-S06 — Canonización, integración y release

Estado: **EN CURSO — S05 VERDE; CANONIZACIÓN RECONCILIÁNDOSE CON MAIN**.

Durante esta campaña se publicaron `v0.1.0-alpha.14` (Gravity Fall/control/compatibilidad) y después `v0.1.0-alpha.15` (pet gravity-breadcrumb pursuit). Shulker Charge pasa por tanto a **0.1.0-alpha.16** y se valida sobre la combinación real con ambas releases, no sobre la antigua base alpha.13.

## Entrada a S06

El gate post-merge exigido por este sprint quedó verde en run **#714** (`34897063938`) sobre `a16d16faaa2cbd3c4b51678f08310ebc9a8bf681`: build/JUnit, **121/121 server GameTests**, cliente por defecto, First Person, Scale Brews servidor/cliente, Fresh Animations y snapshots. El artefacto exacto fue `10369304504`, digest `sha256:f0220df2edb45d076302d9849c10e05e662aacd6319d92135720d68e9a0f3869`.

## Canonización

La preparación de alpha.16 debe:

- migrar Shulker Charge a README, GUIDE, ARCHITECTURE, COMPATIBILITY, CONFIGURATION, VALIDATION, CHANGELOG y notices;
- preservar alpha.15 como la release histórica de pet pursuit ya publicada;
- registrar correctamente el origen/licencia del arte 2D/3D definitivo;
- retirar `docs/work/SHULKER_CHARGE_*` porque SPEC/PLAN/WORKFLOW dejan de ser autoridad temporal;
- actualizar `mod_version` a `0.1.0-alpha.16`;
- sustituir el publisher ya consumido de alpha.15 por uno de alpha.16 que publique exclusivamente el artefacto del `main` validado.

Los assets 2D/3D de Shulker Charge presentes en la rama son definitivos. Icono, textura y geometría 3D son assets originales GPL-3.0-or-later del proyecto. La entidad lanzada conserva el renderer/modelo/textura vanilla porque sigue siendo la `SHULKER_BULLET` exacta; esos assets de Minecraft no se copian al repositorio.

## Gate de salida

Después de reconciliar la canonización con el `main` actual:

1. `feature/shulker-charge` debe pasar otra vez la matriz completa sobre ese HEAD exacto;
2. sólo entonces se integra en `main`;
3. `main` debe repetir la misma matriz completa;
4. `release-alpha16.yml` debe descargar los JAR del run exitoso de `main`, verificar que existe exactamente un JAR normal y uno sources para alpha.16, registrar SHA-256 y crear `v0.1.0-alpha.16` como prerelease contra ese mismo commit;
5. no se permite una segunda compilación para publicar.

El sprint se marca cerrado sólo después de confirmar tag, assets y hashes de la prerelease.
