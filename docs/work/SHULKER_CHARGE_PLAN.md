# Shulker Charge — plan global

Estado global: **S00–S02 CERRADOS · S03 EN CURSO**.

Baseline: `v0.1.0-alpha.13` / `be3b47284a71fbf4fcacb2ab2b2cb3776af290d3`.

## S00 — Investigación y arquitectura
- [x] Auditar registro de items/entities/renderers/dispenser del mod y APIs Fabric 26.2.
- [x] Auditar comportamiento real de `ShulkerBullet`, `WindCharge`/`FireCharge`, dispenser y `TargetBlock`.
- [x] Fijar arquitectura manteniendo `EntityTypes.SHULKER_BULLET` exacto.

## S01 — Item, captura y brewing
- [x] Item stack 64 + icono 2D aprobado.
- [x] Captura melee/flecha natural, incluido ownerless/dispenser-style arrow.
- [x] Fence anti-doble-drop.
- [x] Reorientation usa Charge y rechaza Shulker Shell.
- [x] CI #475 completa verde.

## S02 — Lanzamiento, dispenser y adquisición
- [x] Uso manual con cooldown component 0,5 s y consumo exacto.
- [x] Dispenser con mismo EntityType y facing como intent.
- [x] Ray/cone ranking, Target Block central prioritario y entity LOS inicial.
- [x] No-target flight y readquisición sólo tras invalidación real.
- [x] Persistencia de identidad/intención.
- [x] CI #479 completa verde sobre `9f929af`.

## S03 — Navegación, impacto, redstone y recaptura
- [ ] Routing ortogonal completo hacia Target Blocks.
- [ ] Daño + Levitation y attribution compatibles.
- [ ] Target Block activado por impacto real.
- [ ] Duplicación vanilla de shulkers deliberadamente conservada.
- [ ] Charge lanzada recapturable por melee/flecha, shield/impact/expiry sin item.
- [ ] Tests de concurrencia y no-duplicación.

## S04 — Cliente, modelo 3D y lenguaje visual
- [ ] Render in-hand con representación 3D; inventario/GUI usa icono 2D aprobado.
- [ ] Mantener lenguaje visual de bullet, sin homing curvo ni HUD de lock.
- [ ] Client GameTests y snapshots propios de Shulker Charge.

## S05 — Campaña adversarial
- [ ] Combinar PvP, arrows, dispenser, target death/removal, chunks/lifecycle y múltiples interceptores.
- [ ] Revisar farms/duplicación intencionada y performance de targeting/routing.
- [ ] Revisar snapshots y pasada completa sin cambios.

## S06 — Canonización e integración
- [ ] README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION/CHANGELOG.
- [ ] Capturas README si explican mejor la feature.
- [ ] Documentar arte/licencia.
- [ ] Retirar `docs/work/*`.
- [ ] Bump, CI rama, main, CI main y prerelease exacta.