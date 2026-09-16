# NAV-S03 — free-fall → landing anticipado

Estado: **EN PROGRESO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Problema observado

La predicción de landing está acoplada hoy a la ventana visual final: `LandingPrediction.MAX_TICKS == LandingTiming.PRESENTATION_TICKS == 10`. El servidor sólo descubre una superficie cuando ya toca comenzar a rotar, de modo que adquisición, confianza y presentación son el mismo evento.

## Contratos antes de producción

1. **Adquisición ≠ presentación.** La física puede adquirir un candidato con bastante antelación; el cuerpo/cámara sólo entra en transición durante los últimos `PRESENTATION_TICKS`.
2. **Una sola física.** Toda predicción sigue pasando por `TrajectoryPrediction` + `AirMotion` + `LandingSurfaces.sweep`; no se introduce un raycast paralelo ni una ecuación aproximada.
3. **Primer contacto manda.** Un obstáculo no-soporte anterior invalida la trayectoria. Nunca se mira a través de geometría para encontrar un suelo posterior.
4. **Volumen real.** La predicción sigue barriendo el AABB completo del jugador.
5. **Candidato persistente.** Se conserva identidad de superficie, gravedad, ETA, estabilidad y misses breves para evitar flapping de adquisición.
6. **Nada de compromiso obsoleto.** Un candidato retenido por histéresis no puede iniciar BODY_LANDING/camera landing si la predicción del tick actual no lo confirma.
7. **Revalidación.** Una superficie adquirida debe seguir existiendo bajo el mismo frame gravitatorio. Cambio de gravedad o revisión inválida rompe la adquisición.
8. **Touchdown sincronizado.** Cuando la ETA cruza la ventana final, la duración visual usa la ETA restante y termina en el contacto previsto, no después.
9. **Continuidad ante invalidación.** Si el candidato deja de ser válido durante la presentación, se cancela a la postura visible actual y se vuelve a free-fall sin snap.
10. **Trabajo acotado.** El horizonte largo se mantiene dentro del límite del predictor y Gravity Fall reutiliza el candidato calculado por `LandingState`; no hace un segundo barrido independiente del mundo en el mismo tick.

## Valores iniciales de calibración

- `PRESENTATION_TICKS = 10` se conserva.
- `ACQUISITION_TICKS = 40` (2 s a 20 TPS): suficiente para estabilizar una superficie futura sin convertir cada jugador en un planificador balístico de medio mapa.
- La adquisición registra estabilidad desde el primer tick confirmado y tolera como máximo un miss transitorio fuera de la ventana visual.
- Dentro de la ventana visual sólo una predicción **actual** puede iniciar o mantener el compromiso.

## Tests TM exigidos

- Un soporte a >10 y <=40 ticks se adquiere pero **no** compromete presentación.
- El mismo soporte, al entrar en <=10 ticks, compromete sin cambiar de identidad.
- Un contacto bloqueante anterior sigue impidiendo adquirir el soporte posterior.
- Un miss transitorio fuera de ventana no destruye inmediatamente el candidato; un segundo miss sí.
- Un candidato retenido sólo por histéresis nunca inicia BODY_LANDING.
- Gravity Fall consume el candidato compartido y deja de ejecutar su propia segunda predicción.
- Invalidation/revision/gravity mismatch limpian el candidato y preservan continuidad de presentación.

## Gate de cierre

JUnit + GameTests servidor + cliente/snapshots + lanes opcionales relevantes deben quedar verdes antes de abrir NAV-S04.