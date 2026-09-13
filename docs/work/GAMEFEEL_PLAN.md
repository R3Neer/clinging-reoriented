# Plan temporal — Gamefeel / Gravity Fall

Fuente normativa: `GAMEFEEL_REQUIREMENTS.md`. Protocolo: `GAMEFEEL_SPRINT_WORKFLOW.md`.

## Gates y sprints

### S00 — Foundation audit y seam extensible

Objetivo: reconstruir el ownership actual de cámara/física/caída/input, definir la frontera pública para superficies y dejar baseline reproducible sin cambiar todavía el comportamiento jugable.

- [ ] inventario de writers/readers de gravedad, yaw, visual quaternions, fallDistance y grounded;
- [ ] inventario de client GameTests/snapshots existentes;
- [ ] contrato público mínimo de surface/landing providers sin dependencia Scale;
- [ ] provider vanilla equivalente al soporte actual;
- [ ] tests del contrato/fail-closed;
- [ ] baseline CI exacto.

### S01 — Impact kernel

Objetivo: derivar impacto de velocidad precolisión/componentes bloqueadas y mapearlo al pipeline vanilla sin reset por cambio de gravedad.

- [ ] kernel puro de impacto/caída equivalente;
- [ ] hook mínimo en collision/fall pipeline;
- [ ] preserve vanilla blocks/enchant/effects;
- [ ] ownership transition seguro;
- [ ] tests 90/180/multi-turn/frenado/exploit tardío;
- [ ] eliminar resets sólo tras demostrar equivalencia.

### S02 — Landing prediction y commitment

Objetivo: detectar de forma server-authoritative el futuro soporte válido a tiempo para el snap y bloquear nuevos cambios durante el commitment.

- [ ] predictor acotado usando providers;
- [ ] ETA nominal 180/240 ms;
- [ ] revalidación de identidad/contacto;
- [ ] invalidación externa y fail-closed;
- [ ] input lock sin queue;
- [ ] tests de bordes/invalidaciones/latencia lógica.

### S03 — Cámara desacoplada y landing snap

Objetivo: ningún cambio de gravedad gira cámara; sólo el landing commitment publica una transición visual, desde el frame realmente mostrado.

- [ ] separar physical turn de visual publication;
- [ ] mantener frame visual estable durante vuelo libre;
- [ ] landing transition 90/180 desde current quaternion;
- [ ] no snap-back en invalidación;
- [ ] First Person;
- [ ] snapshots temporales intensivos del arco completo.

### S04 — Sustained Gravity Fall y controles

Objetivo: después de 12 ticks airborne, blend 6 ticks hacia body orientation por velocidad sin tocar cámara ni convertirlo en Elytra.

- [ ] estado/contador derivable y lifecycle;
- [ ] orientation transport por velocidad con twist continuo;
- [ ] zero-speed hold;
- [ ] salida hacia landing frame;
- [ ] W/A/S/D coherentes con frame visual sin nuevo steering;
- [ ] remote player rendering;
- [ ] FA/EMF y First Person lanes;
- [ ] snapshots comparativos Falling / Gravity Fall / Elytra.

### S05 — Adversarial visual campaign y hardening

Objetivo: intentar romper física, cámara y lectura visual con secuencias combinadas antes de declarar estabilidad.

- [ ] holdouts de todos los sprints;
- [ ] secuencias EAST→UP→NORTH→DOWN→WEST→UP;
- [ ] high speed / late turn / zero crossing;
- [ ] invalidación de superficie;
- [ ] agua/Elytra/mounts/pets/respawn;
- [ ] snapshot matrix completa;
- [ ] harness temporal de inspección/comparación si aporta señal;
- [ ] revisión cero-cambios.

### S06 — Canonización y release

Objetivo: retirar documentación temporal, actualizar fuentes canónicas, validar cabeza exacta, integrar en main y publicar prerelease.

- [ ] README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION/CHANGELOG;
- [ ] retirar `docs/work/*` tras migrar contenido estable;
- [ ] bump de prerelease;
- [ ] CI completa sobre commit final de rama;
- [ ] merge a main sin cambios no relacionados;
- [ ] CI de main;
- [ ] prerelease nueva con artefacto exacto validado.
