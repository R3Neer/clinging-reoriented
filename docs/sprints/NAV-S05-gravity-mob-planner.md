# NAV-S05 — planner gravitatorio general de mobs

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Frontera del sprint

S05 construye locomoción estratégica reutilizable para cualquier mob con capacidad legítima de Clinging/Reorientation. No conoce dueño, enemigo ni miedo: recibe un estado físico real, un objetivo abstracto y devuelve `WALK`, `TRANSITION` o `NO_PLAN` sin mutar el mob.

S06 consume esta capa desde follow/chase/flee. La separación es deliberada: S05 decide **cómo moverse físicamente**; los goals deciden **por qué quieren ir allí**.

## Invariantes de producción

1. **Evaluar no ejecuta.** Ninguna consulta cambia gravedad, posición, velocidad, ownership, `airUsed` ni navegación viva.
2. **Capacidades reales.** Sin efecto compatible no hay transición. Clinging gastado en aire no obtiene otro giro. Reorientation conserva su semántica propia.
3. **Ownership ajeno es frontera.** Un mob con gravedad externa no puede ser reclamado por el planner.
4. **Una sola física.** La trayectoria usa `TrajectoryPrediction` + `AirMotion` + `LandingSurfaces.sweep`, igual que el landing del jugador.
5. **Volumen real.** Se simula el AABB del mob en la orientación candidata, incluida recolocación centro-a-centro cuando corresponde.
6. **Primer contacto manda.** Un choque lateral/bloqueante invalida la maniobra aunque exista un soporte mejor detrás.
7. **Desconocido no es aire.** Mundo fuera de límites, world border o chunks no disponibles invalidan el forecast.
8. **Landing utilizable.** Tocar una cara no basta: el volumen terminal debe caber y debe existir al menos una salida tangencial corta.
9. **Daño es coste, no veto.** La severidad del impacto se penaliza de forma no lineal respecto a la vida actual. Una ruta dañina puede seguir existiendo si la situación justifica asumirla.
10. **Robustez importa.** Más salidas tangenciales estables reducen fragilidad/coste.
11. **Horizonte acotado.** Ausencia de contacto dentro del horizonte es `NO_LANDING_IN_HORIZON`, no aire infinito.
12. **Vanilla primero.** El pathfinding ordinario decide WALK antes de gastar simulaciones gravitatorias.

## S05-A — evaluador físico de transición

`MobGravityPlanner` evalúa la maniobra completa:

**cambio de gravedad → vuelo real → primer contacto → soporte habitable**.

Entrada principal:

- mob vivo y no montado;
- gravedad objetivo cardinal;
- horizonte finito.

Salida aceptada:

- posición de lanzamiento efectiva;
- gravedad objetivo;
- contacto terminal;
- ETA;
- AABB de impacto;
- distancia de caída vanilla-equivalente;
- robustez de salida `[0,1]`;
- coste de riesgo;
- coste físico base.

Rechazos explícitos incluyen `NO_CAPABILITY`, `FOREIGN_GRAVITY`, `CAPABILITY_SPENT`, `INCOMPATIBLE_CONTEXT`, `UNCHANGED`, `NO_SPACE`, `UNKNOWN_GEOMETRY`, `INVALID_TRACE`, `NO_LANDING_IN_HORIZON`, `BLOCKING_CONTACT`, `STALE_CONTACT` y `TRAPPED_LANDING`.

`evaluateGroundedLaunch` evalúa además desde una **posición futura soportada** con velocidad estabilizada cero, sin mover al mob. Eso permite pronosticar el giro desde un punto al que primero se llegará caminando.

## Coste físico

S05-A produce un coste neutral basado en:

- tiempo de vuelo;
- penalización por usar un giro;
- recolocación mínima;
- fragilidad del landing;
- daño vanilla-equivalente con crecimiento fuertemente no lineal respecto a la vida actual.

La política de objetivo se añade por encima.

## S05-B — path táctico + región de lanzamiento acotada

S05-B reutiliza Gravity Changer como capa táctica. La dependencia ya sustituye `GroundPathNavigation` por `DirectionalGroundPathNavigation` bajo gravedad no-DOWN y expone las conversiones correctas entre posición de entidad y nodo.

### Objetivo abstracto

