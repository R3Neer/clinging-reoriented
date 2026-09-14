# Shulker Charge — plan global

Estado global: **S00–S02 CERRADOS · S03 REABIERTO POR GAP DE COBERTURA · S04 BLOQUEADO EN GATE**.

Baseline: `v0.1.0-alpha.13` / `be3b47284a71fbf4fcacb2ab2b2cb3776af290d3`.

## S00 — Investigación y arquitectura
- [x] Arquitectura manteniendo `EntityTypes.SHULKER_BULLET` exacto.

## S01 — Item, captura y brewing
- [x] Item stack 64 + assets definitivos presentes en rama.
- [x] Captura melee/flecha y brewing con Charge.
- [x] Fence anti-doble-drop.

## S02 — Lanzamiento, dispenser y adquisición
- [x] Uso manual/cooldown y dispenser.
- [x] Targeting angular, Target Block directo, LOS inicial y readquisición.
- [x] Persistencia de identidad/intención.

## S03 — Navegación, impacto, redstone y recaptura
- [x] Implementación de routing/impacto/duplicación/recaptura.
- [ ] Revalidar holdouts físicos ahora que las suites están explícitamente registradas.
- [ ] CI completa verde sobre batería real de Shulker Charge.

## S04 — Cliente, modelo 3D y lenguaje visual
- [x] GUI usa icono/modelo 2D definitivo presente en rama.
- [x] Candidato de mano 3D y snapshots añadido.
- [ ] Desbloquear tras cierre real de S03.
- [ ] Revisar manualmente snapshots S04 y cerrar client gate.

## S05 — Campaña adversarial
- [ ] Matriz combinada, performance, lifecycle y concurrencia.

## S06 — Canonización e integración
- [ ] Docs canónicos, retirar temporales, bump, CI rama/main y prerelease exacta.
