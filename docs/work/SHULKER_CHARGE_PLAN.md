# Shulker Charge — plan global

Estado global: **S00 EN CURSO**.

Baseline: `v0.1.0-alpha.13` / `be3b47284a71fbf4fcacb2ab2b2cb3776af290d3`.

## S00 — Investigación y arquitectura
- [ ] Auditar registro de items/entities/renderers/dispenser del mod y APIs Fabric 26.2.
- [ ] Auditar comportamiento real de `ShulkerBullet`: hurt, owner, target, navegación, impacto y duplicación de shulkers.
- [ ] Auditar `WindCharge`/`FireCharge`/dispenser y `TargetBlock` actuales.
- [ ] Fijar arquitectura de item, projectile, targeting y recaptura sin textura final.

## S01 — Item, captura y brewing
- [ ] Registrar Shulker Charge stack 64 con placeholder técnico no final.
- [ ] Captura melee y flecha de bullet natural, incluido dispenser-arrow.
- [ ] Escudo/impacto/expiry sin drop y fence anti-doble-drop.
- [ ] Reorientation usa Shulker Charge y rechaza Shulker Shell.
- [ ] Unit + GameTests del dominio.

## S02 — Lanzamiento, dispenser y adquisición
- [ ] Uso manual con cooldown inicial 10 ticks y consumo exacto.
- [ ] Dispenser con mismo proyectil y facing como intent.
- [ ] Selección por ray/cone, Target Block directo prioritario y entity LOS inicial.
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
- [ ] Render in-hand con representación 3D; inventario queda con placeholder hasta recibir arte 2D final.
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
- [ ] Documentar deuda exclusiva: textura 2D final suministrada por usuario.
- [ ] Retirar `docs/work/*` tras migración.
- [ ] Bump de versión, CI exacta de rama, integración a main, CI main y prerelease si el cambio se publica como nueva alpha.