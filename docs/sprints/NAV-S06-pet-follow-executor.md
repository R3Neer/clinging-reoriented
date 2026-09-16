# NAV-S06 — integración de seguimiento de mascotas sin breadcrumbs

Estado: **EN PROGRESO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Objetivo

S06 conecta el planner general de S05 con `FollowOwnerGoal` y sustituye la reproducción de breadcrumbs por seguimiento del **estado actual del dueño y de la geometría actual**.

La mascota no intenta copiar el camino ni los cambios de gravedad del dueño. En cada soporte real decide su siguiente acción segura: seguir andando, aproximarse a una frontera y cambiar de gravedad, o dejar que vanilla gestione el caso porque no hace falta locomoción especial.

## Frontera de responsabilidad

- `MobGravityPlanner`: física de una transición, pura.
- `MobGravityLocalPlanner`: una consulta táctica + comparación local, pura.
- `PetGravityFollow`: ejecución online y memoria temporal por goal.
- `FollowGravityMixin`: adaptador mínimo de `FollowOwnerGoal`; no contiene planificación física.
- `MobGravity`: ownership/capacidad y commit gravitatorio real.

## Principios de integración

1. **Vanilla por defecto.** Si no hay una razón gravitatoria para intervenir, `FollowOwnerGoal` conserva su comportamiento normal.
2. **Sin historia del dueño.** Ningún queue, secuencia, TTL o posición histórica participa en la decisión.
3. **Objetivo actual.** El `focus` es el dueño actual; estar suficientemente cerca puede satisfacer el seguimiento aunque mascota y dueño tengan gravedades distintas.
4. **Dueño en el aire: no perseguirlo con saltos gravitatorios.** Mientras el dueño no tenga soporte real, la locomoción especial no inicia una transición nueva. Vanilla/Gravity Changer puede seguir aproximando al mob por su superficie actual.
5. **Mascota sin soporte: no replantear.** Una transición comprometida termina primero. Una caída ajena al planner no dispara nuevos cambios de gravedad.
6. **Approach y commit separados.** El `Path` de S05 sólo lleva a la frontera. Llegar a ella no autoriza automáticamente el giro.
7. **Revalidación inmediata.** Antes del commit se vuelve a evaluar desde la posición real soportada, con velocidad de lanzamiento estabilizada.
8. **El forecast y el commit deben coincidir.** El ejecutor usa la posición de lanzamiento validada por S05-A, incluida la recolocación centro-a-centro necesaria para que el cuerpo quepa.
9. **Clinging consume el salto soporte→soporte.** Un cambio iniciado desde soporte cuenta como el único cambio de ese tramo aéreo; aterrizar en soporte real recupera la capacidad.
10. **Durante vuelo manda la física.** No hay `navigation.moveTo`, persecución del dueño ni replan de cámara/posición cada tick.
11. **Landing confirmado.** Un contacto de un tick no termina la maniobra. Se exige soporte estable durante varios ticks antes de convertirlo en nueva raíz del planner.
12. **Receding horizon.** Tras landing real se planifica otra vez desde el mundo observado.
13. **Fallos generan memoria, no magia.** Una maniobra que no progresa se excluye temporalmente mediante `ManeuverKey`; el mob no recibe un giro gratis de rescate.
14. **Agua suspende el planner de superficies.** Si la mascota entra en fluido inesperadamente, se libera la navegación especial y el seguimiento acuático ordinario recupera el control hasta que exista soporte sólido otra vez.
15. **Sentarse/ser montado/leash/spectator owner conservan las reglas vanilla.** No se usa el planner para sortear `unableToMoveToOwner()`.

## Meta de seguimiento

La primera política de S06 es deliberadamente simple y neutral:

- radio satisfecho: al menos `stopDistance`, ampliado si hace falta por el ancho del mob;
- `focus = owner.position()`;
- `satisfied = distancia euclídea <= radio`;
- `heuristic = max(0, distancia - radio)`.

No se exige compartir gravedad. Un perro en el suelo puede estar correctamente “siguiendo” a un dueño en techo/pared si ya está dentro del radio útil.

El `startDistance` vanilla sólo decide cuándo arranca el seguimiento ordinario. Dentro de ese dead-zone, S06 sólo despertará `FollowOwnerGoal` de forma especial si existe una **TRANSITION** segura y necesaria; un simple WALK no convierte a la mascota en una sombra pegajosa.

## Fases del ejecutor

### IDLE

