# NAV-S09 — convergencia final

Estado: **CERRADO**. Rama de trabajo: `tm/gravity-navigation-gamefeel-beta4`. Integrado en `main` para `0.1.0-beta.4`.

## Objetivo

S09 demuestra que NAV-S01…S08 converge en un único producto coherente: código, tests, documentación y compatibilidad describen la misma semántica de `0.1.0-beta.4`. No añade una nueva mecánica.

## Gates de código cerrados

- matriz completa verde: build/JUnit, server GameTests, client GameTests, First Person, Scale Brews server/client, Fresh Animations y snapshots;
- ningún breadcrumb de mascota permanece en runtime;
- ningún goal vanilla reachable es secuestrado por el planner gravitatorio;
- owner airborne sigue siendo objetivo de mascota mediante anchor filtrado/proyección tangencial;
- follow/chase/flee usan la misma locomoción genérica, sin listas por especie;
- daño sigue siendo coste y geometría imposible sigue siendo veto;
- reacción sigue siendo finita, dependiente de `MOVEMENT_SPEED` base y nunca instantánea;
- presupuestos S08 acotan planificación sin matar goals vivos;
- un salto breve de la navegación vanilla durante `APPROACH` conserva route/intent durante como máximo 20 ticks sin replanificar ni permitir commit aéreo; una pérdida de soporte más larga falla la arista en vez de secuestrar el mob indefinidamente;
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
- beta.4 se publica por la tubería de release exacta desde `main`.

### VALIDATION / CHANGELOG / diseño histórico

- VALIDATION contiene evidencia de sprints, rojos útiles y separación entre automatización y QA manual;
- no se afirma ningún porcentaje de rendimiento beta.4 sin perfilado reproducible de modpack real;
- CHANGELOG contiene la sección fechada `0.1.0-beta.4` con gameplay, IA, agua/landing/aerodinámica, eficiencia y el hardening de `APPROACH`;
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
- El primer HEAD documental de cierre (`df2f0051024b37d6a05beebc441b27987c963313`) produjo un rojo útil en **#1040 / run `35114000226`** sólo en Scale Brews server: el wolf real seguía con `navDone=false` al tick 120 y el fixture exigía commit exactamente en ese tick. El mismo código había pasado esa lane en #1039 y baseline server/client/First Person seguían verdes.
- El primer hardening del fixture (`c402d03be9188e22b4b06e2c6e217ef0e539125d`) reveló en **#1041 / run `35115667401`** que el problema no era sólo el instante 120: el lobo podía seguir sin commit al agotar 180 ticks. El diagnóstico del executor mostró que un salto vanilla de `PathNavigation` durante `APPROACH` hacía fallar `planningContext`, borraba el plan y devolvía el tick a `FollowOwnerGoal`, permitiendo un bucle approach→jump→clear→replan. El fix mantiene ownership/ruta hasta 20 ticks de pérdida transitoria de soporte en ambos executors, no ejecuta planning ni commit mientras están airborne y falla/cooldown la arista si la pérdida deja de ser transitoria.
- **#1042 / run `35117011829`** no alcanzó a evaluar el fix de comportamiento: al añadir inicialmente dos GameTests separados de la regresión, las fixtures paralelas consumieron slots adicionales del presupuesto regional/global en el mismo tick y el gate existente de owner aéreo no obtuvo token. La regresión se integró en dos tests ya existentes que de todos modos solicitaban esos planes, devolviendo el número de invocaciones del planner al baseline anterior sin resetear ni saltarse el budget.
- HEAD técnico `5a294ad4daa448b184388e35616eeb3e7357341a`: **Build and test #1043 / run `35117601744`**, verde completo en todas las lanes, incluida Scale Brews server que había destapado la regresión del lobo.
- El primer release candidate `a98c64e935c03a14c3fa1ac6fe0a84a1f8e0b009` produjo **#1044 / run `35121970627`** rojo sólo porque el test de IA real agotó `maxTicks=180` aún con navegación viva (`navDone=false`); no hubo cambio de producción respecto a #1043. Los gates deterministas de frontera/revalidación/ownership ya fijan la semántica exacta, así que el test programado se clasificó correctamente como integración eventual y su ventana se amplió a **300 ticks**, sin relajar ninguna aserción física.
- Release candidate final `215f8ce148886851f4014b6b4b028180bd2829b1`: **Build and test #1045 / run `35122645574`**, verde completo en localización, build/JUnit, server GameTests, cliente base, First Person, Scale Brews server/client, Fresh Animations y snapshots.
- `main` fue avanzado por fast-forward a `215f8ce148886851f4014b6b4b028180bd2829b1`; este commit documental posterior registra la integración y genera el `push` de `main` que alimenta el gate final de publicación.

## Cierre

NAV-S09 queda cerrado e integrado en `main`. `0.1.0-beta.4` sólo se publica desde el artefacto exacto de un `Build and test` verde de `main`; el workflow de release no recompila para publicar.