`MobGravityLocalPlanner.Goal` aporta:

- `focus()` — punto mundial que guía el path;
- `satisfied(position, gravity)` — criterio real de llegada;
- `heuristic(position, gravity)` — coste restante no negativo.

### Pureza del pathfinding

`PathNavigation.createPath(...)` modifica metadatos internos, así que la planificación usa una **navegación espejo efímera** y nunca consulta `createPath` sobre la navegación viva.

El `reachRange` estratégico es **0**. La tolerancia de gameplay pertenece a `Goal.satisfied()`, no al booleano indulgente de `Path.canReach()`.

### Por qué una región y no un único punto

El último nodo de un path parcial es una frontera útil, pero no siempre es el mejor lanzamiento. Puede estar demasiado cerca de geometría, mientras uno o dos nodos anteriores producen una transición más robusta. El planner no escanea el mundo: considera sólo una pequeña cola del **mismo path táctico**.

### Algoritmo acotado actual

1. Proyectar `focus` al plano de movimiento actual.
2. Pedir **una sola** ruta a la navegación espejo.
3. Si la ruta satisface realmente el objetivo, devolver `WALK`.
4. Si el mob no tiene capacidad gravitatoria, devolver `NO_PLAN` sin ejecutar forecasts.
5. Si hace falta gravedad, tomar como candidatos los últimos **hasta cuatro nodos** del path parcial. Si no hay path, sólo la posición actual.
6. Desde cada candidato evaluar las cinco gravedades distintas de la actual mediante `evaluateGroundedLaunch`.
7. Coste = caminata hasta lanzamiento + coste físico + heurística del landing.
8. Elegir el candidato finito mínimo.
9. El `walkPath` del resultado se **trunca al nodo de lanzamiento elegido**, de modo que el executor jamás camina más allá de su propia frontera.

Presupuesto máximo de una comparación bloqueada con capacidad:

**4 lanzamientos × 5 gravedades = 20 forecasts físicos.**

Eso ocurre sólo después de que la navegación ordinaria haya fallado. Un mob sin efecto gasta **0 forecasts** gravitatorios.

## S05-C — grafo de soportes online

No se construye un A* global sobre estados hipotéticos. Gravity Changer obtiene origen/gravedad desde el mob real, y clonar o mutar entidades para pathfinding especulativo sería frágil y caro.

El grafo se recorre con receding horizon:

1. soporte estable real = nodo raíz;
2. S05-B elige WALK/TRANSITION local;
3. el executor llega al lanzamiento;
4. revalida justo antes del commit;
5. una transición real ocurre;
6. la física gobierna el vuelo;
7. tras soporte estable confirmado, se replantea desde la nueva realidad.

### Memoria de aristas

`ManeuverKey(currentGravity, frontierNode, targetGravity)` identifica una **maniobra concreta desde un lanzamiento concreto**.

Excluir una clave no prohíbe toda una dirección: si EAST desde un nodo falla, el planner puede probar EAST desde otro nodo cercano de la región de lanzamiento. Sólo tras agotar esas opciones locales pasará a otra dirección. Esto evita tanto bucles como castigos excesivamente amplios.

S06 mantiene una memoria pequeña y caducable de maniobras fallidas/no-progresivas.

## Gates cerrados S05

- WALK alcanzable permanece vanilla y no gasta forecasts;
- gravedad lateral usa las convenciones `nodePosition/entityPosition` correctas;
- path parcial conserva su endpoint táctico cuando no hay capacidad;
- mobs sin efecto gastan 0 forecasts gravitatorios;
- región de lanzamiento puede seleccionar un nodo anterior cuando una transición preferida no existe desde el endpoint terminal;
- approach path queda truncado a la frontera elegida;
- máximo 20 forecasts tras fallo ordinario;
- lanzamiento hipotético no muta al mob;
- `ManeuverKey` es estable bajo jitter sub-bloque dentro del mismo nodo;
- excluir una arista concreta permite otra arista local antes de abandonar esa dirección;
- WALK no queda interferido por memoria de maniobras gravitatorias.

S05 queda cerrado. S06 integra esta locomoción con follow/chase/flee y S07 añade target tracking en vuelo, cambios dinámicos del mundo y reacción finita.
