# SC-S03 — Navegación, impacto, redstone y recaptura

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN**.

## Scope

SC-030..032, SC-060..082 y holdouts físicos de SC-012/013/070..075. S03 convierte el Target Block lock de S02 en navegación completa y demuestra que impacto, shulker duplication y recaptura siguen pasando por mecanismos vanilla reales.

## Estado real leído

- Vanilla sólo ejecuta el recálculo ortogonal post-move cuando `finalTarget != null`; un Target Block guardado fuera de `finalTarget` necesita reproducir ese pequeño bucle.
- `selectNextMoveDirection` vanilla: elige ejes que acercan al target si el bloque vecino está vacío; si no hay opción, prueba una cardinal libre; target delta normalizada a 0.15; tramo 10–50 ticks.
- Cada tick con target vivo, vanilla acelera suavemente velocity hacia `targetDelta` con factor 0.2 y expande `targetDelta` *1.025.
- `Projectile.onHitBlock` llama al `BlockState.onProjectileHit` real. `TargetBlock` fija `POWER` y programa reset (8 ticks para proyectiles no-flecha).
- `ShulkerBullet.onHitEntity` ya hace 4 de daño + Levitation 200 y attribution por owner.
- `Shulker.hurtServer` llama a duplicación sólo cuando direct projectile es exactamente `EntityTypes.SHULKER_BULLET`; nuestra Charge mantiene ese type.
- `ShulkerBullet.onHit` destruye la bullet tras impacto; `hurtServer` destruye tras ser golpeada.

## Plan

1. Añadir routing de BlockPos que replique la semántica vanilla de tramos cardinales sin tocar entity-target routing.
2. En HEAD del tick, si existe Target Block válido, aplicar la misma convergencia velocity→targetDelta que vanilla aplica a entity target; `noGravity` sigue activo.
3. En TAIL, si la bullet sigue viva y el Target Block continúa válido:
   - decrementar `flightSteps` y recalcular al llegar a cero;
   - recalcular al topar con bloque en la dirección actual;
   - recalcular cuando el eje actual alcanza la coordenada del Target Block.
4. El último tramo apunta físicamente al centro del Target Block; la colisión debe acabar en `TargetBlock.onProjectileHit`, nunca en escritura remota de POWER.
5. No modificar `onHitEntity`: daño/Levitation y shulker duplication deben surgir del código vanilla.
6. Reusar el fence de captura para Charges lanzadas; melee/flecha devuelven un item, shield/impact/expiry no.
7. Añadir test-only accessor de Shulker sólo si hace falta abrirlo de forma determinista para el holdout de duplicación; producción no recibe atajo de reproducción.

## Modelo adversarial previo

- obstáculo en el primer eje obliga a giro sin perder target;
- tramo llega a coordenada del target y debe girar, no seguir de largo;
- Target Block desaparece entre dos tramos: se invalida y vuelve a S02 reacquire;
- bullet impacta Target Block mientras se recalcula: un solo impacto y destrucción;
- entity impact no duplica daño por nuestra capa;
- owner jugador conserva attribution; ownerless dispenser sigue dañando sin owner falso;
- Charge recapturada no puede soltar dos items aunque entren melee/arrow casi a la vez;
- shield bloquea daño pero no pasa por el hook de captura;
- shulker duplication no se desactiva por la marca `launchedCharge`.

## Tests de cierre

- Target Block recto: impacto real y `POWER>0` observado dentro de su ventana de 8 ticks;
- Target Block con obstáculo: al menos un cambio de eje y llegada/impacto;
- entity target: daño exacto compatible y Levitation 200-ish tras impacto;
- player owner y dispenser ownerless attribution no rompen el hit;
- launched Charge recaptura melee y arrow exactly-once;
- shield contra Charge lanzada: sin drop;
- block impact/expiry: sin drop;
- shulker abierto + Charge lanzada: duplicación vanilla produce segundo shulker en fixture determinista;
- type permanece `EntityTypes.SHULKER_BULLET` en todo el recorrido.

Gate: CI completa verde y revisión de producción/tests sin cambios pendientes.