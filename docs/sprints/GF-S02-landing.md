# GF-S02 — Landing prediction y commitment

Estado: **CERRADO / GATE VERDE**.

## Tesis

El servidor identifica con unos pocos ticks de antelación un futuro soporte real bajo los pies de la gravedad actual, incluido un provider externo registrado, y bloquea silenciosamente nuevos cambios voluntarios sólo desde el instante en que el aterrizaje queda suficientemente cierto. La cámara sigue siendo owner de S03.

## Scope

FR-GF-020..027, FR-GF-030..035 preparados por S00 y la parte de input de FR-GF-060..063 necesaria para el lock.

## Decisiones finales

### Sweep geométrico vanilla

El provider vanilla implementa swept-AABB continuo entre dos cajas de iguales dimensiones. Para cada AABB de colisión calcula intervalo de entrada/salida por los tres ejes, acepta sólo contactos cuya normal de entrada sea `gravity.getOpposite()`, exige movimiento hacia el lado de los pies y devuelve la menor fracción `t∈[0,1]`. Una colisión lateral puede existir físicamente, pero no es landing.

### Predicción temporal

`LandingPrediction` avanza un horizonte discreto corto desde AABB y `deltaMovement`, reutilizando la recurrencia auditada de gravedad/drag. Se prueban como máximo **5 ticks**, suficiente para la ventana nominal de 180/240 ms más discretización de servidor.

### Commitment

- 90°: commit dentro de la ventana nominal de 180 ms + margen de scheduling de un tick.
- 180° o base visual no cardinal: ventana conservadora equivalente a 240 ms + margen.
- Un frame visual ya coincidente con el futuro suelo no necesita lock artificial por cámara.
- Mientras committed, el servidor descarta nuevas peticiones; no se encolan ni roban salto vanilla.

### Revalidación

`LandingState` conserva identity/contacto, gravity frame y timeout acotado. Soporte real compatible produce touchdown/clear; cambio de gravedad, lifecycle o pérdida de revalidación cancela. El commit nunca vive indefinidamente.

## Checklist de cierre

- [x] I1 swept-AABB puro + tests de floor/ceiling/wall/lateral/tangencial/start overlap.
- [x] I2 `VanillaLandingSurfaceProvider.sweep` y revalidación geométrica.
- [x] I3 `LandingPrediction` de máximo 5 ticks con gravedad cardinal y drag auditado.
- [x] I4 `LandingState` server-authoritative, lifecycle clear y bounded timeout.
- [x] I5 bloqueo silencioso de input durante commitment.
- [x] I6 GameTests de 90°, 180°, lateral, provider, invalidación, cambio externo, touchdown y no-lock prematuro.
- [x] I7 holdouts y revisión cruzada posterior en S03–S05.

## Holdout revelado

Corner con obstáculo lateral anterior al suelo dentro del mismo swept volume: sólo la normal compatible con `gravity.opposite` puede crear commitment. La arquitectura final mantiene esta propiedad en el provider vanilla y en la validación del contrato.

## Deuda explícita

S05 debe demostrar el cruce `predicción válida → soporte destruido → input durante commitment/cancel` sin queue diferida ni snap-back visual.
