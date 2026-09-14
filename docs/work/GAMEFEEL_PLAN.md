# Plan temporal — Gamefeel / Gravity Fall

Fuente normativa: `GAMEFEEL_REQUIREMENTS.md`. Protocolo: `GAMEFEEL_SPRINT_WORKFLOW.md`.

Estado global: **S00–S05 cerrados; S06 EN CURSO**.

## Gates y sprints

### S00 — Foundation audit y seam extensible — CERRADO

Objetivo: reconstruir ownership de cámara/física/caída/input y definir una frontera pública de surfaces/landing sin dependencia Scale.

- [x] inventario de writers/readers de gravedad, yaw, visual quaternions, fallDistance y grounded;
- [x] inventario de client GameTests/snapshots;
- [x] contrato público mínimo de surface/landing providers;
- [x] provider vanilla equivalente;
- [x] tests fail-closed;
- [x] CI acumulativa posterior sin regresiones.

### S01 — Impact kernel — CERRADO

Objetivo: derivar impacto de velocidad precolisión/componentes bloqueadas y mapearlo al pipeline vanilla sin reset por cambio de gravedad.

- [x] kernel puro de impacto/caída equivalente;
- [x] hook mínimo en collision/fall pipeline;
- [x] preserve vanilla blocks/enchant/effects;
- [x] ownership transition seguro;
- [x] tests 90/180/multi-turn/frenado/exploit tardío;
- [x] retirada de resets segmentados demostrada.

### S02 — Landing prediction y commitment — CERRADO

Objetivo: detectar server-authoritative el futuro soporte válido a tiempo para el snap y bloquear nuevos cambios durante commitment.

- [x] predictor acotado usando providers;
- [x] ETA nominal 180/240 ms;
- [x] revalidación de identidad/contacto;
- [x] invalidación externa y fail-closed;
- [x] input lock sin queue;
- [x] tests de bordes/invalidaciones.

### S03 — Cámara desacoplada y landing snap — CERRADO

Objetivo: ningún cambio voluntario de gravedad gira cámara; sólo landing commitment publica transición visual desde el frame realmente mostrado.

- [x] physical turn separado de visual publication;
- [x] frame visual estable en vuelo libre;
- [x] landing transition 90/180 desde current quaternion;
- [x] no snap-back en invalidación;
- [x] First Person;
- [x] snapshots temporales del arco completo.

### S04 — Sustained Gravity Fall y controles — CERRADO

Objetivo: después de 12 ticks airborne, blend 6 ticks hacia body orientation por velocidad sin tocar cámara ni convertirlo en Elytra.

- [x] estado/contador y lifecycle;
- [x] orientation transport por velocidad con twist continuo;
- [x] zero-speed hold;
- [x] salida hacia landing frame;
- [x] W/A/S/D coherentes con frame visual sin nuevo steering;
- [x] remote player rendering;
- [x] First Person lane;
- [x] snapshots Falling / Gravity Fall / landing.

Evidencia de cierre histórica: commit `e2b394710e50fa58253cbd69d55c927996832936`, run 427.

### S05 — Adversarial visual campaign y hardening — CERRADO

Objetivo: romper interacciones entre física, cámara, body, impacto, landing, lifecycle y compatibilidad antes de declarar estabilidad.

- [x] holdouts de todos los sprints;
- [x] secuencia `EAST→UP→NORTH→DOWN→WEST→UP`;
- [x] high speed / late turn / arbitrary-normal / ownership exit;
- [x] invalidación/cancel de landing y no queue;
- [x] agua/Elytra/mounts/pets/respawn/teleport/tracking;
- [x] snapshot matrix completa;
- [x] Fresh Animations + EMF/ETF fixture fijado;
- [x] manifiesto automático de evidencia visual;
- [x] revisión completa sin cambios de producción;
- [x] CI exacta de evidencia: commit `a074ff1a1c6f40bef2e23048c420dc9ab1751ec0`, run 464, 86/86 server GameTests + todas las lanes verdes.

### S06 — Canonización y release — EN CURSO

Objetivo: retirar documentación temporal, actualizar fuentes canónicas, validar cabeza exacta, integrar en main y publicar prerelease.

- [ ] README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION/CHANGELOG;
- [ ] retirar `docs/work/*` tras migrar contenido estable;
- [ ] bump a `0.1.0-alpha.13`;
- [ ] CI completa sobre commit final de rama;
- [ ] merge a main sin cambios no relacionados;
- [ ] CI de main;
- [ ] prerelease `v0.1.0-alpha.13` con artefacto exacto validado.

## Estado de handoff

No volver a abrir S00–S05 salvo que S06 descubra una regresión real. Cualquier fallo durante canonización/release se clasifica según `GAMEFEEL_SPRINT_WORKFLOW.md`; un fallo documental/build/release vuelve a S06, mientras que un fallo de comportamiento observado en la validación obliga a reabrir el sprint propietario y repetir sus gates.
