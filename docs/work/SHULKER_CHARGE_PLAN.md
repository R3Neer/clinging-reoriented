# Shulker Charge — plan global

Estado global: **S00–S03 CERRADOS · S04 EN CURSO**.

Baseline: `v0.1.0-alpha.13` / `be3b47284a71fbf4fcacb2ab2b2cb3776af290d3`.

## S00 — Investigación y arquitectura
- [x] Auditar registro de items/entities/renderers/dispenser del mod y APIs Fabric 26.2.
- [x] Auditar comportamiento real de `ShulkerBullet`, Wind/Fire Charge, dispenser y `TargetBlock`.
- [x] Fijar arquitectura manteniendo `EntityTypes.SHULKER_BULLET` exacto.

## S01 — Item, captura y brewing
- [x] Item stack 64 + icono 2D de la rama.
- [x] Captura melee/flecha natural, incluido ownerless/dispenser-style arrow.
- [x] Fence anti-doble-drop.
- [x] Reorientation usa Charge y rechaza Shulker Shell.
- [x] CI #475 completa verde.

## S02 — Lanzamiento, dispenser y adquisición
- [x] Uso manual con cooldown 0,5 s y consumo exacto.
- [x] Dispenser con mismo EntityType y facing como intent.
- [x] Ray/cone ranking, Target Block central prioritario y entity LOS inicial.
- [x] No-target flight y readquisición sólo tras invalidación real.
- [x] Persistencia de identidad/intención.
- [x] CI #479 completa verde sobre `9f929af`.

## S03 — Navegación, impacto, redstone y recaptura
- [x] Routing ortogonal completo hacia Target Blocks.
- [x] Daño + Levitation y attribution compatibles.
- [x] Target Block activado por impacto real.
- [x] Duplicación vanilla de shulkers deliberadamente conservada.
- [x] Charge lanzada recapturable por melee/flecha, shield/impact/expiry sin item.
- [x] Tests de no-duplicación y comportamiento físico.
- [x] CI #481 completa verde sobre `1f544ab`.

## S04 — Cliente, modelo 3D y lenguaje visual
- [ ] GUI usa icono/modelo 2D definitivo presente en la rama.
- [ ] Mano usa representación 3D equivalente al `ShulkerBulletModel`.
- [ ] Mantener renderer vanilla del proyectil y lenguaje ortogonal, sin HUD/homing curvo.
- [ ] Client GameTests y snapshots propios de Shulker Charge.

## S05 — Campaña adversarial
- [ ] PvP, arrows, dispenser, target death/removal, lifecycle y múltiples interceptores.
- [ ] Farms/duplicación intencionada y performance de targeting/routing.
- [ ] Snapshots coherentes y pasada completa sin cambios.

## S06 — Canonización e integración
- [ ] README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION/CHANGELOG.
- [ ] Capturas README si explican mejor la feature.
- [ ] Documentar arte/licencia del estado definitivo de la rama.
- [ ] Retirar `docs/work/*`.
- [ ] Bump, CI rama, main, CI main y prerelease exacta.
