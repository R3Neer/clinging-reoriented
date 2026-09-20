# NAV-S03 — free-fall → landing anticipado

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Problema observado

La predicción de landing estaba acoplada a la ventana visual final: `LandingPrediction.MAX_TICKS == LandingTiming.PRESENTATION_TICKS == 10`. El servidor sólo descubría una superficie cuando ya tocaba comenzar a rotar, de modo que adquisición, confianza y presentación eran el mismo evento.

## Contratos

1. **Adquisición ≠ approach ≠ commitment.** La física puede adquirir un candidato con bastante antelación. Desde beta.6 el body puede anticipar antes que la cámara y el input sólo queda bloqueado durante el commitment final.
2. **Una sola física.** Toda predicción pasa por `TrajectoryPrediction` + `AirMotion` + `LandingSurfaces.sweep`; no existe un raycast paralelo ni una ecuación aproximada.
3. **Primer contacto manda.** Un obstáculo no-soporte anterior invalida la trayectoria. Nunca se mira a través de geometría para encontrar un suelo posterior.
4. **Volumen real.** La predicción barre el AABB completo del jugador.
5. **Candidato persistente.** Se conserva identidad de superficie, gravedad, ETA, estabilidad y misses breves para evitar flapping de adquisición.
6. **Nada de compromiso obsoleto.** Un candidato retenido por histéresis no puede iniciar BODY_LANDING/camera landing si la predicción del tick actual no lo confirma.
7. **Revalidación.** Una superficie adquirida debe seguir existiendo bajo el mismo frame gravitatorio. Cambio de gravedad o revisión inválida rompe la adquisición.
8. **Touchdown sincronizado sin captura prematura.** CLEAR puede anticipar body hasta 10 ticks pero no compromete cámara/input hasta <=5; AMBIGUOUS necesita dos confirmaciones y usa 4/3 ticks para body/commit.
9. **Continuidad ante invalidación.** Si un LAND comprometido deja de ser válido, el cliente vuelve al HOLD de vuelo previo mediante RECOVER_HOLD en 4 ticks / 200 ms; no congela indefinidamente el quaternion parcial ni hace snap.
10. **Touchdown real manda sobre la predicción.** Si el soporte se adquiere antes de la ETA prevista, se libera inmediatamente cualquier LAND/HOLD residual.
11. **Trabajo acotado.** El horizonte largo se mantiene dentro del límite del predictor y Gravity Fall reutiliza el candidato calculado por `LandingState`; no hace un segundo barrido independiente del mundo en el mismo tick.

## Calibración cerrada

- `ACQUISITION_TICKS = 40` (2 s a 20 TPS).
- `LandingTiming.PRESENTATION_TICKS = 10` sigue siendo el máximo de anticipación corporal CLEAR.
- Política beta.6: GRAZE <=12% de velocidad normal/total; CLEAR >=30%; banda intermedia y velocidad total <0,12 bloques/tick = AMBIGUOUS.
- CLEAR: BODY_LANDING <=10 ticks; LANDING_COMMITTED <=5 ticks.
- AMBIGUOUS: dos confirmaciones; BODY_LANDING <=4 ticks; LANDING_COMMITTED <=3 ticks.
- Un GRAZE conserva durante un tick la identidad exacta de superficie/gravedad para que el primer `onGround` coincidente no se convierta por sí solo en soporte; si persiste, el soporte real gana.
- La adquisición registra estabilidad desde el primer tick confirmado y tolera como máximo un miss transitorio fuera de la ventana visual.
- Dentro del commitment sólo una predicción **actual** puede mantener LAND.
- Cancelar LAND recupera el HOLD previo en 4 ticks / 200 ms.

## Implementación

- `296a2e126eaf20d8f35024e0aad7784b50f38f12`: adquisición anticipada, candidato persistente e integración de una sola predicción por tick.
- `d63ddd08edec5d14e34ca91a5e5ed41409f5ef89`: ETA cruzando servidor → cliente con `visual_land_v2`; duración visual sincronizada con contacto previsto.
- `1cc87dcecf8da61b836b3e1e5552bd9eaa747cc9`: lifecycle adversarial de LAND; invalidación cancela aunque no hubiera HOLD previo y touchdown real libera cualquier presentación residual.

## Evidencia TM

- Soporte a >10 y <=40 ticks: adquirido sin compromiso visual.
- Misma superficie al entrar en <=10 ticks: conserva identidad; CLEAR anticipa body pero sólo compromete al entrar en <=5 ticks.
- Primer contacto bloqueante: impide adquisición posterior.
- Un miss lejano: histéresis; segundo miss: limpieza.
- Candidato sólo retenido: nunca inicia BODY_LANDING.
- Gravity Fall no ejecuta un segundo `sweep` en el mismo tick; existe un contador adversarial específico.
- ETA visual: JUnit cubre 0, 3, 9.5, 10 y >10 ticks.
- Cancelación parcial: desde beta.6 el cliente parte del frame parcial exacto y recupera suavemente el HOLD previo en 200 ms.
- Run **#888 / 35075098789**: build/JUnit, 126 GameTests de servidor, cliente base, First Person, Scale Brews, Fresh Animations y validación de snapshots, todo verde.

## Cierre

NAV-S03 queda cerrado. La dependencia pública siguiente es NAV-S04; no quedan cambios de producción pendientes en S03.


## Refinamiento beta.6 — contacto de pies con intención física

La geometría S03 no cambia: el primer contacto y la cara de pies siguen viniendo del mismo sweep volumétrico. Beta.6 añade una política local del jugador después del hit para distinguir **apoyo** de **roce** sin contaminar el predictor compartido de mobs.

Los holdouts nuevos cubren roce tangencial sin candidato/commit, memoria de un tick frente a `onGround`, persistencia que sí se convierte en soporte, ventanas CLEAR/AMBIGUOUS y recuperación visual a HOLD. La evidencia de CI final se registra en `GF-S06-landing-contact-policy.md`.