No posee navegación. Puede inspeccionar un plan cuando el goal ya está activo o cuando `canUse` pregunta si hace falta despertar por una transición gravitatoria.

### APPROACH

- posee el path declarativo de S05;
- lo entrega a la navegación viva actual;
- observa progreso hacia la frontera, no sólo `navigation.isDone()`;
- si el dueño se desplaza materialmente o la navegación cambia por una gravedad ajena, abandona/replanifica;
- al entrar en la región de lanzamiento, detiene la navegación y pasa a estabilización/revalidación.

### REVALIDATE

- exige soporte real bajo la gravedad actual;
- exige contexto compatible;
- estabiliza el lanzamiento para que la velocidad real coincida con el forecast;
- vuelve a llamar `evaluateGroundedLaunch` desde la posición real;
- si el target gravity ya no es seguro, no gira: memoriza/abandona/replanifica.

### COMMITTED

- `MobGravity` aplica exactamente el cambio validado y adquiere ownership del efecto;
- Clinging marca el tramo aéreo como consumido;
- se detiene la navegación;
- no se responde a micro-movimientos del dueño.

### LANDING_CONFIRM

- espera soporte real y estable durante 2 ticks consecutivos;
- sólo entonces considera terminada la arista del grafo;
- la siguiente planificación parte de la nueva posición/gravedad reales.

### RECOVERY

- se usa si la trayectoria real se desvía, expira su horizonte o entra en un contexto inesperado;
- no concede giros adicionales;
- espera un soporte seguro o devuelve el control al subsistema apropiado (por ejemplo agua);
- la maniobra fallida entra en la memoria temporal de exclusiones.

## Replanning e histéresis

Un plan no se tira por cada paso del dueño. Se invalida si:

- dueño cambia de dimensión/desaparece;
- dueño se mueve materialmente respecto al ancla del plan;
- cambia la navegación/gravedad real antes del commit;
- la frontera deja de ser soporte válido;
- no hay progreso durante una ventana acotada;
- el commit revalidado deja de ser seguro.

`ManeuverKey` fallidas tienen cooldown corto. Cambiar de nodo de soporte o mover materialmente el objetivo permite volver a considerar el espacio de maniobras sin conservar rencor eterno, una capacidad que algunos humanos aún no han implementado.

## Teleport fallback

S06 no debe llamar ciegamente al teleport vanilla desde una gravedad lateral. `TamableAnimal.tryToTeleportToOwner()` valida con `WalkNodeEvaluator` DOWN y después conserva el estado gravitatorio del animal, combinación capaz de crear un landing absurdo.

Se tratará como fallback separado:

1. primero intentar locomoción normal/planificada;
2. sólo por separación extrema o no-progreso repetido;
3. buscar una posición habitable cerca del dueño y una gravedad compatible;
4. validar cuerpo, soporte y peligro antes de mover;
5. no usar el teleport para ocultar fallos ordinarios del planner.

Hasta que ese fallback seguro exista, el controlador especial no sustituye una ruta fallida por un teleport gravitatorio improvisado.

## Entregas

### S06-A — commit físico + máquina de fases aislada

- nuevo commit planificado en `MobGravity`;
- Clinging consume correctamente un giro iniciado desde soporte;
- controlador `PetGravityFollow.State` sin mixin todavía;
- approach → revalidate → committed → landing confirm;
- memoria pequeña de `ManeuverKey` fallidas;
- tests unitarios/GameTests del ejecutor directo.

### S06-B — `FollowOwnerGoal`

- el mixin delega al controlador sólo cuando existe plan especial;
- vanilla queda intacto para mobs sin capacidad y para WALK ordinario;
- transición especial puede despertar dentro de `startDistance` cuando aún está fuera del radio útil;
- owner airborne no provoca giro;
- refresh de navegación tras cambio de gravedad;
- sentarse/liberarse conserva semántica vanilla.

### S06-C — retirada de breadcrumbs + fallback

- eliminar `GravityBreadcrumbs` y sus hooks;
- eliminar campos `breadcrumb*` de `MobGravity.State`;
- reemplazar tests históricos por tests de objetivo actual;
- revisar teleport/recovery seguro y no-progreso;
- adversarial con rocas/primer contacto lateral que reproduzca el bug original.

## Gate final S06

Una mascota con Reorientation/Clinging debe poder seguir al dueño entre superficies sin conocer su historia, sin cambiar gravedad remotamente y sin aceptar una maniobra cuyo primer contacto previsible sea bloqueo/atrapamiento. Una mascota sin esos efectos debe conservar el seguimiento vanilla.