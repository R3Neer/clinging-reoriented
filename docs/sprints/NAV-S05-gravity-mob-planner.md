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

## Coste inicial

S05-A no pretende fijar todavía la política de follow/chase/flee. Produce un coste físico neutral:

- tiempo de vuelo;
- pequeña penalización fija por usar un giro gravitatorio;
- distancia de recolocación mínima de lanzamiento;
- fragilidad del landing según número de salidas tangenciales;
- daño vanilla-equivalente como coste fuertemente no lineal respecto a vida actual.

El coste específico del objetivo se añadirá por encima en S06.

## Tests TM de S05-A

- una pared EAST real se detecta como landing válido usando geometría vanilla;
- la evaluación no cambia gravedad/posición/velocidad/ownership;
- un primer contacto no-soporte vence a cualquier soporte posterior;
- mundo/chunk desconocido falla cerrado;
- Clinging gastado en aire rechaza y Reorientation sigue siendo capaz;
- gravedad externa no puede ser reclamada;
- landing sin salida tangencial se rechaza;
- el coste de riesgo crece monótonamente y de forma no lineal;
- el mismo impacto cuesta más cuanto menor es la vida disponible.

## S05-B — frontera caminable y comparación local

S05-B no hace todavía una búsqueda global de estados. Su trabajo es convertir un objetivo espacial en **un único punto de decisión caminable** sobre la superficie actual y comparar desde ahí un conjunto acotado de maniobras.

### Dependencia reutilizada de Gravity Changer 26.2

La dependencia ya sustituye `GroundPathNavigation` vanilla por `DirectionalGroundPathNavigation` cuando la gravedad del mob no es DOWN, conservando capacidades como puertas, flotación y vallas. Además expone:

- `DirectionalMobAiUtil.projectOntoMovementPlane(origin,target,gravity)`;
- `DirectionalGroundNodeEvaluator.nodePosition(entityPosition,gravity)`;
- `DirectionalGroundNodeEvaluator.entityPosition(nodePosition,gravity)`.

Por tanto S05 no debe implementar otro pathfinder para paredes/techos. La navegación existente es la capa táctica; nuestro planner sólo decide **cuándo y hacia qué soporte cambiar de gravedad**.

### Objetivo abstracto

S05-B introduce un objetivo neutral con tres operaciones conceptuales:

- `focus()` — punto mundial que guía el path táctico;
- `satisfied(position, gravity)` — si un estado de soporte ya cumple el objetivo;
- `heuristic(position, gravity)` — coste restante no negativo para comparar candidatos.

Follow/chase/flee implementarán estas políticas en S06. S05-B no conoce dueño, enemigo ni miedo.

### Algoritmo acotado

1. Proyectar `focus` al plano de movimiento actual con `DirectionalMobAiUtil`.
2. Convertir esa proyección a nodo con `DirectionalGroundNodeEvaluator.nodePosition`.
3. Pedir **una sola** ruta a la navegación actual sin ejecutarla.
4. Si el path llega y el estado terminal satisface el objetivo, devolver `WALK`.
5. Si el path es parcial, usar su `getEndNode()` convertido a posición de entidad como frontera caminable.
6. Si no existe path, la posición actual es la única frontera permitida en S05-B.
7. Desde esa frontera evaluar como máximo las cinco gravedades distintas de la actual mediante el kernel S05-A.
8. Coste candidato = coste de aproximación caminando + coste físico S05-A + heurística del objetivo desde el landing.
9. Elegir el candidato finito de menor coste; si ninguno existe, devolver `NO_PLAN`.

### Invariantes S05-B

- **Una consulta de pathfinding** por planificación local.
- **Como máximo cinco simulaciones** gravitatorias.
- Crear un `Path` no inicia `moveTo` ni modifica la navegación activa.
- Nunca se escanean cubos 3D ni todos los bloques cercanos.
- Un path parcial sirve de frontera, no se interpreta como llegada al objetivo.
- La posición de frontera se obtiene con la misma convención de nodos que Gravity Changer, no con `BlockPos.containing` ingenuo bajo gravedad lateral.
- WALK domina a un giro cuando ya cumple el objetivo con navegación ordinaria.
- La gravedad actual no se reevalúa como “transición”.
- Un landing rechazado por S05-A no puede reaparecer por una heurística barata.
- El resultado sigue siendo declarativo: `WALK`, `TRANSITION` o `NO_PLAN`; la ejecución pertenece a una capa posterior.

### Tests TM S05-B

- objetivo caminable en la superficie actual devuelve WALK y cero evaluaciones gravitatorias aceptadas;
- obstáculo que produce path parcial usa exactamente el último nodo como frontera;
- gravedad lateral usa la conversión `nodePosition/entityPosition` de Gravity Changer;
- ninguna consulta cambia posición, gravedad, navegación ni estado de capacidad;
- nunca se evalúan más de cinco gravedades;
- una transición físicamente válida pero más cara que WALK no desplaza a WALK;
- una transición barata hacia un landing que mejora el objetivo vence a permanecer bloqueado;
- path nulo cae a frontera actual sin búsqueda global.

## S05-C — expansión perezosa de estados

Sólo después de cerrar S05-B, S05-C permitirá encadenar unos pocos estados soporte→soporte con presupuesto estricto. La expansión será perezosa, reutilizará S05-B para cada estado y no convertirá el mundo en una rejilla 3D global.

S06 conectará después el planner con goals concretos y retirará breadcrumbs sólo cuando el reemplazo general esté demostrado.
