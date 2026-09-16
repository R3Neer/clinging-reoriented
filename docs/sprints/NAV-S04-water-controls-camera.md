# NAV-S04 — agua: controles de cámara y frame visual

Estado: **EN PROGRESO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Problema observado

La navegación acuática está partida entre dos modelos incompatibles:

- `WorldVerticalWaterMixin` ya fuerza Space/Shift a mundo `+Y/-Y`.
- `VisualMovementMixin` se desactiva explícitamente en agua.
- Gravity Changer 26.2 redirige `Entity.moveRelative` y rota todo el vector de entrada desde coordenadas de jugador al frame de gravedad física.

Resultado: con gravedad lateral, la vertical de Space/Shift es mundial pero WASD vuelve a depender de la gravedad lógica. Además, la cámara puede conservar el frame gravitatorio mientras el jugador nada libremente, aunque el contrato de gameplay sea “agua libre = presentación world-up”.

## Contratos antes de producción

1. **Gravedad lógica ≠ frame acuático.** Entrar en agua no cambia el atributo de gravedad. Sólo cambia cómo se presentan cámara y controles.
2. **W/S = cámara forward/back.** Incluye pitch real de cámara; mirar hacia arriba/abajo debe producir la correspondiente componente vertical de natación.
3. **A/D = cámara left/right.** El strafe sigue los ejes de pantalla, no el plano tangente de la gravedad física.
4. **Space/Shift = mundo `+Y/-Y`.** Se conserva el boundary ya implementado por `WorldVerticalWaterMixin`.
5. **Sin aceleración gratis.** El remapeo de WASD es rígido: conserva la magnitud tangencial que vanilla + Gravity Changer ya habían normalizado.
6. **Natación libre = world-up.** Mientras el jugador está en agua y no tiene soporte gravitatorio estable, la presentación converge a una cámara equivalente a gravedad DOWN, sin tocar la gravedad lógica.
7. **Soporte sumergido = floor-up.** Si el AABB realmente está soportado en la dirección de gravedad actual, la presentación converge al frame de ese suelo/pared/techo.
8. **Contacto real, no flag ciego.** La futura detección de soporte visual debe usar geometría/`LandingSurfaces`, no sólo `onGround`, y debe respetar el primer contacto.
9. **Histéresis breve.** Geometría irregular no puede alternar world-up/floor-up cada tick. La entrada/salida de soporte tendrá debounce explícito y acotado.
10. **Transición continua.** Cambiar entre natación libre y soporte no hace snap visual.
11. **Gravity Fall no compite con agua.** El macroestado Gravity Fall sigue retirándose al entrar en fluido.
12. **Ownership explícito.** El frame acuático deberá tener prioridad definida frente a `VisualTransitions`; no se aceptan dos propietarios de la misma `GravityRotationAnimation` en el mismo instante.

## Fase A — locomoción WASD

Primera implementación del sprint:

- reutilizar exactamente el delta que producirían vanilla + Gravity Changer para conocer magnitud y coeficientes forward/strafe;
- eliminar su componente sobre el eje de gravedad, porque la vertical acuática tiene boundary separado;
- descomponer la parte tangencial en `forward` y `left` físicos;
- reconstruirla sobre `cameraForward` y `cameraLeft` completos, sin proyectarlos a ningún plano;
- cancelar `Entity.moveRelative` sólo para el jugador local en agua cuando Clinging/Reorientation controla la física.

Esto resuelve la mitad locomotora sin adelantar todavía el ownership visual de cámara.

## Fase B — presentación de cámara

Diseño de implementación:

- adquirir soporte con un probe volumétrico mínimo vía `LandingSurfaces`, manteniendo agua fuera del concepto de soporte;
- libre: objetivo visual world-up;
- soportado: objetivo visual definido por el soporte/gravedad efectiva;
- interpolación sobre quaternion y no sobre Euler;
- preservar la dirección de mirada al corregir roll/up;
- debounce corto de soporte y liberación;
- al salir del agua, devolver ownership al sistema seco sin discontinuidad.

## Tests TM exigidos

- EAST/NORTH/etc.: W sigue `cameraForward`, incluidos pitches positivos y negativos.
- A/D siguen `cameraLeft/right`.
- Diagonal conserva magnitud; cero input no crea impulso.
- La componente sobre el eje gravitatorio no contamina WASD acuático.
- Space/Shift siguen siendo mundo `+Y/-Y` con gravedad lateral.
- Cambio de gravedad mientras se nada libre no inclina la cámara fuera de world-up.
- Apoyarse bajo agua en suelo/pared/techo hace converger la cámara al soporte.
- Perder soporte hace volver de forma continua a world-up sin cambiar la gravedad lógica.
- Contacto intermitente de un tick no produce flapping.
- Capturas cliente para libre/support/release con invariantes numéricas vecinas.

## Gate de cierre

JUnit + GameTests servidor + cliente/snapshots + First Person + Fresh Animations + Scale Brews deben quedar verdes antes de NAV-S05.
