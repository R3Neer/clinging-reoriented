# GC-S02 — Lanzamiento, dispenser y targeting

Estado: **CERRADO / GATE VERDE**.

> Nota de canonización beta.1: la feature se publicó finalmente como **Gravity Charge**; clases, ID, assets y tests se renombraron antes de beta.1.

## Scope cerrado

GC-020..054. S02 crea la Gravity Charge lanzada, conserva intención original, integra dispenser y demuestra adquisición/readquisición. Navegación completa hacia Target Blocks, impacto/redstone, daño, duplicación y recaptura end-to-end quedan en S03.

## Implementación cerrada

Cabeza de evidencia histórica: `9f929af3a7267c5ceaaf4f7a36842ffe80d13297`.

- `GravityChargeItem` implementa `ProjectileItem`; jugador y dispenser crean `EntityTypes.SHULKER_BULLET` exacto.
- Uso manual conserva owner jugador, consume una unidad y usa component cooldown de 0,5 s.
- Dispenser crea la misma bullet ownerless y conserva su facing como intención.
- `GravityChargeBullet` es sólo una subclase de creación que evita que `ProjectileDispenseBehavior` pise nuestro routing con `shoot`; no registra otro EntityType.
- `ShulkerBulletMixin` persiste identidad de Gravity Charge, intención y Target Block y mantiene `noGravity` mientras es Gravity Charge lanzada.
- Entity targets reutilizan `finalTarget` y `selectNextMoveDirection` vanilla.
- Sin target se conserva vuelo cardinal según la intención original.
- Targeting acotado: 32 bloques, cono 15°, score angular primero/distancia después, LOS en cada adquisición nueva, owner excluido.
- Target Block alcanzado por ray central tiene prioridad absoluta; la búsqueda asistida de bloques usa sólo 24 raycasts fijos alrededor del ray de intención, sin scan volumétrico/global.
- Un target válido no se reemplaza. Al desaparecer/morir/ser removido o retirarse un Target Block, se permite readquisición; perder LOS después de lock no lo invalida.
- Estado extra e intención sobreviven save/load.

## Incidencia y clasificación

La primera implementación histórica, CI #478, falló antes de tests por dos errores de compilación: colocación de `@Nullable` sobre `Direction.Axis` y comparación imposible en un predicate. Se clasificó como **implementación/API** y se corrigieron exclusivamente firmas/filtro sin cambiar arquitectura ni algoritmo.

## Tests y evidencia

CI histórica **#479**, run `34854352197`: **success** completo. Cubrió kernel unitario, uso manual, factory de dispenser, ranking real, prioridad de Target Block, oclusión inicial, lock retenido tras perder LOS, readquisición tras removal, persistencia y toda la matriz opcional.

La promoción beta.1 vuelve a ejecutar esas pruebas tras el renombre completo a `GravityCharge*` y `clinging_reoriented:gravity_charge`.

## Gate

S02 cerrado. S03 no debe reabrir la política de adquisición salvo regresión demostrable.
