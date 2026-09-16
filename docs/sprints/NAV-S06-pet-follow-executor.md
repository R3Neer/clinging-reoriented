# NAV-S06 — integración de seguimiento de mascotas sin breadcrumbs

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Objetivo

S06 conecta el planner general de S05 con `FollowOwnerGoal` y sustituye la reproducción de breadcrumbs por seguimiento del **estado actual/filtrado del dueño y de la geometría actual**.

La mascota no intenta copiar el camino ni los cambios de gravedad del dueño. En cada soporte real decide su siguiente acción segura: seguir andando, aproximarse a una frontera y cambiar de gravedad, o dejar que vanilla gestione el caso porque no hace falta locomoción especial.

## Frontera de responsabilidad

- `MobGravityPlanner`: física de una transición, pura.
- `MobGravityLocalPlanner`: una consulta táctica + comparación local, pura.
- `PetGravityFollow`: ejecución online y memoria temporal por goal.
- `FollowGravityMixin`: adaptador mínimo de `FollowOwnerGoal`; no contiene planificación física.
- `MobGravity`: ownership/capacidad y commit gravitatorio real.
- `MobGravityPlanningBudget`: presupuesto compartido con la navegación general de mobs.
- `MobFlightReactor`: observación/reacción aérea común una vez comprometida la transición.

## Principios de integración

1. **Vanilla por defecto.** Si no hay una razón gravitatoria para intervenir, `FollowOwnerGoal` conserva su comportamiento normal.
2. **Sin historia del dueño.** Ningún queue, secuencia, TTL o posición histórica participa en la decisión.
3. **Objetivo actual/filtrado.** El owner sigue siendo la fuente de intención, pero el seguimiento aéreo usa un ancla filtrada/proyectada que evita perseguir ciegamente una coordenada 3D instantánea.
4. **Dueño en el aire no ordena giros.** Un cambio de gravedad del dueño nunca se convierte en una instrucción remota. Si el dueño está airborne, la mascota puede seguirlo como objetivo mediante tracking filtrado/tangencial y conservar locomoción segura desde su soporte actual.
5. **Mascota sin soporte: no planner de superficie.** Una transición comprometida termina primero. Una caída ajena al planner no dispara búsquedas grounded en mitad del aire.
6. **Approach y commit separados.** El `Path` de S05 sólo lleva a la frontera. Llegar a ella no autoriza automáticamente el giro.
7. **Revalidación inmediata.** Antes del commit se vuelve a evaluar desde la posición real soportada, con velocidad de lanzamiento estabilizada.
8. **El forecast y el commit deben coincidir.** El ejecutor usa la posición de lanzamiento validada por S05-A, incluida la recolocación centro-a-centro necesaria para que el cuerpo quepa.
9. **Clinging consume el salto soporte→soporte.** Un cambio iniciado desde soporte cuenta como el único cambio de ese tramo aéreo; aterrizar en soporte real recupera la capacidad según las reglas normales del efecto.
10. **Durante vuelo manda la física.** No hay `navigation.moveTo` ni pathfinding de superficie; S07 puede observar objetivo/geometría y reaccionar sólo tras la latencia legal.
11. **Landing confirmado.** Un contacto de un tick no termina la maniobra. Se exige soporte estable durante varios ticks antes de convertirlo en nueva raíz del planner.
12. **Receding horizon.** Tras landing real se planifica otra vez desde el mundo observado.
13. **Fallos generan memoria, no magia.** Una maniobra que no progresa se excluye temporalmente mediante `ManeuverKey`; el mob no recibe un giro gratis de rescate.
14. **Agua suspende el planner de superficies.** Si la mascota entra en fluido inesperadamente, se libera la navegación especial y el seguimiento correspondiente recupera el control hasta que exista soporte sólido otra vez.
15. **Sentarse/ser montado/leash/spectator owner conservan reglas vanilla.** No se usa el planner para sortear `unableToMoveToOwner()`.
16. **Presupuesto compartido.** Pet follow no dispone de una vía ilimitada: nuevas planificaciones grounded consumen el mismo presupuesto S08 que chase/flee.

## Meta de seguimiento

La política base es deliberadamente neutral:

- radio satisfecho: al menos `stopDistance`, ampliado si hace falta por el ancho del mob;
- objetivo estratégico: owner actual con filtrado suficiente para no hacer thrash;
- estar suficientemente cerca puede satisfacer el seguimiento aunque mascota y dueño tengan gravedades distintas;
- la heurística restante mide proximidad útil, no igualdad de gravedad.

No se exige compartir gravedad. Un perro en el suelo puede estar correctamente “siguiendo” a un dueño en techo/pared si ya está dentro del radio útil.

El `startDistance` vanilla sigue decidiendo cuándo arranca el seguimiento ordinario. El wake especial sólo existe cuando una transición gravitatoria real es necesaria; un simple WALK no convierte a la mascota en una sombra pegajosa.

## Fases del ejecutor

### IDLE / WAITING_PLAN

IDLE no posee navegación especial. Puede inspeccionar un plan cuando el goal está activo o necesita despertar por una transición gravitatoria.

