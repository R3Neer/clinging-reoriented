# SC-S02 — Lanzamiento, dispenser y targeting

Estado: **CERRADO / GATE VERDE**.

## Scope cerrado

SC-020..054. S02 crea la Charge lanzada, conserva intención original, integra dispenser y demuestra adquisición/readquisición. Navegación completa hacia Target Blocks, impacto/redstone, daño, duplicación y recaptura end-to-end quedan en S03.

## Implementación cerrada

Cabeza de evidencia: `9f929af3a7267c5ceaaf4f7a36842ffe80d13297`.

- `ShulkerChargeItem` implementa `ProjectileItem`; jugador y dispenser crean `EntityTypes.SHULKER_BULLET` exacto.
- Uso manual conserva owner jugador, consume una unidad y usa component cooldown de 0,5 s.
- Dispenser crea la misma bullet ownerless y conserva su facing como intención.
- `ShulkerChargeBullet` es sólo una subclase de creación que evita que `ProjectileDispenseBehavior` pise nuestro routing con `shoot`; no registra otro EntityType.
- `ShulkerBulletMixin` persiste identidad de Charge, intención y Target Block y mantiene `noGravity` mientras es Charge lanzada.
- Entity targets reutilizan `finalTarget` y `selectNextMoveDirection` vanilla.
- Sin target se conserva vuelo cardinal según la intención original.
- Targeting acotado: 32 bloques, cono 15°, score angular primero/distancia después, LOS en cada adquisición nueva, owner excluido.
- Target Block alcanzado por ray central tiene prioridad absoluta; la búsqueda asistida de bloques usa sólo 24 raycasts fijos alrededor del ray de intención, sin scan volumétrico/global.
- Un target válido no se reemplaza. Al desaparecer/morir/ser removido o retirarse un Target Block, se permite readquisición; perder LOS después de lock no lo invalida.
- Estado extra e intención sobreviven save/load.

## Incidencia y clasificación

La primera implementación `0e88cd3042cd9f40753ad1f7d8eaca5b494617bf`, CI #478, falló antes de tests por dos errores de compilación: colocación de `@Nullable` sobre `Direction.Axis` y comparación imposible `LivingEntity != ShulkerBullet` en un predicate. Se clasificó como **implementación/API**. `9f929af` corrigió exclusivamente esas firmas/filtro sin cambiar arquitectura ni algoritmo.

## Tests y evidencia

CI **#479**, run `34854352197`, sobre `9f929af`: **success** completo.

Pasaron:

- kernel unitario: target alineado lejano vence cercano off-axis; fuera de cono rechazado; intent malformado falla cerrado;
- manual launch: consumo exacto, cooldown component, owner, type vanilla y no-gravity;
- factory de dispenser: type exacto, ownerless, facing→intent y no-gravity;
- entity alignment ranking real;
- Target Block central con prioridad sobre entidad alineada;
- oclusión inicial de entidad;
- lock retenido tras perder LOS;
- readquisición tras removal de entidad;
- readquisición tras retirar Target Block;
- persistencia de identidad/intención;
- toda la matriz histórica: client vanilla, First Person, Scale Brews server/client, Fresh Animations y snapshot validator.

## Gate

S02 cerrado. S03 parte de `9f929af` y no debe reabrir la política de adquisición salvo regresión demostrable.