# SC-S02 — Lanzamiento, dispenser y targeting

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN**.

## Scope

SC-020..054. S02 crea y clasifica la Charge lanzada, conserva la intención original, adquiere/readquiere targets y demuestra consumo/cooldown/dispenser. S03 cerrará navegación completa hacia bloques, impacto/redstone, daño, duplicación y recaptura end-to-end.

## Investigación exacta 26.2

- `ShulkerBullet` guarda `finalTarget`, `currentMoveDirection`, `flightSteps` y sus tres target deltas; `selectNextMoveDirection(axis,target)` ya implementa el lenguaje ortogonal para entidades.
- Sin target vanilla aplica gravedad. Una Charge lanzada debe usar `noGravity` para conservar vuelo libre mientras busca.
- `ProjectileDispenseBehavior` llama a `Projectile.spawnProjectileUsingShoot(...); projectile.shoot(...)`, por lo que preconfigurar sólo `deltaMovement` en `asProjectile` no basta.
- Una subclase Java mínima de `ShulkerBullet` puede ignorar ese `shoot` inicial manteniendo `EntityTypes.SHULKER_BULLET` exacto. Tras save/load puede reaparecer como `ShulkerBullet` base porque la semántica posterior se persiste y vive en mixin.
- `Direction.getApproximateNearest` proporciona la dirección cardinal dominante para vuelo libre.

## Plan de implementación

1. `ShulkerChargeProjectile` expone un duck mínimo: initialize, launched flag, intent, target entity/block y forceAcquire para tests.
2. `ShulkerChargeBullet extends ShulkerBullet` sólo protege el lanzamiento de dispenser contra el `shoot` genérico que pisaría nuestro routing; no registra nuevo EntityType.
3. `ShulkerBulletMixin` sombrea el target/routing vanilla, persiste `launchedCharge`, intención y Target Block, y mantiene `noGravity` para Charges lanzadas.
4. Al inicializar se normaliza/falla cerrado la intención, se intenta adquisición y se configura:
   - entidad → `EntityReference.of` + `selectNextMoveDirection` vanilla;
   - Target Block → BlockPos persistido + dirección cardinal inicial hacia su centro;
   - nada → cardinal dominante de la intención.
5. Mientras exista target entidad vivo/no spectator o Target Block cargado y aún `minecraft:target`, no se busca sustituto.
6. Si deja de ser válido, se readquiere como máximo cada 4 ticks. Ocultarse tras una pared no invalida un lock existente.
7. `ShulkerChargeTargeting` mantiene el ranking separado de la entidad:
   - rango 32;
   - cono 15°;
   - Target Block directamente alcanzado por el ray central gana inmediatamente;
   - entidades vivas dentro del cono requieren LOS en cada adquisición;
   - targets asistidos compiten por error angular y luego distancia;
   - Target Blocks asistidos se descubren mediante un fan fijo de 24 raycasts alrededor del ray central, nunca por scan global/volumétrico.
8. `ShulkerChargeItem` implementa `ProjectileItem` y crea la misma bullet marcada tanto desde jugador como dispenser. Uso manual consume uno, atribuye owner y conserva el component cooldown 0,5 s.
9. `ShulkerCharges.initialize` registra `DispenserBlock.registerProjectileBehavior(ITEM)` una sola vez.

## Modelo adversarial previo

- target más cercano pero fuera de línea no debe ganar a uno más lejano bajo crosshair;
- Target Block central detrás de bloque opaco no debe descubrirse;
- owner nunca se auto-lockea;
- target muerto/removido debe soltar lock; pérdida de LOS sola no;
- target block reemplazado debe soltar lock sin cargar chunks;
- save/load no debe convertir una Charge lanzada en bullet natural;
- dispenser no debe sobrescribir la velocidad inicial con `shoot` genérico;
- sin target no debe caer ni quedarse quieta;
- múltiples scans no deben reasignar un target todavía válido.

## Tests de cierre

- kernel de score: alineado lejano vence cercano off-axis, fuera de cono rechazado;
- uso manual: mismo tipo vanilla, owner, marca, consumo y cooldown component;
- dispenser/asProjectile: mismo tipo exacto, ownerless, intención de facing y `noGravity`;
- no-target: vuelo cardinal hacia intent;
- entidad alineada adquirida; entity off-axis/ocluida rechazada;
- Target Block ray central prioritario;
- target válido retenido aunque pierda LOS;
- muerte/removal y Target Block retirado permiten readquisición;
- persistencia del estado extra.

S02 no se cierra hasta CI completa verde sobre el HEAD exacto y revisión del diff de producción/tests.