Si el presupuesto S08 no concede slot, la intención puede quedar esperando sin reclamar un giro ni fabricar una ruta parcial. En ticks posteriores vuelve a competir por presupuesto.

### APPROACH

- posee el path declarativo de S05;
- lo entrega a la navegación viva actual;
- observa progreso hacia la frontera, no sólo `navigation.isDone()`;
- si el dueño se desplaza materialmente o la navegación cambia por una gravedad ajena, abandona/replanifica de forma acotada;
- al entrar en la región de lanzamiento, detiene la navegación y pasa a estabilización/revalidación.

### REVALIDATE

- exige soporte real bajo la gravedad actual;
- exige contexto compatible;
- estabiliza el lanzamiento para que la velocidad real coincida con el forecast;
- vuelve a llamar `evaluateGroundedLaunch` desde la posición real;
- si la transición ya no es segura, no gira: memoriza/abandona/replanifica.

### COMMITTED

- `MobGravity` aplica exactamente el cambio validado y adquiere ownership del efecto;
- Clinging marca el tramo aéreo como consumido;
- se detiene la navegación de superficie;
- el target sigue observándose a través de S07, pero no se obedece cada micro-movimiento del owner.

### LANDING_CONFIRM

- espera soporte real y estable durante 2 ticks consecutivos;
- sólo entonces considera terminada la arista del grafo;
- la siguiente planificación parte de la nueva posición/gravedad reales.

### RECOVERY

- se usa si la trayectoria real se desvía, expira su horizonte o entra en un contexto inesperado;
- no concede giros adicionales;
- espera un soporte seguro o devuelve el control al subsistema apropiado;
- la maniobra fallida entra en la memoria temporal de exclusiones.

## Replanning e histéresis

Un plan no se tira por cada paso del dueño. Se invalida si:

- dueño cambia de dimensión/desaparece;
- el objetivo filtrado se mueve materialmente respecto al ancla del plan;
- cambia la navegación/gravedad real antes del commit;
- la frontera deja de ser soporte válido;
- no hay progreso durante una ventana acotada;
- el commit revalidado deja de ser seguro.

Durante vuelo, S07 mantiene observación barata. Pequeño jitter no abre una nueva decisión; movimiento material sostenido actualiza la intención sin retrasar eternamente el reloj de reacción.

`ManeuverKey` fallidas tienen cooldown corto. Cambiar de nodo de soporte o mover materialmente el objetivo permite volver a considerar el espacio de maniobras sin conservar rencor eterno, una capacidad que algunos humanos aún no han implementado.

## Teleport fallback

S06 no llama ciegamente al teleport vanilla desde una gravedad lateral. `TamableAnimal.tryToTeleportToOwner()` valida con supuestos DOWN y después puede conservar un estado gravitatorio incompatible con esa colocación.

`PetGravityTeleport` mantiene el fallback separado:

1. primero intentar locomoción normal/planificada;
2. sólo por separación extrema o no-progreso repetido;
3. buscar una posición habitable cerca del dueño y una gravedad compatible;
4. validar cuerpo, soporte, fluido/peligro y al menos una salida tangencial antes de mover;
5. preferir la gravedad actual cuando siga siendo viable;
6. no robar ownership externo/borrowed;
7. no usar teleport para ocultar fallos ordinarios del planner.

## Entregas cerradas

### S06-A — commit físico + máquina de fases

- commit planificado en `MobGravity`;
- Clinging consume correctamente un giro iniciado desde soporte;
- executor separado del mixin;
- approach → revalidate → committed → landing confirm/recovery;
- memoria pequeña y acotada de `ManeuverKey` fallidas.

### S06-B — `FollowOwnerGoal`

- el mixin delega sólo cuando existe trabajo especial;
- vanilla queda intacto para mobs sin capacidad y WALK ordinario;
- una transición especial puede despertar seguimiento cuando es necesaria;
- owner airborne se mantiene como objetivo filtrado sin copiar su giro;
- refresh de navegación tras cambio de gravedad;
- sentarse/liberarse conserva semántica vanilla.

### S06-C — retirada de breadcrumbs + fallback

- `GravityBreadcrumbs` y sus hooks dejan de ser arquitectura runtime;
- se eliminan campos/colas históricas del estado vivo;
- tests se basan en objetivo/geometría actuales;
- teleport/recovery es gravedad-aware y fail-closed;
- regresiones de rocas/primer contacto lateral impiden volver al bug original.

## Gate final S06

Cumplido: una mascota con Reorientation/Clinging puede seguir al dueño entre superficies sin conocer su historia, sin cambiar gravedad remotamente y sin aceptar una maniobra cuyo primer contacto previsible sea bloqueo/atrapamiento. Un owner airborne sigue siendo rastreable mediante un objetivo filtrado; una mascota sin esos efectos conserva seguimiento vanilla.

El HEAD funcional `b2de88e83e1e64416288d220c8d86d52aeca014d` pasó **CI #1038 / run `35105097956`** con la matriz completa en verde.

## Cierre

S06 queda cerrado. S07 aporta reacción dinámica durante vuelo y S08 presupuesto/eficiencia; S09 sólo debe converger documentación, búsquedas legacy y la matriz final.
