# NAV-S05 — planner gravitatorio general de mobs

Estado: **EN PROGRESO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Frontera del sprint

S05 construye locomoción estratégica reutilizable para cualquier mob con capacidad legítima de Clinging/Reorientation. No integra todavía semántica de `FollowOwnerGoal`, persecución o huida; eso pertenece a S06. El planner recibe estados/objetivos y devuelve maniobras físicas evaluadas.

La primera entrega S05-A es deliberadamente pequeña: dado un mob y una gravedad candidata desde el estado físico actual, evaluar la maniobra completa **cambio de gravedad → vuelo real → primer contacto → soporte habitable** sin mutar el mob.

## Invariantes antes de producción

1. **Evaluar no ejecuta.** Ninguna consulta del planner cambia gravedad, posición, velocidad, ownership, `airUsed` ni navegación.
2. **Capacidades reales.** Sin efecto compatible no hay transición. Clinging gastado en aire no obtiene otro giro. Reorientation no se degrada artificialmente a Clinging.
3. **Ownership ajeno es frontera.** Un mob con gravedad externa no puede ser reclamado por el planner.
4. **Una sola física.** La trayectoria usa `TrajectoryPrediction` + `AirMotion` + `LandingSurfaces.sweep`, igual que el landing del jugador.
5. **Volumen real.** Se simula el AABB del mob en la orientación gravitatoria candidata, incluida la recolocación mínima de centrado que ya usa `MobGravity` cuando el giro directo no cabe.
6. **Primer contacto manda.** Si el primer choque no es soporte bajo la gravedad candidata, la maniobra es inválida aunque exista un soporte mejor detrás.
7. **Desconocido no es aire.** Mundo fuera de límites, world border o chunks no disponibles invalidan el forecast.
8. **Landing utilizable.** Tocar una cara no basta: el volumen terminal debe caber y existir al menos una salida tangencial corta.
9. **Daño es coste, no veto.** La severidad del impacto se convierte en una penalización no lineal respecto a la vida actual. Una ruta potencialmente letal puede existir físicamente, pero será extraordinariamente cara.
10. **Robustez importa.** Más direcciones tangenciales libres reducen fragilidad/coste.
11. **Horizonte acotado.** S05-A no busca indefinidamente. Un contacto que no aparece dentro del horizonte se clasifica como `NO_LANDING_IN_HORIZON`, no como aire infinito.
12. **Sin goals todavía.** S05 no reemplaza pathfinding vanilla ni toca breadcrumbs/FollowOwner hasta que el evaluador esté demostrado.

## S05-A — evaluador de transición inmediata

Entrada:

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
- coste físico total base.

Rechazos explícitos:

- `NO_CAPABILITY`;
- `FOREIGN_GRAVITY`;
- `CAPABILITY_SPENT`;
- `INCOMPATIBLE_CONTEXT`;
- `UNCHANGED`;
- `NO_SPACE`;
- `UNKNOWN_GEOMETRY`;
- `INVALID_TRACE`;
- `NO_LANDING_IN_HORIZON`;
- `BLOCKING_CONTACT`;
- `STALE_CONTACT`;
- `TRAPPED_LANDING`.

S05-A también expone `evaluateGroundedLaunch`: evalúa desde una posición futura soportada, con velocidad estabilizada cero, sin mover al mob. Esto permite que el path táctico encuentre una frontera de lanzamiento y que la física se pronostique desde esa frontera, no desde el presente.

## Coste físico inicial

S05-A produce un coste neutral:

- tiempo de vuelo;
- pequeña penalización fija por usar un giro gravitatorio;
- distancia de recolocación mínima de lanzamiento;
- fragilidad del landing según número de salidas tangenciales;
- daño vanilla-equivalente como coste fuertemente no lineal respecto a vida actual.

La política de objetivo se añade por encima.

## S05-B — frontera caminable y comparación local

S05-B convierte un objetivo espacial en **un único punto de decisión caminable** sobre la superficie actual y compara desde ahí un conjunto acotado de maniobras.

### Dependencia reutilizada de Gravity Changer 26.2

Gravity Changer sustituye `GroundPathNavigation` vanilla por `DirectionalGroundPathNavigation` bajo gravedad no-DOWN y expone:

- `DirectionalMobAiUtil.projectOntoMovementPlane(origin,target,gravity)`;
- `DirectionalGroundNodeEvaluator.nodePosition(entityPosition,gravity)`;
- `DirectionalGroundNodeEvaluator.entityPosition(nodePosition,gravity)`.

S05 no implementa otro pathfinder de paredes/techos. La dependencia sigue siendo la capa táctica.

### Objetivo abstracto

`MobGravityLocalPlanner.Goal` aporta:

- `focus()` — punto mundial que guía el path;
- `satisfied(position, gravity)` — criterio real de llegada;
- `heuristic(position, gravity)` — coste restante no negativo.

S05 no conoce dueño, enemigo ni miedo.

### Pureza del pathfinding táctico

Minecraft 26.2 hace que `PathNavigation.createPath(...)` actualice metadatos internos incluso sin `moveTo`. Por ello S05-B consulta una **navegación espejo efímera**:

- DOWN → `GroundPathNavigation` nueva;
- gravedad lateral → `DirectionalGroundPathNavigation` nueva;
- copia `canFloat`, puertas y vallas desde el evaluador vivo;
- nunca llama `createPath` sobre la navegación activa.

