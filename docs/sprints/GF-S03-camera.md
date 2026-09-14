# GF-S03 — Cámara desacoplada y landing snap

Estado: **CERRADO / GATE VERDE**.

## Tesis

Todo giro voluntario de jugador cambia física/yaw gauge sin mover la vista mundial; la cámara queda retenida en el quaternion realmente mostrado durante vuelo libre. Sólo `LANDING_COMMITTED` inicia un SLERP hacia el frame canónico del futuro suelo. Invalidar ese landing conserva el frame intermedio, nunca hace snap-back.

## Scope

FR-GF-010..013 y presentación de FR-GF-020..027. Los snaps inmediatos de mobs/monturas se conservan fuera del free-flight del jugador.

## Protocolo visual final

1. `visual_hold_v1(target, yawDelta, sequence)` — giro físico voluntario en vuelo libre.
2. `visual_land_v1(target, kind, sequence)` — comienza landing snap; no reaplica yaw gauge.
3. `visual_cancel_land_v1(sequence)` — cancela sólo la trayectoria de landing y congela el quaternion mostrado.
4. `visual_transition_v2` — transiciones inmediatas no voluntarias/legacy y entidades que todavía requieren snap propio.

### Hold

Captura `VisualTransitions.current(entity)`, compensa el yaw gauge para conservar look mundial y fuerza Gravity Changer al endpoint físico sin dejar interpolación upstream latente. Giros posteriores parten siempre del quaternion retenido actual; nada se encola.

### Land

Parte del quaternion actualmente mostrado y hace shortest-path SLERP a `RotationUtil.getEntityRotationQuaternion(target)` con duraciones nominales 180 ms quarter / 240 ms half. Al completar libera ownership visual dejando Gravity Changer ya asentado en el mismo endpoint.

### Cancel

Captura el quaternion exacto del instante de cancelación y lo convierte en HOLD. No vuelve al frame previo ni al target. El servidor pasa a base visual desconocida y futuros commitments usan la ventana conservadora.

### First Person

Se conserva una única fuente de quaternion visual: Gravity Changer + `GravitySnapMixin`. First Person consume ese frame; no existe una segunda cámara paralela.

## Checklist de cierre

- [x] I1 ampliar `Payloads` con Hold/Land/Cancel y secuencia monotónica común.
- [x] I2 refactorizar `VisualTransitions` con modos HOLD/LAND/SNAP_IMMEDIATE.
- [x] I3 usar Hold en giros voluntarios y mantener snaps inmediatos donde corresponde.
- [x] I4 publicar Land al crear `LandingState.commit`.
- [x] I5 publicar Cancel si el commitment publicado se invalida.
- [x] I6 suprimir feedback de error para `LANDING_COMMITTED`.
- [x] I7 client tests de free-flight hold, multi-turn, landing settle, cancel, stale sequence y First Person.
- [x] I8 snapshots semánticos de pre-turn, hold y landing.
- [x] I9 holdout multigiro y revisión cruzada posterior en S04–S05.

## Holdout revelado

La secuencia `DOWN→EAST→UP→WEST` sin landing cambia tres veces la dirección lógica y el yaw gauge, pero mantiene continuo el forward mundial renderizado. Un Land posterior recorre una sola trayectoria desde el frame retenido actual, no una cola de snaps acumulados.

## Evidencia acumulada

S04 y S05 construyen encima del mismo ownership de cámara. La campaña S05 incluye además pérdida de ownership, agua/Elytra, teleports, respawn, tracking y multigiro, por lo que cualquier regresión estructural de S03 reaparece allí.
