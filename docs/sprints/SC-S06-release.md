# SC-S06 — Canonización, integración y release

Estado: **EN CURSO — GATE ADVERSARIAL POST-MERGE VERDE; CANONIZACIÓN PREPARADA**.

`v0.1.0-alpha.14` fue publicado desde `main` durante esta campaña con cambios de Gravity Fall, fluidos, seguridad y mace. Shulker Charge pasa por tanto a **0.1.0-alpha.15** y se valida sobre la combinación real con ese `main`, no sobre la antigua base alpha.13.

## Entrada a S06

El gate post-merge exigido por este sprint quedó verde en run **#714** (`34897063938`) sobre `a16d16faaa2cbd3c4b51678f08310ebc9a8bf681`: build/JUnit, server GameTests, cliente por defecto, First Person, Scale Brews servidor/cliente, Fresh Animations y snapshots.

## Canonización

La preparación de alpha.15 debe salir como un único commit de release-prep que:

- migre Shulker Charge a README, GUIDE, ARCHITECTURE, COMPATIBILITY, CONFIGURATION, VALIDATION, CHANGELOG y notices;
- registre el origen/licencia del arte 2D/3D definitivo;
- retire `docs/work/SHULKER_CHARGE_*` porque SPEC/PLAN/WORKFLOW dejan de ser autoridad temporal;
- actualice `mod_version` a `0.1.0-alpha.15`;
- sustituya el workflow one-shot de alpha.14 por uno de alpha.15 que publique exclusivamente el artefacto del `main` validado.

Los assets 2D/3D de Shulker Charge presentes en la rama son definitivos. El icono y la geometría son assets originales GPL-3.0-or-later del proyecto. El modelo 3D referencia en runtime `minecraft:entity/shulker/spark` sin redistribuir esa textura de Mojang.

## Gate de salida

Después del commit de canonización:

1. la rama `feature/shulker-charge` debe pasar otra vez la matriz completa sobre ese HEAD exacto;
2. sólo entonces se integra en `main`;
3. `main` debe repetir la misma matriz completa;
4. el workflow `release-alpha15.yml` debe descargar los JAR del run exitoso de `main`, verificar que existe exactamente un JAR normal y uno sources para alpha.15, registrar SHA-256 y crear `v0.1.0-alpha.15` como prerelease contra ese mismo commit;
5. no se permite una segunda compilación para publicar.

El cierre histórico del sprint se registra después de confirmar el release; hasta entonces este documento no finge haber terminado trabajo que GitHub todavía no ha ejecutado.
