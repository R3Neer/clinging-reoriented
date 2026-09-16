# NAV-S06-D/E — navegación gravitatoria general para goals de mobs

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Por qué existe esta extensión de S06

S05 construyó un planner físico y local que opera sobre `Mob` y un `Goal` abstracto. S06-A/C demostró ese planner con `FollowOwnerGoal`, eliminó breadcrumbs y añadió teleport seguro. S06-D/E completa la promesa arquitectónica: la locomoción gravitatoria no es una capacidad exclusiva de mascotas.

El objetivo es que los goals vanilla sigan siendo propietarios de la **intención** —perseguir, huir, ir a una posición— mientras una capa de locomoción gravitatoria común decide cómo ejecutar esa intención cuando la navegación normal no puede resolverla.

## Regla de oro

**No se reemplaza la IA de alto nivel.**

- un zombie sigue queriendo alcanzar su `mob.getTarget()`;
- `AvoidEntityGoal` sigue queriendo aumentar separación respecto a `toAvoid`;
- `PanicGoal` sigue eligiendo su destino de pánico;
- el planner gravitatorio sólo amplía el conjunto de locomociones posibles.

No hay tablas por especie ni una lista manual de zombies, lobos, vacas, etc. La capacidad se deriva de tener Clinging/Reorientation y un `PathNavigation` compatible.

## S06-D — bridge de intención de navegación

### Intents

El controlador general admite dos formas mínimas:

- `EntityIntent`: objetivo vivo/móvil. `focus()` se lee del entity actual, no de una coordenada congelada.
- `PositionIntent`: destino espacial elegido por vanilla.

Cada intent conserva además el speed modifier solicitado. No persiste a disco; es estado transitorio de IA.

### Cuándo interviene

1. Vanilla intenta primero su navegación normal.
2. Si existe un path que satisface semánticamente el objetivo, el bridge no interviene.
3. Si el path es `null`, parcial/no alcanzable o posteriormente deja de progresar, y el mob tiene capacidad gravitatoria legítima, el bridge puede pedir un `TRANSITION` al planner S05.
4. Si S05 devuelve sólo `WALK` o `NO_PLAN`, vanilla conserva el control.
5. Una transición especial posee temporalmente la navegación hasta landing/recovery.
6. Si el presupuesto S08 no permite planificar en ese tick, la intención queda en `WAITING_PLAN` y se reintenta después sin continuar una ruta parcial obsoleta.

### Recursión y ownership

El bridge observa únicamente entradas de intención (`moveTo(Entity, speed)` y destinos espaciales). La ejecución interna del planner usa `moveTo(Path, speed)` y no vuelve a registrar un intent nuevo.

Mientras una transición especial está activa, peticiones repetidas del mismo goal actualizan/observan el intent pero no pisan la ruta de aproximación ni cancelan un vuelo comprometido.

### Estado por mob

La memoria transitoria vive con `MobGravity.State`, no en mapas globales:

- intent actual;
- fase `IDLE/WAITING_PLAN/APPROACH/REVALIDATE/COMMITTED/LANDING_CONFIRM/RECOVERY`;
- plan local;
- ruta poseída;
- memoria corta de maniobras fallidas;
- throttles de planificación/progreso.

Nada de esto se serializa: al descargar/recrear una entidad, los goals reconstruyen su intención de forma natural.

## S06-E — wake adapters para goals que fallan antes de `moveTo`

Algunos goals consultan `createPath` dentro de `canUse()` y ni siquiera arrancan si el path es `null`. El bridge de navegación no puede rescatar una intención que nunca llegó a existir.

Por eso se usan adaptadores **por tipo de goal**, no por especie.

### `MeleeAttackGoal`

`canUse()` puede devolver `false` cuando `createPath(target,0)` no satisface la persecución. El adaptador puede despertar el goal si:

- existe target vivo válido;
- se respeta la cadencia/cooldown vanilla de `canUse()`;
- el mob tiene capacidad gravitatoria disponible;
- el planner general encuentra o difiere legítimamente una transición útil hacia el target.

`start/tick/canContinue/stop` siguen siendo vanilla salvo mientras el controlador general posee un segmento gravitatorio. Esto habilita zombies y cualquier otro `PathfinderMob` que use `MeleeAttackGoal` sin convertir el adapter en una IA de combate alternativa.

### `AvoidEntityGoal`

Vanilla sigue eligiendo la amenaza y su lógica de huida. Cuando la salida ordinaria no puede satisfacerse por navegación normal, el adapter expone al planner una intención de escape compatible con la misma locomoción genérica.

La cobertura final demuestra que una intención de huida puede tomar una transición gravitatoria cuando aporta una salida física útil, sin usar gravedad si correr sobre la superficie actual ya resuelve el objetivo.

### `PanicGoal`

Como `start()` ya emite `moveTo(posX,posY,posZ,speed)`, el bridge S06-D cubre el caso sin mixin específico. No se modifica su selección de agua ni sus causas de pánico.

## Presupuesto y eficiencia

- navegación ordinaria siempre primero;
- una sola consulta táctica espejo por plan local;
- hasta cuatro nodos de lanzamiento × cinco gravedades alternativas: `<=20` forecasts físicos por plan S05;
- presupuesto S08 de 32 nuevos planes por nivel/tick y 4 por región 64×64/tick;
- negative-result throttle;
- no replanning completo cada tick;
- durante vuelo no existe pathfinding de superficie;
- solicitudes excedentes esperan conservando intención.

## Gates S06-D/E cerrados

- zombie/Reorientation con target sin ruta ordinaria puede despertar `MeleeAttackGoal` y obtener locomoción gravitatoria;
- el mismo zombie sin efecto conserva comportamiento vanilla y no despierta mágicamente;
- target alcanzable por suelo no provoca transición;
- la cadencia absoluta de `MeleeAttackGoal` se conserva en vez de convertir el wake adapter en un poll por tick;
- una petición repetida durante `APPROACH` no pisa la ruta del planner;
- una petición repetida durante `COMMITTED` no devuelve navegación de superficie;
- cambiar target materialmente invalida/replantea de forma acotada;
- ownership externo nunca es reclamado;
- `AvoidEntityGoal`/flee puede usar una transición cuando la salida ordinaria no resuelve el escape;
- si la superficie actual basta, no se usa gravedad por espectáculo;
- una ruta gravitatoria con alto daño pierde preferencia frente a una salida segura, pero el daño sigue siendo coste, no veto geométrico;
- ninguna ruta puede terminar atrapada o ignorar un primer contacto bloqueante;
- un goal vivo puede permanecer activo mientras el presupuesto aplaza la planificación en `WAITING_PLAN`.

## Evidencia de cierre

Los últimos hardenings de S06 incluyen los fixtures de `MeleeAttackGoal` con target vanilla válido, preservación de su reloj absoluto de `canUse`, cobertura de intents diferidos y la prueba de flee gravitatorio. Sobre esa base se cerró S07 y se implementó S08 sin introducir un planner alternativo por especie.

El HEAD funcional `b2de88e83e1e64416288d220c8d86d52aeca014d` pasó **CI #1038 / run `35105097956`** con build/JUnit, GameTests de servidor, cliente base, First Person, Scale Brews server/client, Fresh Animations y snapshots en verde.

## Cierre

S06-D/E queda cerrado. La locomoción general de follow/chase/flee comparte el planner S05; S07 añade reacción aérea finita y S08 limita el coste global. Cualquier ajuste posterior pertenece a convergencia S09 o a un nuevo sprint explícito, no a una extensión silenciosa de S06.
