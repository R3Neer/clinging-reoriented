# SC-S00 — Investigación y arquitectura

Estado: **CERRADO / GATE VERDE**.

## Scope

SC-001..103 a nivel arquitectónico. No se implementa gameplay en S00.

## Estado real auditado — Minecraft/Fabric 26.2

- `ShulkerBullet` sigue siendo `EntityTypes.SHULKER_BULLET`, conserva target por `EntityReference`, movimiento en tramos cardinales, steering a velocidad objetivo 0.15, 4 de daño + 200 ticks de Levitation, `onHitBlock` real y destrucción tras impacto.
- `Shulker.hurtServer` activa `hitByShulkerBullet()` sólo cuando el `directEntity` es literalmente `EntityTypes.SHULKER_BULLET`. Conservar ese tipo es la forma más robusta de mantener duplicación vanilla.
- `Projectile.onHitBlock` delega en `BlockState.onProjectileHit`; `TargetBlock` produce redstone desde ese hook. No hace falta activación remota especial.
- `WindChargeItem` y `FireChargeItem` implementan `ProjectileItem`; 26.2 expone `DispenserBlock.registerProjectileBehavior`. `Wind Charge` obtiene su cooldown de 0,5 s mediante `Item.Properties.useCooldown(0.5f)`.
- `ShulkerBulletRenderer` usa el modelo vanilla de tres prismas ortogonales y `minecraft:textures/entity/shulker/spark.png`. Reutilizar el tipo vanilla conserva renderer y lenguaje visual sin registrar otra entidad.

## Arquitectura convergida

1. Registrar `ShulkerChargeItem` stack 64. La textura 2D final queda bloqueada a asset del usuario; no bloquea gameplay.
2. Las Charges lanzadas serán **instancias normales de `ShulkerBullet` con `EntityTypes.SHULKER_BULLET`**, ampliadas mediante `ShulkerBulletMixin` + interfaz duck persistente.
3. El mixin sólo altera navegación cuando una bullet está marcada como Charge lanzada. Bullets naturales conservan vanilla salvo la nueva regla de captura al recibir melee/arrow.
4. Estado extra persistente: `launchedCharge`, dirección de intención original, Target Block opcional y fence de captura. Owner vanilla conserva attribution de jugador; dispenser puede permanecer ownerless.
5. Target de entidad: escribir `finalTarget` y reutilizar `selectNextMoveDirection` vanilla. No se duplica su navegación.
6. Target Block: mantener `finalTarget=null` y reproducir sólo el mínimo routing cardinal hacia `BlockPos`; `setNoGravity(true)` evita que la bullet caiga al quedar sin entidad target. Steering libre/block usa la misma velocidad objetivo 0.15.
7. Sin target: vuelo cardinal según componente dominante de la intención; se conserva la intención exacta únicamente para futuros scans.
8. Adquisición de entidades: query AABB 32 bloques, ranking angular/perpendicular, owner excluido, target vivo/no spectator, LOS en cada nueva adquisición. Perder LOS después no invalida lock.
9. Target Blocks: centro-ray prioritario y abanico fijo/acotado de raycasts dentro del cono, nunca scan volumétrico/global. Los raycasts quedan ocluidos por geometría real.
10. Readquisición: sólo si no hay target válido, cada ~4 ticks. Target vivo no se sustituye por otro “mejor”.
11. Captura: inyección server-side en `ShulkerBullet.hurtServer`; `AbstractArrow` o ataque no-projectile de `LivingEntity` generan exactamente un item. El fence se arma antes del drop. Shield/impact/expiry no pasan por esa ruta de captura.
12. Dispenser: `ProjectileItem` + `registerProjectileBehavior`; `asProjectile` crea la misma bullet marcada con facing como intent.

## Riesgos clasificados y respuesta

- **Duplicación shulker accidentalmente rota** → evitada manteniendo tipo vanilla exacto.
- **Block target no soportado por `finalTarget`** → routing mínimo sólo para BlockPos.
- **Scan de bloques caro** → fan de raycasts fijo, no volumen.
- **Doble drop por dos hits** → fence server-side por entidad.
- **Target stale/unloaded** → `EntityReference` vanilla + validación antes de reacquire.
- **Sin target cae por gravedad** → `noGravity` sólo en Charges lanzadas.
- **Asset 2D ausente** → deuda de arte aislada; no se copia textura de terceros.

## Gate

Arquitectura cerrada. S01 puede implementar item/captura/brewing sin depender de targeting ni renderer final.