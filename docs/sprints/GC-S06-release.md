# GC-S06 — Canonización, integración y release

Estado: **EN CURSO — GRAVITY CHARGE MARCA LA ENTRADA EN BETA**.

La línea alpha queda cerrada con **0.1.0-alpha.15**, dedicada a pet gravity-breadcrumb pursuit. Gravity Charge pasa deliberadamente a **0.1.0-beta.1** y debe validarse sobre el `main` exacto posterior a alpha.15, no sobre una base alpha anterior.

## Entrada a S06

El gate adversarial de la feature quedó verde antes de la promoción de versión:

- run **#710** (`34895769482`): matriz completa verde tras el hardening del corredor de vuelo;
- run **#714** (`34897063938`) sobre `a16d16faaa2cbd3c4b51678f08310ebc9a8bf681`: matriz completa post-alpha.14 verde, incluidas Scale Brews servidor/cliente, First Person, Fresh Animations y snapshots.

Alpha.15 fue publicada desde `main` en `fe74b979179065afa505baa4b2ece75bebc9f4d2`. La entrada en beta obliga a repetir la matriz completa sobre esa combinación real.

## Canonización beta.1

El release-prep de beta.1 debe:

- usar **Gravity Charge** como nombre externo y `clinging_reoriented:gravity_charge` / `GravityCharge*` como nombres internos;
- migrar la feature a README, GUIDE, ARCHITECTURE, COMPATIBILITY, CONFIGURATION, VALIDATION, CHANGELOG y notices;
- migrar los sprint docs `SC-*` a `GC-*` y el registro de arte a `docs/art/gravity-charge/`;
- declarar explícitamente que Gravity Charge es la feature que marca la transición alpha → beta;
- mantener `en_us` y `es_es` en paridad exacta, con `Gravity Charge` / `Carga de gravedad`, y hacer que CI falle si las claves divergen;
- mantener retirados los documentos temporales de trabajo, ya sin autoridad normativa;
- actualizar `mod_version` a `0.1.0-beta.1`;
- sustituir el publisher one-shot del último alpha por `release-beta1.yml`, manteniendo la política exact-artifact.

Los assets 2D y geometría 3D de Gravity Charge son originales GPL-3.0-or-later del proyecto y usan la textura propia `clinging_reoriented:item/gravity_charge`. La entidad lanzada conserva el renderer vanilla únicamente porque sigue siendo `minecraft:shulker_bullet`; no se redistribuyen assets de Mojang.

## Qué significa entrar en beta

Beta no relaja los gates. Significa que, con Gravity Charge integrada, el núcleo de gravedad, cámara, landing, lifecycle, compatibilidad y gameplay principal se considera suficientemente coherente para una fase de validación más amplia.

Sigue siendo prerelease: pueden aparecer bugs, tuning y compatibilidad adicional. No se declara feature-freeze ni estabilidad absoluta.

## Gate de salida

1. Sincronizar la rama de Gravity Charge con el `main` exacto que publicó alpha.15.
2. Resolver la combinación manteniendo la historia alpha.15 intacta y beta.1 reservada a Gravity Charge.
3. Ejecutar la matriz completa sobre el HEAD beta.1 de la rama, incluyendo paridad EN/ES y snapshots `gravity-charge-*`.
4. Integrar en `main` sólo si esa matriz queda verde.
5. Ejecutar otra vez la matriz completa sobre el `main` exacto integrado.
6. `release-beta1.yml` debe descargar los JAR de ese run exitoso, verificar exactamente un JAR normal y uno sources, registrar SHA-256 y crear `v0.1.0-beta.1` contra ese mismo commit.
7. No se permite una segunda compilación para publicar.

El cierre histórico del sprint se registra sólo después de confirmar la prerelease beta.1.
