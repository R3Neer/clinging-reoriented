# GF-S02 — Landing prediction y commitment

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN**.

## Tesis

Al cerrar S02, el servidor puede identificar con unos pocos ticks de antelación un futuro soporte real bajo los pies de la gravedad actual, incluido un provider externo registrado, y bloquear silenciosamente nuevos cambios voluntarios sólo desde el instante en que el aterrizaje queda suficientemente cierto. No cambia aún la cámara.

## Scope

FR-GF-020..027, FR-GF-030..035 ya preparados por S00 y la parte de input de FR-GF-060..063 necesaria para el lock. S03 será owner de la presentación.

## Decisiones

### Sweep geométrico vanilla

El provider vanilla implementa swept-AABB continuo entre dos cajas de iguales dimensiones. Para cada AABB de colisión:

- calcula intervalo de entrada/salida por los tres ejes;
- acepta sólo contactos cuya normal de entrada sea exactamente `gravity.getOpposite()`;
- exige movimiento hacia el lado de los pies;
- devuelve la menor fracción `t∈[0,1]`;
- identidad/revalidación se basa en la AABB mundial exacta de la collision shape, no en `onGround`.

Una colisión lateral puede existir físicamente, pero no es landing.

### Predicción temporal

`LandingPrediction` avanza un horizonte discreto corto desde el AABB y `deltaMovement` actuales. El desplazamiento de cada tick usa el movimiento previsto y luego aplica gravedad/drag con la misma recurrencia auditada de Gravity Changer/Vanilla para aire normal. El objetivo no es predecir parkour a distancia sino responder: «¿este cuerpo tocará su próximo suelo durante la ventana del snap?».

Se prueban como máximo **5 ticks**, porque 240 ms ≈ 4.8 ticks y cubre el peor giro nominal de 180°. No se amplía el lock más allá de la ventana visual que necesita S03.

La aceleración usa `GravityDirectionUtil.scaleGravity(entity, 0.08)`. El drag aéreo base inicial es 0.98 en el eje local vertical y 0.91 en los ejes locales horizontales; efectos/medios que cambien completamente el modelo (agua, Elytra, vehículo) no son elegibles para commitment en esta fase.

### Umbral de commit

- 90°: sólo commit si ETA ≤ 180 ms + un margen de scheduling de un tick.
- 180°: ETA ≤ 240 ms + un tick.
- El margen no cambia la duración visual futura; evita perder el punto de entrada por discretización de servidor.
- Si el target visual frame ya coincide con el futuro suelo, no hace falta un giro visual y no se crea un lock artificial.

### Revalidación

El `LandingState` guarda contact identity, gravity frame y tick límite. Mientras committed:

- si se obtiene soporte real compatible → touchdown y clear;
- si cambia la gravedad, lifecycle o provider/contact deja de revalidar → cancel;
- nunca se mantiene más allá de un timeout acotado derivado del ETA original + 2 ticks.

### Input

Se añade `Result.LANDING_COMMITTED`. El servidor descarta la petición. El cliente NO reproduce bass ni success sound para ese resultado y no lo encola.

## Plan

- [ ] I1 Implementar swept-AABB puro y tests de floor/ceiling/wall/lateral/tangencial/start overlap.
- [ ] I2 Implementar `VanillaLandingSurfaceProvider.sweep` y revalidación geométrica futura.
- [ ] I3 `LandingPrediction` puro/servidor: máximo 5 ticks, gravedad cardinal, drag auditado, devuelve contacto + ETA + target gravity.
- [ ] I4 `LandingState` por jugador y tick server-authoritative; lifecycle clear y bounded timeout.
- [ ] I5 Bloquear `attempt` durante commitment con resultado silencioso específico.
- [ ] I6 GameTests: 90°, 180°, lateral no commit, provider externo, invalidación, cambio de gravedad externo, touchdown clear, no lock prematuro.
- [ ] I7 Holdout adversarial y revisión cero-cambios.

## Modelo adversarial previo

- corner collision donde dos ejes entran a la vez;
- cuerpo ya solapado al iniciar sweep;
- delta cero/casi cero;
- soporte detrás del jugador pero no en trayectoria;
- pared lateral antes del suelo: no convertir pared en suelo;
- provider devuelve hit anterior al vanilla y luego desaparece;
- contacto válido pero gravity frame cambia;
- 90° con ETA 5 ticks: no lock demasiado temprano;
- 180° necesita más ventana que 90°;
- chunk/surface unloaded entre predicción y commit;
- slime/knockback externo invalida trayectoria después del commit;
- request llega en el mismo tick exacto del touchdown;
- no bass sound por `LANDING_COMMITTED`.

### Holdout reservado

Se reserva un corner donde un obstáculo lateral se alcanza antes que el suelo gravitatorio y ambos están dentro del mismo swept volume. Sólo el contacto cuya normal coincide con `gravity.opposite` puede crear commitment.
