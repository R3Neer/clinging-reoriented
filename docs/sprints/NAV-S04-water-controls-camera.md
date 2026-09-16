# NAV-S04 — agua: controles de cámara y frame visual

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Problema observado

La navegación acuática estaba partida entre dos modelos incompatibles:

- `WorldVerticalWaterMixin` ya forzaba Space/Shift a mundo `+Y/-Y`.
- `VisualMovementMixin` se desactivaba explícitamente en agua.
- Gravity Changer 26.2 redirigía `Entity.moveRelative` y rotaba todo el vector de entrada desde coordenadas de jugador al frame de gravedad física.

Resultado original: con gravedad lateral, la vertical de Space/Shift era mundial pero WASD volvía a depender de la gravedad lógica. Además, la cámara podía conservar el frame gravitatorio mientras el jugador nadaba libremente, aunque el contrato de gameplay fuese “agua libre = presentación world-up”.

## Contratos cerrados

1. **Gravedad lógica ≠ frame acuático.** Entrar en agua no cambia el atributo de gravedad. Sólo cambia cómo se presentan cámara y controles.
2. **W/S = cámara forward/back.** Incluye pitch real de cámara; mirar hacia arriba/abajo produce la correspondiente componente vertical de natación.
3. **A/D = cámara left/right.** El strafe sigue los ejes de pantalla, no el plano tangente de la gravedad física.
4. **Space/Shift = mundo `+Y/-Y`.** Se conserva el boundary implementado por `WorldVerticalWaterMixin`.
5. **Sin aceleración gratis.** El remapeo de WASD es rígido: conserva la magnitud tangencial que vanilla + Gravity Changer ya habían normalizado.
6. **Natación libre = world-up.** Mientras el jugador está en agua y no tiene soporte gravitatorio estable, la presentación converge a una cámara equivalente a gravedad DOWN, sin tocar la gravedad lógica.
7. **Soporte sumergido = floor-up.** Si el AABB realmente está soportado en la dirección de gravedad actual, la presentación converge al frame de ese suelo/pared/techo.
8. **Contacto real, no flag ciego.** La detección de soporte visual usa geometría/`LandingSurfaces`, no sólo `onGround`, y respeta el primer contacto.
9. **Histéresis breve.** Geometría irregular no alterna world-up/floor-up cada tick. La entrada/salida de soporte tiene debounce explícito y acotado.
10. **Transición continua.** Cambiar entre natación libre y soporte no hace snap visual.
11. **Gravity Fall no compite con agua.** El macroestado Gravity Fall sigue retirándose al entrar en fluido.
12. **Ownership explícito.** El frame acuático sólo se activa cuando el cliente ha recibido que Clinging/Reorientation controla la física; los fixtures publican ese ownership igual que gameplay real.

## Fase A — locomoción WASD

Implementado:

- reutilizar exactamente el delta que producirían vanilla + Gravity Changer para conocer magnitud y coeficientes forward/strafe;
- eliminar su componente sobre el eje de gravedad, porque la vertical acuática tiene boundary separado;
- descomponer la parte tangencial en `forward` y `left` físicos;
- reconstruirla sobre `cameraForward` y `cameraLeft` completos, sin proyectarlos a ningún plano;
- cancelar `Entity.moveRelative` sólo para el jugador local en agua cuando Clinging/Reorientation controla la física.

## Fase B — presentación de cámara

Implementado:

- adquisición de soporte mediante probe volumétrico vía `LandingSurfaces`, manteniendo agua fuera del concepto de soporte;
- libre: objetivo visual world-up;
- soportado: objetivo visual definido por el soporte/gravedad efectiva;
- corrección de roll sobre quaternion, preservando la dirección de mirada;
- debounce corto de soporte y liberación;
- liberación continua del ownership acuático al salir del estado correspondiente.

## Tests TM cubiertos

- EAST con pitch real: W sigue `cameraForward` y A sigue `cameraLeft` mediante input de cliente real;
- Space/Shift siguen siendo mundo `+Y/-Y` con gravedad lateral;
- cambio de gravedad mientras se nada libre conserva world-up;
- soporte EAST sumergido hace converger camera-up al soporte;
- retirar soporte devuelve continuamente a world-up sin cambiar gravedad lógica;
- debounce de adquisición/liberación evita flapping;
- capturas cliente para libre/support/release;
- los client GameTests nuevos están registrados explícitamente en `fabric.mod.json`, evitando el falso verde descubierto durante el sprint.

## Evidencia de cierre

El primer gate completo y honesto es GitHub Actions **#903**, commit `39f3a364c744376583a745fe549c8268ff83ee73`: build/JUnit, GameTests de servidor, client GameTests registrados, snapshots, First Person, Scale Brews y Fresh Animations finalizaron en verde.

El rojo anterior de #900 fue útil: reveló que el fixture de cliente sólo había sincronizado el atributo de gravedad EAST, pero no el ownership de Clinging. Se corrigieron `WaterCameraFrameClientGameTest` y `WaterMovementClientGameTest` para publicar el estado como lo hace gameplay real y esperar a `controlsPhysics(clientPlayer)` antes de medir.

## Gate de cierre

**CUMPLIDO.** NAV-S04 queda cerrado. El trabajo activo pasa a NAV-S05.