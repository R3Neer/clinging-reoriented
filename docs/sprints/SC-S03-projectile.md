# SC-S03 — Navegación, impacto, redstone y recaptura

Estado: **REABIERTO / COBERTURA REAL EN CORRECCIÓN**.

La incorporación explícita de las suites Shulker Charge al registro de GameTests durante S04 expuso que la evidencia anterior de #481 no era suficiente para certificar estos holdouts físicos. La primera batería fiable de 105 tests (#491) detectó seis fallos: uno de modo de juego implícito del mock y cinco fixtures físicos con timeout de ~20 ticks para recorridos que, a velocidad vanilla ~0,15 bloques/tick, requieren bastante más tiempo; varios además no poseían explícitamente el volumen de aire de su recorrido.

Clasificación TM: **test/fixture y cobertura**, salvo que la batería corregida revele un fallo de producción.

Correcciones de fixture:
- player manual explícitamente Survival para verificar consumo;
- corredores/volúmenes de prueba limpiados antes de colocar targets/obstáculos;
- `maxTicks` ajustado a la distancia y navegación cardinal real;
- ninguna modificación de producción en esta iteración.

El sprint sólo vuelve a `CERRADO / GATE VERDE` cuando los holdouts Shulker Charge registrados pasen realmente en CI completa.
