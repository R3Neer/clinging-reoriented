# NAV-S09 — convergencia final

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Objetivo

S09 demuestra que NAV-S01…S08 converge en un único producto coherente: código, tests, documentación y compatibilidad describen la misma semántica beta.4 unreleased. No añade una nueva mecánica.

## Gates de código cerrados

- matriz completa verde: build/JUnit, server GameTests, client GameTests, First Person, Scale Brews server/client, Fresh Animations y snapshots;
- ningún breadcrumb de mascota permanece en runtime;
- ningún goal vanilla reachable es secuestrado por el planner gravitatorio;
- owner airborne sigue siendo objetivo de mascota mediante anchor filtrado/proyección tangencial;
- follow/chase/flee usan la misma locomoción genérica, sin listas por especie;
- daño sigue siendo coste y geometría imposible sigue siendo veto;
- reacción sigue siendo finita, dependiente de `MOVEMENT_SPEED` base y nunca instantánea;
- presupuestos S08 acotan planificación sin matar goals vivos;
- no quedan rutas especiales capaces de girar remotamente antes de llegar a una frontera de lanzamiento;
- el estado servidor legado `gravityFallForwardIntent` fue retirado: `gravity_fall_look_v1` conserva el float sólo por compatibilidad de layout, el cliente envía 0 y beta.4 lo ignora.

## Convergencia documental

### README / GUIDE

Quedan alineados con el runtime actual:

- Gravity Fall no tiene steering especial por W;
- postura persistente + mirada + estabilización débil + drag transversal anisótropo;
- landing con 40 ticks de adquisición y <=10 ticks de presentación final;
- predicción refrescada desde velocidad real actual, sin adivinar futura mirada/postura;
- agua camera-relative para WASD, Space/Shift mundo ±Y, world-up en nado libre y support-up con soporte real;
- mascotas history-free sin breadcrumbs y owner airborne con tracking filtrado;
- mobs con efecto pueden usar transiciones gravitatorias para perseguir/escapar cuando vanilla no resuelve;
- Reorientation puede reaccionar en vuelo tras latencia y forecast legal; Clinging gastado no obtiene giro extra.

### ARCHITECTURE / CONFIGURATION / COMPATIBILITY

Los tres documentos reflejan:

- causalidad `look -> body attitude -> anisotropic aerodynamics -> real velocity/trajectory`;
- seam compartido `TrajectoryPrediction` + `AirMotion` + `LandingSurfaces`;
- `LandingPrediction.ACQUISITION_TICKS = 40`;
- región de lanzamiento S05: hasta 4 nodos × 5 direcciones (`<=20` forecasts);
- `MobGravityNavigation`, `PetGravityFollow`, `MobFlightMonitor/Reaction/Reactor` y `MobGravityPlanningBudget`;
- budgets de 32 planes globales/tick y 4 por región 64×64/tick;
- frame acuático separado de gravedad lógica;
- ownership vanilla/Gravity Changer/Clinging/optional mods actualizado;
- beta.3 sigue siendo la última versión publicada y beta.4 permanece unreleased.

### VALIDATION / CHANGELOG / diseño histórico

- VALIDATION contiene evidencia de sprints, rojos útiles y separación entre automatización y QA manual;
- no se afirma ningún porcentaje de rendimiento beta.4 sin perfilado reproducible de modpack real;
- CHANGELOG `Unreleased` recoge gameplay, IA, agua/landing/aerodinámica y eficiencia;
- alpha.15 conserva breadcrumbs y alpha.14 conserva W air-diving únicamente como historia fechada;
- `docs/design/README.md` marca los cinco documentos de diseño pre-implementación como archivo histórico y apunta a los documentos normativos actuales.

## Auditoría legacy obligatoria

Resultado de las búsquedas de cierre:

- `GravityBreadcrumbs.java` y su test están eliminados; no hay replay de owner-turns en runtime;
- las menciones actuales de `breadcrumb(s)` en documentación normativa describen explícitamente su retirada; las restantes históricas están fechadas;
- W air-diving / steering de 6° sólo permanece como historia antigua o negación explícita del comportamiento actual;
- ARCHITECTURE ya no describe `LandingPrediction.MAX_TICKS = 10`; el contrato vigente es adquisición 40 / presentación 10;
- el planner actual no se documenta como “max five forecasts”: el límite real es hasta 20 por plan local;
- owner airborne ya no aparece como razón para dejar de seguir;
- CONFIGURATION dejó de describir beta.2 y sus constantes antiguas;
- COMPATIBILITY dejó de atribuir a Clinging un subsistema de air-diving vigente;
- el wire field `forwardIntent` se conserva sólo para no romper `gravity_fall_look_v1`; no existe ya estado de gameplay servidor asociado.

## Evidencia de cierre

- S07 funcional: `370c2f4c8a40f87b9d89e1895cf0df4df8ba68bd`, **run `35103896552`** verde.
- HEAD funcional de convergencia antes de esta pasada documental: `b2de88e83e1e64416288d220c8d86d52aeca014d`, **run `35105097956`** verde.
- HEAD tras convergencia documental y limpieza de estado legado: `48311e9d994a0335c15242491328ac4fbfd82188`, **Build and test #1039 / run `35112406795`**, verde en localización, build/JUnit, server GameTests, cliente base, First Person, Scale Brews server/client, Fresh Animations y snapshots.

## Cierre

NAV-S09 queda cerrado. Este cierre no cambia versión, etiqueta, `main` ni publica prerelease. `0.1.0-beta.3` sigue siendo la última versión publicada; la campaña beta.4 queda lista como rama convergida para una futura decisión explícita de integración/release.
