# NAV-S03 — free-fall → landing anticipado

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Problema observado

La predicción de landing estaba acoplada a la ventana visual final: `LandingPrediction.MAX_TICKS == LandingTiming.PRESENTATION_TICKS == 10`. El servidor sólo descubría una superficie cuando ya tocaba comenzar a rotar, de modo que adquisición, confianza y presentación eran el mismo evento.

## Contratos

1. **Adquisición ≠ presentación.** La física puede adquirir un candidato con bastante antelación; el cuerpo/cámara sólo entra en transición durante los últimos `PRESENTATION_TICKS`.
2. **Una sola física.** Toda predicción pasa por `TrajectoryPrediction` + `AirMotion` + `LandingSurfaces.sweep`; no existe un raycast paralelo ni una ecuación aproximada.
3. **Primer contacto manda.** Un obstáculo no-soporte anterior invalida la trayectoria. Nunca se mira a través de geometría para encontrar un suelo posterior.
4. **Volumen real.** La predicción barre el AABB completo del jugador.
5. **Candidato persistente.** Se conserva identidad de superficie, gravedad, ETA, estabilidad y misses breves para evitar flapping de adquisición.
6. **Nada de compromiso obsoleto.** Un candidato retenido por histéresis no puede iniciar BODY_LANDING/camera landing si la predicción del tick actual no lo confirma.
7. **Revalidación.** Una superficie adquirida debe seguir existiendo bajo el mismo frame gravitatorio. Cambio de gravedad o revisión inválida rompe la adquisición.
8. **Touchdown sincronizado.** Cuando la ETA cruza la ventana final, la duración visual usa la ETA restante y termina en el contacto previsto, no después.
9. **Continuidad ante invalidación.** Si el candidato deja de ser válido durante la presentación, se cancela a la postura visible actual y se vuelve a free-fall sin snap.
10. **Touchdown real manda sobre la predicción.** Si el soporte se adquiere antes de la ETA prevista, se libera inmediatamente cualquier LAND/HOLD residual.
11. **Trabajo acotado.** El horizonte largo se mantiene dentro del límite del predictor y Gravity Fall reutiliza el candidato calculado por `LandingState`; no hace un segundo barrido independiente del mundo en el mismo tick.

## Calibración cerrada

- `PRESENTATION_TICKS = 10`.
- `ACQUISITION_TICKS = 40` (2 s a 20 TPS).
- La adquisición registra estabilidad desde el primer tick confirmado y tolera como máximo un miss transitorio fuera de la ventana visual.
- Dentro de la ventana visual sólo una predicción **actual** puede iniciar o mantener el compromiso.
- La duración de LAND se calcula a partir de la ETA restante y se limita a la ventana de 10 ticks.

## Implementación

- `296a2e126eaf20d8f35024e0aad7784b50f38f12`: adquisición anticipada, candidato persistente e integración de una sola predicción por tick.
- `d63ddd08edec5d14e34ca91a5e5ed41409f5ef89`: ETA cruzando servidor → cliente con `visual_land_v2`; duración visual sincronizada con contacto previsto.
- `1cc87dcecf8da61b836b3e1e5552bd9eaa747cc9`: lifecycle adversarial de LAND; invalidación cancela aunque no hubiera HOLD previo y touchdown real libera cualquier presentación residual.

## Evidencia TM

- Soporte a >10 y <=40 ticks: adquirido sin compromiso visual.
- Misma superficie al entrar en <=10 ticks: conserva identidad y compromete.
- Primer contacto bloqueante: impide adquisición posterior.
- Un miss lejano: histéresis; segundo miss: limpieza.
- Candidato sólo retenido: nunca inicia BODY_LANDING.
- Gravity Fall no ejecuta un segundo `sweep` en el mismo tick; existe un contador adversarial específico.
- ETA visual: JUnit cubre 0, 3, 9.5, 10 y >10 ticks.
- Cancelación parcial: el cliente conserva el frame visible exacto.
- Run **#888 / 35075098789**: build/JUnit, 126 GameTests de servidor, cliente base, First Person, Scale Brews, Fresh Animations y validación de snapshots, todo verde.

## Cierre

NAV-S03 queda cerrado. La dependencia pública siguiente es NAV-S04; no quedan cambios de producción pendientes en S03.
