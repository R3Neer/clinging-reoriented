# NAV-S00 — campaña TM: física, landing, agua y navegación gravitatoria

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

Base: `main` tras el merge de compatibilidad Alchemical Leather. La campaña integró los diseños previos de `design/pet-follow-redesign` y los convirtió en implementación incremental con gates verdes por sprint.

## Resultado de la campaña

### NAV-S01 — núcleo compartido de predicción física

`TrajectoryPrediction` + `AirMotion` + `LandingSurfaces` constituyen el seam común de predicción volumétrica. El predictor avanza AABB, velocidad y gravedad reales, conserva primer contacto/ETA/soporte y falla cerrado ante geometría desconocida.

Landing y navegación gravitatoria de mobs consumen esta misma base. La aerodinámica corporal de Gravity Fall modifica la **velocidad real** antes de predicciones posteriores; el predictor se refresca desde ese estado medido cada tick y no intenta adivinar inputs futuros de mirada/postura.

### NAV-S02 — aerodinámica corporal de Gravity Fall

Cerrado con causalidad `mirada -> actitud corporal -> drag anisótropo -> trayectoria`:

- actitud corporal persistente en world-space;
- deadzone cervical de 35°;
- seguimiento de mirada limitado a 7,5°/tick;
- estabilización débil por velocidad de 1,25°/tick;
- drag transversal adicional del 2,5%/tick;
- sin steering especial por W, sin thrust y sin lift.

### NAV-S03 — free-fall -> landing anticipado

Cerrado separando adquisición y presentación:

- `ACQUISITION_TICKS = 40`;
- presentación final <=10 ticks;
- candidato persistente con identidad/histéresis;
- primer contacto autoritativo;
- invalidación continua sin snap;
- touchdown real libera presentación residual.

### NAV-S04 — agua: controles y cámara

Cerrado con:

- WASD camera-relative, incluido pitch;
- Space/Shift mundo +Y/-Y;
- natación libre visualmente world-up;
- soporte real sumergido visualmente support-up;
- gravedad lógica separada del frame acuático;
- fluidos como frontera genérica de soporte/landing/Gravity Fall.

### NAV-S05 — planner gravitatorio general de mobs

Cerrado con evaluación pura de maniobras soporte->soporte y planner local vanilla-first:

- AABB real y primer contacto;
- geometría imposible = veto;
- daño/riesgo = coste fuerte no lineal;
- una navegación espejo por plan;
- hasta 4 nodos de lanzamiento × 5 gravedades alternativas = `<=20` forecasts físicos;
- `ManeuverKey` por arista concreta y memoria acotada.

### NAV-S06 — integración de objetivos de IA

Cerrado para mascotas y mobs generales:

- pet follow history-free, sin breadcrumbs;
- owner airborne sigue siendo objetivo mediante tracking filtrado/proyección tangencial, nunca una orden remota de giro;
- `FollowOwnerGoal`, melee, flee/avoid y posiciones mantienen la intención vanilla;
- wake adapters sólo exponen intents que vanilla abortaría antes de `moveTo`;
- approach/revalidate/commit/landing/recovery mantienen ownership explícito;
- teleport de mascota es fallback validado, no locomoción oculta.

### NAV-S07 — mundo dinámico, tracking móvil y reacción

Cerrado con observación continua pero acción retardada:

- `MobFlightMonitor` sin pathfinding de superficie;
- target/world changes materiales con histéresis;
- `MobReactionTime` derivado de `MOVEMENT_SPEED` base, 2–10 ticks;
- Reorientation puede corregir sólo tras delay y forecast legal;
- Clinging gastado no obtiene un segundo giro;
- amenaza desaparecida cancela la reacción pendiente;
- obstáculo demasiado tardío produce impacto natural.

### NAV-S08 — eficiencia, presupuestos y adversarial

Cerrado con límites estructurales:

- monitor aéreo `min(20, reactionTicks + 2)`;
- landing comprometido revalidado aparte del horizonte corto;
- máximo 32 nuevas planificaciones grounded por nivel/tick;
- máximo 4 por región X/Z de 64×64/tick;
- exceso de trabajo pasa a `WAITING_PLAN` preservando intent;
- pet follow comparte el mismo presupuesto;
- memoria de maniobras fallidas limitada a 8 entradas por executor.

### NAV-S09 — convergencia final

Cerrado tras sincronizar código, tests y documentación pública:

- README/GUIDE/ARCHITECTURE/CONFIGURATION/COMPATIBILITY actualizados al comportamiento beta.4;
- VALIDATION y CHANGELOG documentan gates, rojos útiles y límites reales;
- diseños pre-implementación quedan marcados explícitamente como archivo histórico;
- breadcrumbs eliminados del runtime;
- estado servidor `gravityFallForwardIntent` eliminado; el campo homónimo permanece únicamente en el layout wire `gravity_fall_look_v1` por continuidad de protocolo y se ignora en beta.4;
- un salto breve de `PathNavigation` durante `APPROACH` conserva el plan especial durante como máximo 20 ticks sin planificar ni comprometer gravedad mientras falta soporte.

## Invariantes globales cerrados

- No inventar capacidades gravitatorias: Clinging y Reorientation conservan exactamente sus reglas.
- Ningún predictor atraviesa geometría desconocida como si fuese aire.
- El volumen corporal real manda, no raycasts puntuales.
- Player landing y transiciones de mobs comparten el mismo seam `TrajectoryPrediction`/`AirMotion`/`LandingSurfaces`.
- La aerodinámica de postura actúa sobre la velocidad real; las predicciones futuras parten de ese estado actualizado y no adivinan input futuro.
- Daño/riesgo es coste fuerte; geometría físicamente inválida es veto.
- El planner gravitatorio no sustituye pathfinding vanilla cuando éste ya resuelve el objetivo.
- El trabajo caro por tick está acotado local, regional y globalmente.
- Las transiciones visuales preservan continuidad y ownership.

## Evidencia final

El HEAD técnico final `5a294ad4daa448b184388e35616eeb3e7357341a` pasó **Build and test #1043 / run `35117601744`** completo: localización, build/JUnit, server GameTests, cliente base, First Person, Scale Brews server/client, Fresh Animations y validación de snapshots.

La campaña queda cerrada en ese HEAD. La preparación de `0.1.0-beta.4` posterior sólo cambia metadatos/documentación de release y añade el workflow de publicación; no altera gameplay. El prerelease se publica únicamente desde el artefacto del CI verde de `main`.
