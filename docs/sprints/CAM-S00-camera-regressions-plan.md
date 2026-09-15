# CAM-S00 — Plan TM de regresiones de cámara Gravity Fall

Estado: **PLANIFICADO / SIN CAMBIOS DE PRODUCCIÓN**.

## Problemas separados

### C1 — Primera persona: handedness de ratón en esfera completa

Síntoma observado: durante Gravity Fall, después de ciertos giros/pasos por los polos, un desplazamiento horizontal del ratón puede producir una rotación visual en el sentido contrario.

Hipótesis principal: `GravityFallLookMixin` integra `yaw += mouseX` y `pitch += mouseY` en coordenadas de Euler globales. Al cruzar ±90° de pitch cambia la handedness visual del eje yaw, aunque el vector de mirada siga siendo matemáticamente válido.

### C2 — Tercera persona: seguimiento/orbita de mirada incómodo

Síntoma observado: en `THIRD_PERSON_BACK`, durante Gravity Fall la cámara no sigue de forma natural la intención de mirada y puede sentirse desacoplada o extraña.

No se asumirá que la solución sea pegar la cámara al cuerpo. La arquitectura de Gravity Fall mantiene dos responsabilidades distintas:

- la **cámara** expresa la intención de mirada del jugador;
- el **cuerpo** cuenta la trayectoria física y puede retrasarse/transportarse según la velocidad.

## Invariantes de diseño

1. **Handedness de pantalla:** mouse +X siempre significa girar visualmente hacia la derecha de la pantalla y mouse -X hacia la izquierda, para cualquier orientación válida de Gravity Fall.
2. **Vertical coherente:** mouse +Y/-Y conserva el mismo significado visual a través de ambos polos y durante loops completos.
3. **Continuidad:** cruzar ±90°, ±180° o completar 360° no introduce snap, inversión instantánea, roll espurio ni discontinuidad de interpolación.
4. **Cámara independiente del body root:** cambios de `BodyOrientation` por velocidad no arrastran ni rotan por sí mismos la cámara.
5. **Tercera persona estable:** la cámara orbita un pivote estable del jugador siguiendo la intención de mirada, no el quaternion corporal de Gravity Fall.
6. **Sin pérdida de semántica vanilla al salir:** al terminar Gravity Fall, la orientación se canonicaliza a yaw/pitch vanilla equivalente sin cambiar el vector de mirada.
7. **Sin cambios físicos colaterales:** selección de gravedad, momentum, aerodinámica y remapeo W/A/S/D siguen siendo independientes de la implementación de cámara.

## Método TM

### CAM-S01 — Reproducciones rojas

No tocar producción.

Añadir un Client GameTest de primera persona que pruebe input horizontal/vertical real en al menos estas regiones: pitch 0°, +80°, +100°, +170°, -80°, -100° y -170°. Cada impulso debe comprobar la dirección en **screen-space**, no sólo el valor numérico de yaw/pitch.

Añadir diagonales alrededor de ambos polos y un loop vertical completo con impulsos horizontales antes/después de cada cruce.

Añadir un Client GameTest de tercera persona que mida `mainCamera` forward/right/up y posición/orbita mientras:

- el jugador mira a izquierda/derecha/arriba/abajo;
- el body permanece con velocidad fija;
- el body cambia 90° y 180° por cambio de trayectoria sin input de cámara;
- la velocidad cruza por cero y se invierte;
- se alterna `THIRD_PERSON_BACK` y, como control, `FIRST_PERSON`.

Cada checkpoint visual importante tendrá screenshot acompañado de una aserción numérica vecina.

### CAM-S02 — Kernel matemático de mirada

Sólo después de obtener reproducciones rojas.

Evaluar una base ortonormal/quaternion de orientación de cámara durante Gravity Fall, aplicando input respecto a los ejes locales de pantalla/cámara y evitando que el comportamiento dependa del signo de `cos(pitch)`.

La solución preferida debe eliminar la singularidad conductual del polo en vez de parchear únicamente el signo de yaw tras ±90°.

Añadir JUnit puro para composición, handedness, normalización, loops y canonicalización de salida.

### CAM-S03 — Integración primera persona

Integrar el kernel en la ruta de input local de Gravity Fall con el mínimo ownership necesario.

Gate: toda la matriz CAM-S01 de primera persona verde, tests históricos full-sphere verdes y salida vanilla sin cambio de gaze.

### CAM-S04 — Integración tercera persona

Usar la misma orientación/intención de cámara para que tercera persona orbite de forma estable alrededor del jugador. El body root continúa siendo velocity-owned y no puede realimentar la cámara.

Gate: fixtures de órbita verdes y snapshots revisados. No se aceptará una solución que haga cómoda una trayectoria a costa de marear en otra.

### CAM-S05 — Campaña adversarial

Cruzar:

- polos + input diagonal;
- varias rotaciones físicas cardinales mientras la cámara conserva intención;
- BODY_LANDING / cancel / RESET;
- agua y salida de agua;
- Elytra boundary;
- First Person mod;
- Fresh Animations/EMF/ETF;
- cambio rápido FIRST_PERSON ↔ THIRD_PERSON_BACK.

Cierre sólo con CI completa verde y revisión de snapshots del HEAD exacto.

## Criterios de aceptación finales

- ningún input horizontal cambia de signo visual al cruzar un polo;
- ningún input vertical cambia de semántica visual por la orientación corporal;
- tercera persona responde a la mirada de forma predecible sin seguir servilmente el body root;
- el body puede rotar por la trayectoria con cámara inmóvil si el jugador no mueve el ratón;
- no hay snap de gaze al entrar/salir de Gravity Fall;
- no hay regresión de HOLD/LAND, First Person, Fresh Animations, Scale Brews ni snapshots existentes.
