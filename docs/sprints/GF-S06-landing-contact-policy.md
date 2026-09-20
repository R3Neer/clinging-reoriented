# GF-S06 — landing contact intent

Estado: **CERRADO FUNCIONAL / CANDIDATO DE RELEASE beta.6**. Rama: `tm/landing-contact-intent-beta6`.

## Problema

La geometría de landing distinguía correctamente la cara de los pies de hombro/lateral, pero cualquier primer contacto que incluyese la normal de soporte podía adquirir autoridad de landing. Un roce casi tangencial o un empate de esquina podía entrar en la ventana LAND, bloquear nuevos giros y, si Minecraft levantaba `onGround`, convertir el roce en soporte canónico.

## Política beta.6

La geometría compartida no cambia. `TrajectoryPrediction`, `AirMotion`, `LandingSurfaces.sweep` y `SweptAabb` siguen resolviendo el primer contacto volumétrico. Después del hit, sólo el landing local del jugador aplica `LandingPolicy`:

- **GRAZE**: velocidad normal / total <= 0,12;
- **CLEAR**: velocidad normal / total >= 0,30;
- **AMBIGUOUS**: banda intermedia;
- velocidad total < 0,12 bloques/tick también es AMBIGUOUS.

CLEAR puede iniciar BODY_LANDING con ETA <=10, pero `LANDING_COMMITTED` espera a <=5. AMBIGUOUS exige dos predicciones actuales consecutivas, inicia body con ETA <=4 y compromete a <=3.

Un GRAZE no crea candidato. La identidad exacta `SurfaceKey + gravity` se conserva un tick adicional para que el primer `onGround` coincidente no recargue Clinging ni canonicalice el frame. Si el soporte persiste después de ese tick, vuelve a mandar el soporte real.

## Cancelación visual

`visual_cancel_v2` sustituye el booleano de “freeze current” por recuperación del HOLD. `VisualTransitions.RECOVER_HOLD` empieza en el quaternion parcial exacto y vuelve al frame previo de vuelo en 4 ticks / 200 ms. Transfer/lifecycle sigue soltando ownership de inmediato.

## Fronteras

- hombro/lateral sigue siendo non-support;
- primer contacto sigue mandando;
- no se añade botón de aterrizaje ni inferencia de intención desde teclas;
- el planner de mobs sigue usando el predictor físico compartido sin los umbrales de confort del jugador;
- el flag vanilla `onGround` por sí solo no crea soporte semántico.

## Cobertura

- `LandingPolicyTest`: GRAZE/AMBIGUOUS/CLEAR, baja velocidad, ventanas de body/commit;
- `LandingStateGameTests.tangentialFeetGrazeDoesNotBecomeLandingCandidate`;
- `LandingStateGameTests.predictedGrazeSuppressesFirstGroundFlagButPersistentFeetSupportWins`;
- holdouts de adquisición 40 ticks, first-contact blocking y delayed commit;
- `ReorientationTests.chargeSurvivesEffectRefreshAndFalseGround`: un ground flag sin geometría real no recarga ni bloquea como suelo;
- snapshot adversarial `s05-cancelled-landing-recovers-hold`.

## Historia roja útil

- **#1065 / 35509766626**: seis holdouts expresaban la política antigua (commit <=10 o fixtures sin velocidad hacia soporte) y un GameTest de Gravity Charge carecía de ticket de simulación estable. Se corrigieron fixtures/tests; no se relajó producción.
- **#1066 / 35509962888**: quedó sólo la calibración del fixture de commit <=5; se acercó el soporte sin alterar el umbral.
- **#1068 / 35510197126**: quedó sólo el contrato histórico que esperaba `BLOCKED` por un `onGround` forjado. Beta.6 exige soporte geométrico real, por lo que el resultado correcto con Clinging gastado es `AIR_CHANGE_USED`.

## Gate

- HEAD funcional: `163722f0db55362b1fb4f1e5d5099a9ac76cc855`.
- **Build and test #1069 / run `35510393577`: SUCCESS** en build/JUnit, server GameTests, cliente base, First Person, Scale Brews server/client, Fresh Animations y snapshots.
- El commit de release-prep añade versión, documentación y publisher; debe repetir la matriz completa.
- La publicación sigue el contrato habitual: sólo el artefacto del CI verde del `main` exacto puede crear `v0.1.0-beta.6`; no se recompila para release.
