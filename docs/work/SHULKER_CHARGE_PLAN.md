# Shulker Charge — plan global

Estado global: **S00–S01 CERRADOS · S02 EN CURSO**.

Baseline: `v0.1.0-alpha.13` / `be3b47284a71fbf4fcacb2ab2b2cb3776af290d3`.

## S00 — Investigación y arquitectura
- [x] Auditar registro de items/entities/renderers/dispenser del mod y APIs Fabric 26.2.
- [x] Auditar comportamiento real de `ShulkerBullet`: hurt, owner, target, navegación, impacto y duplicación de shulkers.
- [x] Auditar `WindCharge`/`FireCharge`/dispenser y `TargetBlock` actuales.
- [x] Fijar arquitectura de item, projectile, targeting y recaptura sin textura final.

## S01 — Item, captura y brewing
- [x] Registrar Shulker Charge stack 64.
- [x] Captura melee y flecha de bullet natural, incluido dispenser-arrow.
- [x] Fence anti-doble-drop y rechazo de destrucción genérica; shield/impact/expiry end-to-end reservados como holdouts S03.
- [x] Reorientation usa Shulker Charge y rechaza Shulker Shell.
- [x] CI completa #475 verde sobre `e9e49c2`.

## S02 — Lanzamiento, dispenser y adquisición
- [ ] Uso manual con cooldown inicial 0,5 s y consumo exacto.
- [ ] Dispenser con mismo proyectil y facing como intent.
- [ ] Selección por ray/cone, Target Block directo prioritario y entity LOS en cada nueva adquisición.
- [ ] Vuelo sin target y readquisición sólo al quedar sin target válido.
- [ ] Tests de ranking, no-target y lifecycle de target.

## S03 — Navegación, impacto, redstone y recaptura
- [ ] Reutilizar/conservar navegación ortogonal tipo shulker.
- [ ] Daño + Levitation y attribution compatibles.
- [ ] Target Block activado por impacto real.
- [ ] Duplicación vanilla de shulkers deliberadamente conservada.
- [ ] Charge lanzada recapturable por melee/flecha, escudo sin item.
- [ ] Tests de concurrencia y no-duplicación.

## S04 — Cliente, modelo 3D y lenguaje visual
- [ ] Render in-hand con representación 3D; usar el icono 2D aprobado ya incorporado en la rama para inventario/GUI.
- [ ] Mantener lenguaje visual de bullet, sin homing curvo ni HUD de lock.
- [ ] Client GameTests y snapshots de vuelo libre, lock, reacquire, Target Block e intercepción cuando sean observables.

## S05 — Campaña adversarial
- [ ] Combinar PvP, arrows, dispenser, target death/removal, chunks/lifecycle y múltiples interceptores.
- [ ] Revisar duplicación de shulkers y farms sin bloquear comportamiento intencionado.
- [ ] Revisar performance del scan periódico y ausencia de scans globales.
- [ ] Revisar artefactos/snapshots y hacer pasada completa sin cambios.

## S06 — Canonización e integración
- [ ] Actualizar README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION/CHANGELOG.
- [ ] Añadir capturas nuevas al README sólo si explican mejor la feature y existe arte estable.
- [ ] Documentar licencia/origen del icono 2D aprobado y del modelo 3D reutilizado.
- [ ] Retirar `docs/work/*` tras migración.
- [ ] Bump de versión, CI exacta de rama, integración a main, CI main y prerelease si el cambio se publica como nueva alpha.