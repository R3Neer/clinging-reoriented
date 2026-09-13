# GF-S03 — Cámara desacoplada y landing snap

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN**.

## Tesis

Al cerrar S03, todo giro voluntario de jugador cambia física/yaw gauge sin mover la vista mundial; la cámara queda retenida en el quaternion realmente mostrado durante vuelo libre. Sólo `LANDING_COMMITTED` inicia un SLERP hacia el frame canónico del futuro suelo. Invalidar ese landing conserva el frame intermedio, nunca hace snap-back.

## Scope

FR-GF-010..013 y presentación de FR-GF-020..027. Los snaps inmediatos de mobs/monturas y el retirement forzado se conservan salvo regresión; S04 tratará el cuerpo de jugador.

## Protocolo visual

Se mantienen tres operaciones diferentes con una única secuencia monotónica de jugador:

1. `visual_hold_v1(target, yawDelta, sequence)` — giro físico voluntario en vuelo libre.
2. `visual_land_v1(target, kind, sequence)` — comienza landing snap; no aplica nuevo yaw gauge.
3. `visual_cancel_land_v1(sequence)` — cancela sólo la trayectoria de landing y congela el quaternion actualmente mostrado.

`visual_transition_v2` se conserva para transiciones inmediatas no voluntarias/legacy que todavía necesitan el snap actual.

## Hold

Al recibir `visual_hold_v1`:

- captura `VisualTransitions.current(entity)`;
- calcula `compensatedVisualStart(current, yawDelta)` para que aplicar el yaw gauge lógico no cambie el look mundial compuesto;
- aplica el mismo yaw gauge al cliente;
- fuerza el estado interno de Gravity Changer al target físico para que no quede una animación upstream latente;
- devuelve indefinidamente el quaternion compensado desde `GravitySnapMixin`.

Una segunda/tercera reorientación parte siempre del quaternion retenido actual y vuelve a compensarlo. Nada se encola.

## Land

Al recibir `visual_land_v1`:

- toma el quaternion actualmente mostrado, sea cardinal o intermedio;
- NO vuelve a aplicar yaw gauge;
- fuerza el endpoint interno de Gravity Changer al target;
- hace SLERP shortest-path hacia `RotationUtil.getEntityRotationQuaternion(target)`;
- duración nominal: 180 ms quarter, 240 ms half;
- al completar, libera ownership visual y deja a Gravity Changer ya asentado en el mismo endpoint.

## Cancel

Durante un landing activo, cancel captura el quaternion del instante exacto y lo convierte en nuevo HOLD. No vuelve al frame previo ni al target.

El servidor marca entonces `visualBaseKnown=false`; un compromiso posterior usa la ventana conservadora de 240 ms porque el frame ya no es cardinal.

## First Person

Se conserva una sola fuente de quaternion visual: Gravity Changer + `GravitySnapMixin`. First Person sigue consumiendo ese frame; no se crea una cámara alternativa.

## Plan

- [ ] I1 ampliar `Payloads` con Hold/Land/Cancel y secuencia monotónica común;
- [ ] I2 refactorizar `VisualTransitions` con modos HOLD/LAND/SNAP_IMMEDIATE;
- [ ] I3 usar Hold en `ClingingReoriented.attempt` voluntario; mantener snap inmediato en retirement y mounted path por ahora;
- [ ] I4 publicar Land exactamente al crear `LandingState.commit`;
- [ ] I5 publicar Cancel si un commitment ya publicado se invalida físicamente;
- [ ] I6 suprimir feedback de error para `LANDING_COMMITTED`;
- [ ] I7 client tests: 90/180 free-flight hold, multi-turn world-look continuity, landing settle, cancel mid-snap, sequence stale, First Person;
- [ ] I8 snapshots semánticos iniciales: pre-turn, post-physical-turn held, landing start/mid/end;
- [ ] I9 holdout adversarial + revisión cero-cambios.

## Modelo adversarial previo

- payload Hold llega antes/después del atributo de gravedad;
- varios Holds antes de que llegue el target lógico anterior;
- Land llega cuando Gravity Changer aún cree estar animando otro target;
- Cancel exactamente al completar Land;
- stale sequence Hold/Land/Cancel;
- muerte/respawn cambia instancia de animation;
- First Person offset no puede generar un segundo roll;
- pitch casi vertical y 180°;
- world-look debe ser continuo aunque yaw local cambie mucho;
- retirement forzado no puede quedar congelado por un HOLD olvidado.

### Holdout reservado

Secuencia de tres giros físicos `DOWN→EAST→UP→WEST` sin landing: la dirección lógica y el yaw gauge deben cambiar tres veces, pero el forward mundial renderizado antes/después de cada Hold debe ser continuo dentro de epsilon. Después un Land a WEST debe recorrer una sola trayectoria desde ese frame retenido, no tres snaps acumulados.