El `reachRange` estratégico es **0**. Un valor positivo permite que Minecraft marque un path como alcanzado varios nodos Manhattan antes del foco; la tolerancia semántica pertenece a `Goal.satisfied()`, no al pathfinder.

### Algoritmo acotado

1. Proyectar `focus` al plano de movimiento actual.
2. Convertir la proyección al nodo correcto para esa gravedad.
3. Pedir una sola ruta al espejo efímero.
4. Si llega al nodo y `Goal.satisfied` acepta el estado terminal, devolver `WALK`.
5. Si el path es parcial, usar exactamente `getEndNode()` convertido con `entityPosition` como frontera.
6. Si no hay path, usar sólo la posición actual como frontera.
7. Desde esa frontera evaluar como máximo cinco gravedades distintas de la actual mediante `evaluateGroundedLaunch`.
8. Coste = aproximación caminando + coste físico + heurística del landing.
9. Elegir el candidato finito mínimo o `NO_PLAN`.

### Invariantes S05-B

- una consulta de pathfinding;
- máximo cinco forecasts gravitatorios;
- ninguna mutación de navegación viva, posición, gravedad u ownership;
- nada de escaneo volumétrico global;
- path parcial = frontera, nunca llegada;
- conversión de nodos mediante Gravity Changer también para gravedad lateral;
- WALK domina cuando el objetivo ya es alcanzable ordinariamente;
- resultado declarativo: `WALK`, `TRANSITION` o `NO_PLAN`.

### Tests TM S05-B

- WALK normal alcanzable;
- WALK bajo gravedad EAST con ancla `entityPosition` distinta del centro ingenuo del bloque;
- path parcial usa exactamente su último nodo;
- ninguna consulta toca path/target de la navegación viva;
- lanzamiento futuro usa la frontera como origen sin mover al mob;
- máximo cinco gravedades alternativas;
- transición física seleccionable desde una frontera futura;
- estado ya satisfecho no gasta pathfinding ni forecasts.

## S05-C — grafo de soportes online, no A* hipotético

La idea inicial era expandir varios estados soporte→soporte por adelantado. La auditoría de Gravity Changer 26.2 muestra una frontera importante: `DirectionalGroundNodeEvaluator.prepare(...)` lee la **gravedad real del mob**, y la navegación/pathfinder también toma la posición real del mob para construir el origen y la región de búsqueda.

Forzar una búsqueda multiestado puramente hipotética exigiría una de estas tres cosas:

1. mutar temporalmente posición/gravedad del mob y restaurarlas;
2. crear clones de entidades para cada nodo;
3. duplicar o parchear profundamente el pathfinding de Gravity Changer.

Las tres opciones empeoran seguridad, compatibilidad y coste, justo lo contrario del rediseño. Por tanto S05-C adopta el modelo que ya pedía el diseño original: **receding horizon / model-predictive control**.

### El grafo sigue existiendo, pero se recorre en tiempo real

Cada soporte estable realmente alcanzado es un nodo del grafo. Desde ese nodo:

1. S05-B calcula el mejor segmento WALK y, si hace falta, una transición segura;
2. el ejecutor camina hasta la frontera;
3. revalida inmediatamente antes del commit;
4. ejecuta una sola transición;
5. la física real gobierna el vuelo;
6. al confirmar un soporte estable real, ese soporte se convierte en el nuevo nodo raíz;
7. se vuelve a planificar desde la realidad observada.

No se simula pathfinding ordinario sobre estados en los que el mob todavía no existe.

### Evitar mínimos locales y bucles

El receding horizon necesita memoria, no clarividencia. S05-C añadirá una identidad estable para cada arista local:

`ManeuverKey(currentGravity, frontierNode, targetGravity)`

La frontera se cuantiza con la misma convención de nodo que Gravity Changer. El planner aceptará un conjunto de claves temporalmente excluidas y omitirá esas maniobras al comparar candidatos.

S06 mantendrá por mob una memoria pequeña y caducable de maniobras fallidas/no-progresivas. Así, si una transición segura no conduce a progreso real, el siguiente replan prueba otra arista en vez de repetir el mismo salto eternamente. Esa memoria se limpia o degrada al cambiar materialmente el objetivo, aterrizar en una región nueva o expirar el cooldown.

### Por qué esto es mejor que la búsqueda multiestado anticipada

- el pathfinding siempre consulta la posición y gravedad verdaderas;
- las superficies dinámicas se observan después de cada landing real;
- el coste queda acotado por replan;
- no se clonan mobs ni se tocan estados vivos para hacer preguntas;
- Clinging recupera naturalmente su cambio tras soporte real, sin tener que simular resets de recurso;
- el comportamiento sigue siendo legible: caminar → girar → caer → aterrizar → pensar otra vez.

### Gate S05-C

- `ManeuverKey` estable bajo pequeñas variaciones sub-bloque;
- una clave excluida no puede ser seleccionada;
- excluir el mejor candidato permite elegir el siguiente candidato seguro;
- WALK ordinario no queda bloqueado por memoria de maniobras gravitatorias;
- siguen respetándose los límites de una consulta de path y cinco forecasts por replan;
- la API sigue siendo pura respecto al mob.

Tras este gate, S05 queda cerrado y S06 conecta el planner online con `FollowOwnerGoal`, añade estado de ejecución/revalidación/landing/recovery y elimina breadcrumbs.
