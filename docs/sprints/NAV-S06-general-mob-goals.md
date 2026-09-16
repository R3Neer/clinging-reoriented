# NAV-S06-D/E — navegación gravitatoria general para goals de mobs

Estado: **EN PROGRESO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Por qué existe esta extensión de S06

S05 ya construyó un planner físico y local que opera sobre `Mob` y un `Goal` abstracto. S06-A/C demostró ese planner con `FollowOwnerGoal`, eliminó breadcrumbs y añadió teleport seguro. Falta completar la promesa arquitectónica: que la locomoción gravitatoria no sea una capacidad exclusiva de mascotas.

El objetivo de S06-D/E es que los goals vanilla sigan siendo propietarios de la **intención** (perseguir, huir, ir a una posición), mientras una capa de locomoción gravitatoria común decide cómo ejecutar esa intención cuando la navegación normal no puede resolverla.

## Regla de oro

**No se reemplaza la IA de alto nivel.**

- un zombie sigue queriendo alcanzar su `mob.getTarget()`;
- `AvoidEntityGoal` sigue queriendo aumentar separación respecto a `toAvoid`;
- `PanicGoal` sigue eligiendo su destino de pánico;
- el planner gravitatorio sólo amplía el conjunto de locomociones posibles.

No habrá tablas por especie ni una lista manual de zombies, lobos, vacas, etc. La capacidad se deriva de tener Clinging/Reorientation y un `PathNavigation` compatible.

## S06-D — bridge de intención de navegación

### Intents

El controlador general admite dos formas mínimas:

- `EntityIntent`: objetivo vivo/móvil. `focus()` se lee del entity actual, no de una coordenada congelada.
- `PositionIntent`: destino espacial elegido por vanilla.

Cada intent conserva además el speed modifier solicitado. No persiste a disco; es estado transitorio de IA.

### Cuándo interviene

1. Vanilla intenta primero su navegación normal.
2. Si existe un path alcanzable, el bridge no interviene.
3. Si el path es `null`, parcial/no alcanzable o posteriormente deja de progresar, y el mob tiene capacidad gravitatoria legítima, el bridge puede pedir un `TRANSITION` al planner S05.
4. Si S05 devuelve sólo `WALK` o `NO_PLAN`, vanilla conserva el control.
5. Una transición especial posee temporalmente la navegación hasta landing/recovery.

### Recursión y ownership

El bridge observa únicamente entradas de intención (`moveTo(Entity, speed)` y destinos espaciales). La ejecución interna del planner usa `moveTo(Path, speed)` y no vuelve a registrar un intent nuevo.

Mientras una transición especial está activa, peticiones repetidas del mismo goal actualizan/observan el intent pero no pisan la ruta de aproximación ni cancelan un vuelo comprometido.

### Estado por mob

La memoria transitoria vive con `MobGravity.State`, no en mapas globales:

- intent actual;
- fase `IDLE/APPROACH/REVALIDATE/COMMITTED/LANDING_CONFIRM/RECOVERY`;
- plan local;
- ruta poseída;
- memoria corta de maniobras fallidas;
- throttles de planificación/progreso.

Nada de esto se serializa: al descargar/recrear una entidad, los goals reconstruyen su intención de forma natural.

## S06-E — wake adapters para goals que fallan antes de `moveTo`

Algunos goals consultan `createPath` dentro de `canUse()` y ni siquiera arrancan si el path es `null`. El bridge de navegación no puede rescatar una intención que nunca llegó a existir.

Por eso se permiten adaptadores **por tipo de goal**, no por especie:

### `MeleeAttackGoal`

`canUse()` puede devolver `false` cuando `createPath(target,0)` es `null`. El adaptador puede devolver `true` si:

- existe target vivo válido;
- el mob tiene capacidad gravitatoria disponible;
- el planner general encuentra una transición útil hacia el target.

`start/tick/canContinue/stop` siguen siendo vanilla salvo mientras el controlador general posee un segmento gravitatorio. Esto habilita zombies y cualquier otro `PathfinderMob` que use `MeleeAttackGoal`.

### `AvoidEntityGoal`

Vanilla elige primero una posición `getPosAway` y exige un path. Primera entrega: si esa posición es válida como intención pero el path ordinario no existe, permitir que el planner gravitatorio la alcance mediante otra superficie.

Segunda entrega dentro del mismo sprint: evaluar también destinos gravitatorios que aumenten la **ventaja de escape**, no sólo distancia euclídea. El coste de huida compara aproximadamente tiempo/separación obtenida y puede preferir pared/techo cuando eso dificulta más la persecución que correr recto.

### `PanicGoal`

Como `start()` ya emite `moveTo(posX,posY,posZ,speed)`, el bridge S06-D cubre la mayor parte del caso sin mixin específico. No se modifica su selección de agua ni sus causas de pánico.

## Presupuesto y eficiencia

- navegación ordinaria siempre primero;
- una sola consulta táctica espejo por replan local;
- máximo cinco forecasts de gravedad por comparación S05;
- negative-result throttle;
- no replanning completo cada tick;
- durante vuelo no existe pathfinding de superficie;
- varios mobs deben escalonar trabajo en S08 si las pruebas de estrés muestran picos.

## Gates S06-D

- zombie/Reorientation con target detrás de barrera sin path normal despierta y obtiene transición segura;
- el mismo zombie sin efecto conserva `MeleeAttackGoal` vanilla y no despierta mágicamente;
- target alcanzable por suelo no provoca transición;
- una petición repetida mientras `APPROACH` no pisa la ruta del planner;
- una petición repetida durante `COMMITTED` no devuelve navegación de superficie;
- cambiar target materialmente invalida/replantea de forma acotada;
- ownership externo nunca es reclamado.

## Gates S06-E

- `AvoidEntityGoal` puede usar una transición cuando su destino de huida ordinario está aislado por geometría;
- si correr por la superficie actual ya es suficiente, no se usa gravedad por espectáculo;
- una ruta gravitatoria con alto daño pierde preferencia frente a una salida segura, pero no desaparece del grafo por el mero hecho de hacer daño;
- ninguna ruta puede terminar atrapada o con primer contacto lateral bloqueante.

## Frontera con S07

S06 no implementa todavía reflejos variables ni correcciones aéreas ante bloques colocados tarde. Primero debe existir un controlador gravitatorio **general** consumido por follow/chase/flee. S07 añadirá sobre ese controlador común:

- target tracking filtrado durante vuelo;
- latencia derivada de `MOVEMENT_SPEED` base;
- detección de cambios de geometría;
- correcciones legales de Reorientation;
- impacto natural cuando la reacción llega demasiado tarde.
