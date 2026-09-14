# Shulker Charge — plan global

Estado global: **S00–S04 CERRADOS · S05 EN CURSO · S06 BLOQUEADO HASTA GATE ADVERSARIAL**.

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
- [x] Detectado y corregido el caso Target Block tratado como obstáculo sólido.
- [x] 105/105 GameTests completos verdes en run #624; revalidado en #626.
- [x] No-change gate de producción cerrado.

## S04 — Cliente, modelo 3D y lenguaje visual
- [x] GUI usa icono/modelo 2D definitivo presente en rama.
- [x] Presentación 3D y snapshots GUI/FP/TP/proyectil.
- [x] Snapshot `FIXED` aislado añadido para que el modelo 3D sea evidencia inspeccionable sin modificar assets.
- [x] Revisión manual + cliente base/First Person/Scale Brews/Fresh Animations + validador verdes en run #626.

## S05 — Campaña adversarial
- [ ] Dispenser real: bloque + block entity + redstone + consumo exacto + facing preservado.
- [ ] Lock válido no se sustituye por candidato posterior mejor.
- [ ] Adquisición tardía automática desde free flight.
- [ ] Target transferido a otra dimensión deja de resolver y provoca readquisición.
- [ ] Carrera mixta arrow/melee y ownership no duplica drops.
- [ ] Dos Charges simultáneas mantienen estado/targets independientes.
- [ ] Stress smoke de múltiples Charges targetless durante varias rondas de readquisición.
- [ ] Clasificar cualquier fallo y completar review/no-change gate final.

## S06 — Canonización e integración
- [ ] Migrar comportamiento estable a README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION/CHANGELOG.
- [ ] Retirar `docs/work/SHULKER_CHARGE_*` tras migración.
- [ ] Bump a `0.1.0-alpha.14` (alpha.13 ya está publicada).
- [ ] CI completa sobre HEAD final de rama.
- [ ] Integrar en `main` mediante PR/merge validado.
- [ ] CI completa sobre `main` exacto.
- [ ] Publicar prerelease `v0.1.0-alpha.14` desde el artefacto exacto validado.
