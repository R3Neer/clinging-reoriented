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

## Después de S05-A

S05-B añadirá generación barata de posiciones de lanzamiento locales y comparación entre direcciones. S05-C añadirá una búsqueda perezosa/acotada de pocos estados de soporte. Sólo tras esos gates S06 conectará el planner con goals concretos y retirará breadcrumbs.
