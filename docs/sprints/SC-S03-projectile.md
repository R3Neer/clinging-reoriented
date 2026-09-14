# SC-S03 — Navegación, impacto, redstone y recaptura

Estado: **CERRADO / GATE VERDE**.

## Scope

SC-030..032, SC-060..082 y holdouts físicos de SC-012/013/070..075. S03 convierte el Target Block lock de S02 en navegación completa y demuestra que impacto, shulker duplication y recaptura siguen pasando por mecanismos vanilla reales.

## Estado real validado

- Las Charges lanzadas siguen siendo exactamente `EntityTypes.SHULKER_BULLET`; no existe EntityType paralelo.
- Entity targets reutilizan el routing vanilla de `ShulkerBullet`.
- Target Blocks usan routing cardinal equivalente: ejes que acercan al objetivo si la celda vecina está libre, fallback cardinal, `targetDelta` base 0.15 y tramos de 10–50 ticks.
- Mientras existe Target Block válido se reutiliza la convergencia vanilla velocity→`targetDelta`; no hay homing curvo.
- `Projectile.onHitBlock` llega al `TargetBlock.onProjectileHit` real. No existe escritura remota de `POWER`.
- `ShulkerBullet.onHitEntity` conserva 4 de daño, Levitation y attribution vanilla.
- `Shulker.hurtServer` sigue viendo un `EntityTypes.SHULKER_BULLET`, por lo que la duplicación vanilla se mantiene.
- El mismo fence de captura evita doble drop en recaptura melee/flecha; shield, impacto normal y discard no devuelven item.

## Holdouts cerrados

- Target Block recto: señal redstone por impacto físico y destrucción del proyectil.
- Target Block desalineado: ruta multi-eje cardinal y llegada real.
- Impacto de entidad: daño vanilla + Levitation, sin capa duplicada de daño.
- Recaptura melee/flecha exactly-once.
- Bloque ordinario y descarte: cero drops.
- Escudo: neutralización sin drop y sin daño al jugador.
- Shulker abierto: la Charge conserva la ruta vanilla de duplicación.
- El tipo del proyectil permanece `EntityTypes.SHULKER_BULLET` durante el recorrido.

## Evidencia

Implementación: `1f544ab56869622293c66abf56a53efbcc755d8e` (`feat: complete Shulker Charge projectile behavior`).

CI: run **#481 / 34855783742**, `success` en build+unit, GameTests servidor, cliente vanilla, First Person, Scale Brews server/client, Fresh Animations y validador de snapshots heredado.

Revisión posterior: sin cambios de producción requeridos para cerrar S03.

Gate: **VERDE**.
