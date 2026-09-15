# CAM-S00 — Plan TM de regresiones de cámara Gravity Fall

Estado: **CERRADO / GATE VERDE**.

## Problemas cerrados

### C1 — Primera persona: handedness de ratón en esfera completa

El antiguo full-sphere look almacenaba pitch fuera de ±90° y seguía sumando yaw/pitch Euler. Eso conservaba un vector de mirada matemáticamente válido, pero cambiaba el sentido visual del yaw al atravesar un polo. El resultado podía ser mouse horizontal invertido respecto a la pantalla.

### C2 — Tercera persona: seguimiento/orbita de mirada incómodo

La tercera persona heredaba el mismo frame Euler discontinuo. La solución no consiste en pegar la cámara al cuerpo: el **cuerpo** sigue contando la trayectoria física y la **cámara** sigue expresando intención de mirada.

## Invariantes de cierre

1. **Handedness de pantalla:** mouse +X/-X conserva el mismo significado visual en cualquier orientación de Gravity Fall.
2. **Vertical coherente:** mouse vertical conserva significado a través de ambos polos y loops completos.
3. **Continuidad:** ±90°, ±180° y 360° no introducen snap, inversión ni roll acumulado en un loop vertical puro.
4. **Cámara independiente del body root:** `BodyOrientation` por velocidad no realimenta la cámara.
5. **Tercera persona estable:** usa el mismo look continuo y conserva el boom/clipping vanilla.
6. **Salida vanilla:** al terminar Gravity Fall, yaw/pitch ya representan el mismo forward en coordenadas vanilla compatibles.
7. **Física intacta:** selección de gravedad, momentum, aerodinámica y W/A/S/D permanecen separados de presentación.

## Resultado TM

### CAM-S01 — Reproducciones rojas

Commit `73ab816b111824ac79c903eb5f5bd0a9b6d523d3` añadió pruebas sin tocar producción:

- primera persona +80° frente a +100°;
- primera persona -80° frente a -100°;
- órbita `THIRD_PERSON_BACK` antes/después del polo;
- comprobaciones en screen-space y snapshots vecinos.

Run **#792** (`34948135795`) dejó build/JUnit y server GameTests verdes y falló en Client GameTests al registrar la regresión. El síntoma quedó reproducido antes de implementar.

### CAM-S02 — Kernel matemático

`GravityFallLookMath` pasó a trabajar con una orientación continua. Los deltas de ratón se aplican sobre los ejes locales de cámara/pantalla y se mantiene una representación yaw/pitch vanilla-compatible del mismo forward para interacción, servidor y sistemas existentes.

JUnit cubre handedness a ambos lados de los polos, input diagonal, loop vertical 360°, canonicalización de forward y compensación del yaw gauge durante cambios físicos de gravedad.

### CAM-S03 — Primera persona

`GravityFallLookState` conserva el frame completo de cámara durante Gravity Fall y `GravityFallLookMixin` delega el input al kernel screen-relative. El antiguo almacenamiento de pitch > ±90° deja de ser el estado de producción.

### CAM-S04 — Tercera persona

`GravityFallCameraMixin` entrega a la cámara vanilla el mismo frame continuo. Minecraft conserva su propio brazo de tercera persona, distancia, front view y clipping contra bloques; no existe una órbita paralela inventada por Clinging.

Run **#793** (`34949005750`) pasó la matriz completa con la primera implementación: build/JUnit, server, default client, First Person, Scale Brews server/client, Fresh Animations y snapshot validation.

### CAM-S05 — Campaña adversarial

Se añadieron holdouts para:

- diagonales alrededor de ambos polos;
- continuidad 360° de forward/up;
- cambio `THIRD_PERSON_BACK → FIRST_PERSON → THIRD_PERSON_BACK` conservando el mismo forward/up;
- regresiones históricas HOLD/LAND/body/agua;
- First Person real;
- Scale Brews server/client;
- Fresh Animations/EMF/ETF.

Head de cierre adversarial: `d6a0cf545698e8fe7a2244329664b0d3fb0e480d`.

Run **#794** (`34950338232`) pasó la matriz completa. Los snapshots CAM se incorporan al gate de release de beta.2 para que no vuelvan a ser evidencia opcional.

## Criterios de aceptación

Cumplidos:

- ningún input horizontal cambia de signo visual al cruzar un polo;
- el input diagonal conserva ambos ejes de pantalla;
- tercera persona responde a la misma mirada sin seguir servilmente el body root;
- cambiar entre primera y tercera persona conserva el frame de mirada;
- el body puede cambiar con la trayectoria sin arrastrar la cámara;
- la salida de Gravity Fall conserva gaze;
- no hay regresión en HOLD/LAND, fluidos, First Person, Fresh Animations ni Scale Brews.
