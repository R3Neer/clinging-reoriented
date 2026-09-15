# Rediseño desde cero — seguimiento gravitatorio de mascotas

Estado: **DISEÑO / SIN IMPLEMENTACIÓN**.

Este documento sustituye conceptualmente el modelo de breadcrumbs. No propone todavía clases, mixins ni estructura de código. El objetivo es definir qué decisiones debe tomar una mascota, con qué información y bajo qué invariantes de seguridad/gamefeel.

## 1. Problema que realmente hay que resolver

La pregunta no es:

> “¿Cómo puede la mascota repetir los cambios de gravedad que hizo el dueño?”

La pregunta correcta es:

> “Dado el estado actual de la mascota, el estado actual del dueño y la geometría actual del mundo, ¿qué secuencia segura de caminar, cambiar gravedad y aterrizar acerca a la mascota a una posición válida de seguimiento?”

La mascota debe **seguir al dueño**, no reconstruir su historia.

Consecuencias:

- no hay breadcrumbs;
- no hay posiciones históricas del dueño como instrucciones;
- un cambio de gravedad del dueño no obliga a la mascota a cambiar gravedad;
- la mascota puede llegar por una superficie o gravedad distinta;
- si ya puede mantener proximidad sin cambiar gravedad, no debe hacer acrobacias gratuitamente;
- una transición gravitatoria sólo es válida si se predice una salida segura, no simplemente porque el animal quepa en el instante del giro.

## 2. Principios de diseño

### P1 — Seguridad como restricción dura

Una ruta peligrosa no compite con una ruta segura mediante una penalización pequeña. Se descarta.

Una transición gravitatoria candidata debe quedar fuera del plan si puede razonablemente provocar:

- intersección/sufocación;
- colisión lateral fuerte antes del soporte previsto;
- aterrizaje sin espacio corporal;
- impacto previsiblemente dañino de forma inaceptable;
- caída al vacío o fuera de mundo/borde;
- entrada en geometría/chunks no conocidos;
- aterrizaje en un peligro que el animal no tolera;
- un estado final sin una mínima viabilidad de movimiento.

### P2 — Objetivo dinámico, no ruta histórica

El dueño sólo aporta un **objetivo actual**. Su trayectoria pasada es irrelevante.

### P3 — Seguir no significa compartir gravedad

Si el dueño camina por un techo bajo y el lobo puede seguirlo perfectamente por el suelo situado dos bloques debajo, ése puede ser el comportamiento correcto. Igualar la gravedad del dueño es, como mucho, una preferencia secundaria cuando mejora el seguimiento.

### P4 — Aprovechar la navegación normal siempre que baste

El comportamiento más natural debe ganar por defecto:

1. caminar con la gravedad actual;
2. sólo si eso no permite mantener/recuperar el seguimiento, considerar una transición gravitatoria.

### P5 — Una transición gravitatoria es una maniobra completa

No termina al cambiar el atributo de gravedad. Termina al alcanzar **soporte estable y seguro** bajo la nueva gravedad.

### P6 — Replanificación frecuente, ejecución estable

El dueño se mueve, así que ningún plan largo debe considerarse sagrado. Pero una mascota tampoco debe cambiar de idea cada tick.

Se usa planificación de horizonte limitado/receding horizon:

- planificar unos pocos pasos útiles;
- ejecutar sólo el siguiente segmento;
- volver a planificar tras un aterrizaje o un cambio material del objetivo;
- no abandonar una transición aérea ya comprometida salvo emergencia.

### P7 — El teletransporte es fallback, no locomoción principal

Minecraft ya acepta que una mascota muy rezagada aparezca cerca del dueño. Debe conservarse como red de seguridad ante imposibilidad, distancia extrema o falta prolongada de progreso, pero no sustituir la navegación gravitatoria normal.

## 3. Unidad fundamental: estado de soporte

El planificador razona principalmente entre **estados estables de soporte**.

Un estado de soporte describe conceptualmente:

- una posición/pose en la que el animal cabe;
- una dirección de gravedad;
- una superficie que realmente soporta al animal bajo esa gravedad;
- espacio libre suficiente para permanecer y empezar a moverse;
- el estado de capacidad gravitatoria de la mascota (sin efecto / Clinging disponible o consumido / Reorientation / ownership externo);
- contexto de medio relevante (sólido, fluido, superficie dinámica, etc.).

No hace falta que el dueño y la mascota compartan el mismo estado de soporte.

## 4. El objetivo no es un punto: es un conjunto de estados aceptables

Buscar exactamente `owner.position()` es conceptualmente incorrecto.

El planificador debe construir alrededor del dueño un **conjunto de estados objetivo**: posiciones seguras desde las que la mascota estaría razonablemente “siguiendo” al dueño.

### Dueño estable/apoyado

Se buscan estados de soporte seguros en su entorno cercano:

- sobre la misma superficie;
- sobre una superficie paralela cercana;
- bajo/encima/a un lado si sigue existiendo proximidad útil;
- con cualquier gravedad permitida para la mascota.

No se fuerza la gravedad del dueño.

### Dueño en el aire con aterrizaje próximo y predecible

El objetivo puede desplazarse hacia la zona de soporte que previsiblemente ocupará el dueño al terminar la maniobra.

La mascota no debe perseguir ciegamente la posición aérea instantánea.

### Dueño en vuelo prolongado o trayectoria no fiable

La mascota mantiene una estrategia de **tracking seguro**:

- acercarse cuanto pueda desde superficies estables;
- conservar una posición útil respecto de la proyección del dueño sólo como objetivo de navegación, nunca como orden de giro;
- esperar una oportunidad segura de transición;
- utilizar fallback de distancia si el dueño se aleja demasiado.

Un cambio de gravedad del dueño en mitad del aire no dispara automáticamente ninguna acción en la mascota.

## 5. Grafo conceptual de navegación

El problema puede verse como un grafo dinámico de estados de soporte.

### Nodos

Cada nodo es un estado estable de soporte de la mascota.

### Aristas WALK

Representan desplazamiento normal sobre la superficie actual bajo la gravedad actual.

La navegación ordinaria debe resolver esta parte. El planificador gravitatorio no necesita reinventar caminar alrededor de una piedra si el pathfinding normal ya sabe hacerlo.

### Aristas GRAVITY TRANSITION

Representan:

1. caminar hasta una posición de lanzamiento alcanzable;
2. estabilizar la maniobra lo suficiente para que la predicción sea válida;
3. cambiar una vez la gravedad;
4. recorrer la trayectoria física resultante;
5. adquirir un nuevo soporte estable.

La arista existe **sólo** si toda la maniobra es segura.

### Arista TELEPORT / RESCUE

No forma parte del camino preferido. Sólo aparece como fallback de continuidad cuando la navegación normal/gravitatoria no progresa y las reglas de seguimiento justificarían una recuperación tipo vanilla.

## 6. Generación de una transición gravitatoria segura

Éste es el núcleo del sistema.

Desde un estado de soporte actual se consideran posiciones de lanzamiento **alcanzables caminando** y direcciones de gravedad alternativas permitidas.

Para cada candidato se predice la maniobra completa usando:

- dimensiones reales del animal;
- pose;
- gravedad candidata;
- velocidad real o velocidad esperada en el instante de lanzamiento;
- aceleración gravitatoria;
- geometría de colisión del mundo;
- hazards;
- límites/chunks;
- daño de impacto esperado cuando corresponda.

### 6.1 Barrido volumétrico, no rayo puntual

La trayectoria válida es la del volumen corporal completo, no la del centro del animal.

Cada tramo debe permanecer libre hasta alcanzar el soporte terminal esperado.

### 6.2 Qué colisión se considera “aterrizaje”

El contacto terminal debe ser compatible con la nueva gravedad: la superficie debe comportarse como suelo para esa dirección.

Una roca golpeada de lado antes de llegar al soporte no se interpreta como éxito sólo porque haya existido una colisión.

### 6.3 El estado final debe ser habitable

No basta con que el AABB final quepa exactamente.

Se exige además una pequeña viabilidad posterior:

- margen suficiente para no empezar ya atrapado;
- alguna posibilidad local de movimiento/escape;
- soporte estable suficiente para el footprint del animal;
- ausencia de peligro inmediato incompatible con la criatura.

Una grieta en la que “técnicamente cabe” pero desde la que no puede caminar debe recibir rechazo o coste de fragilidad extremo.

### 6.4 Seguridad de impacto

Una transición que previsiblemente cause daño grave o mortal no debe ser elegida para ahorrar recorrido.

La salud de la mascota forma parte de la seguridad de ruta.

### 6.5 Mundo desconocido

No se planifica una trayectoria a través de chunks no disponibles ni fuera de límites válidos. Lo desconocido no cuenta como espacio libre.

## 7. Búsqueda del plan

La búsqueda tiene dos fases conceptuales.

### Fase rápida: ¿puedo seguir sin cambiar gravedad?

Antes de construir transiciones:

1. generar estados objetivo alrededor del dueño;
2. comprobar si alguno es alcanzable con navegación ordinaria bajo la gravedad actual;
3. si existe una ruta razonable, usarla.

Esto evita comportamiento circense innecesario.

### Fase gravitatoria

Si la gravedad actual no permite converger:

1. generar un frente local de estados de soporte alcanzables mediante una transición segura;
2. desde esos estados, considerar de nuevo caminar y, si hace falta, otra transición;
3. buscar una ruta de coste mínimo hacia cualquier estado objetivo.

La búsqueda debe ser acotada y perezosa: sólo explorar estados/transiciones cuando hagan falta y dentro de un horizonte razonable.

## 8. Función de coste

Primero se aplican los filtros de seguridad. Entre rutas seguras, el coste debe favorecer comportamiento natural.

Orden conceptual:

1. **tiempo/distancia estimada hasta recuperar seguimiento**;
2. **menos cambios de gravedad**;
3. **menos tiempo en transición aérea**;
4. **aterrizajes amplios y robustos frente a aterrizajes estrechos/frágiles**;
5. **mantener el plan actual** cuando la alternativa sólo es marginalmente mejor;
6. opcionalmente, compartir gravedad con el dueño como bonus pequeño, nunca como obligación.

Esto introduce histéresis: una mejora diminuta no debe hacer que el lobo abandone una ruta estable y cambie de pared como un robot aspirador poseído.

## 9. Ejecución por estados

### FOLLOW / SURFACE NAVIGATION

La mascota camina hacia el siguiente waypoint o estado objetivo usando su navegación normal.

Si el dueño se mueve, se puede actualizar el destino sin cambiar de modo mientras el plan siga siendo útil.

### APPROACH TRANSITION

La mascota camina hacia la zona desde la que la transición seleccionada fue validada.

Justo antes de comprometerse se vuelve a validar la maniobra con el mundo y el estado actuales.

Si ya no es segura, no gira: replantea.

### TRANSITION COMMITTED

La gravedad cambia y la física gobierna el desplazamiento.

Durante esta fase:

- no se inicia una segunda ruta ordinaria;
- no se persigue cada movimiento del dueño;
- no se considera completado el paso simplemente porque el atributo cambió;
- se supervisa que la trayectoria real siga siendo compatible con la envolvente segura predicha.

### LANDING CONFIRMATION

La transición sólo termina cuando la mascota adquiere soporte estable durante tiempo suficiente para distinguir un apoyo real de un roce de un tick.

Entonces el nuevo estado se convierte en raíz de una nueva planificación.

### RECOVERY

Si la realidad se desvía de la predicción:

- colisión inesperada;
- soporte que desapareció/se movió;
- animal atascado;
- entrada inesperada en fluido;
- trayectoria excede el horizonte seguro;

se abandona el plan y se prioriza adquirir un estado seguro, aunque eso aleje temporalmente a la mascota del dueño.

## 10. Por qué esto evita el bug del lobo contra las rocas

Escenario:

- dueño cae/cambia gravedad hacia unas rocas;
- lobo está en suelo DOWN;
- el dueño termina en una superficie lateral.

El algoritmo no sabe ni necesita saber dónde giró el dueño.

1. Construye estados objetivo cerca del dueño.
2. Comprueba si puede llegar manteniendo DOWN. Si puede quedarse suficientemente cerca desde el suelo, simplemente camina.
3. Si no puede, busca transiciones seguras desde posiciones alcanzables de su suelo actual.
4. Una posición junto a la roca desde la que EAST provoca que el volumen del lobo golpee primero el suelo/una arista lateral queda **rechazada**.
5. Otra posición algo desplazada desde la que EAST produce una trayectoria limpia hasta una cara de roca con espacio corporal queda aceptada.
6. El lobo camina primero hasta ese launch state.
7. Revalida.
8. Cambia EAST.
9. La transición no se considera terminada hasta estar estable sobre la roca.
10. Desde ahí vuelve a planificar hacia el dueño.

No existe el paso “he alcanzado una coordenada parecida a la que tuvo el dueño, por tanto giro aquí”.

## 11. Capacidad de Clinging y Reorientation

El planificador debe respetar las reglas del mod, no inventar energía gravitatoria.

### Sin efecto compatible

No se realizan transiciones gravitatorias propias. Se conserva seguimiento vanilla/fallback normal.

### Clinging

Una transición soporte→soporte usa como máximo un cambio de gravedad. Al alcanzar soporte seguro se recupera naturalmente la posibilidad de una nueva etapa según las reglas existentes.

Esto encaja especialmente bien con el grafo de estados de soporte.

### Reorientation

Aunque permitiría múltiples cambios aéreos, la primera versión del nuevo algoritmo **no debería necesitarlos como estrategia normal**. También puede planificar soporte→soporte con una sola transición por etapa.

Eso hace la IA mucho más legible y segura. Las maniobras aéreas múltiples pueden estudiarse después como extensión si existe un caso de gameplay real que las justifique.

### Ownership externo

Si otra fuente posee la gravedad del animal, el sistema de seguimiento no la roba. Puede intentar seguir dentro de las capacidades de ese estado o recurrir al fallback correspondiente.

## 12. Dueño moviéndose mientras existe un plan

La planificación es dinámica, pero debe evitar thrashing.

### Replanificar si

- el dueño ha desplazado materialmente el conjunto de objetivos;
- cambia de dimensión/teleporta;
- el siguiente tramo deja de existir;
- una superficie dinámica cambia;
- una transición todavía no comprometida deja de ser segura;
- aparece una ruta claramente mejor;
- la mascota deja de progresar.

### No replanificar por

- pequeñas oscilaciones del dueño;
- un único tick de cambio de contacto;
- cada cambio de yaw/cámara;
- un cambio de gravedad del dueño que no altere de forma relevante el objetivo espacial.

Durante `TRANSITION COMMITTED`, el dueño puede hacer lo que quiera: la mascota termina o aborta de forma segura su maniobra antes de replantear.

## 13. Dueño en Gravity Fall prolongado

Una mascota no debe convertirse en un misil que replica cada decisión aérea.

Comportamiento preferido:

- mantener tracking desde superficies alcanzables;
- usar predicción de aterrizaje del dueño cuando sea fiable;
- buscar una ruta hacia la zona donde probablemente vuelva a haber soporte;
- no lanzarse a una transición sin destino seguro sólo porque el dueño pase cerca en el aire;
- si la separación se vuelve extrema y no existe ruta práctica, entrar en fallback de seguimiento/teleport seguro.

## 14. Fallback de teletransporte

Debe conservar la expectativa vanilla de que una mascota no desaparezca para siempre por un problema de pathfinding.

Sólo se activa tras criterios como:

- distancia excesiva;
- ausencia de plan seguro;
- varios ciclos sin progreso;
- separación brusca causada por teleport/movimiento del dueño.

El destino se busca como **estado de soporte seguro cerca del dueño**, no simplemente como una coordenada libre.

Preferencias:

1. pose segura cerca del dueño con mínima discontinuidad de gravedad;
2. si el animal tiene capacidad legítima para ello, otra gravedad que produzca una pose estable cercana;
3. nunca aparecer dentro de geometría, sobre hazard o en un estado inmediatamente inestable.

Teleport no debe ocultar fallos normales de planificación a distancias donde caminar/cambiar gravedad debería funcionar.

## 15. Agua y otros medios

El planificador de superficies se suspende cuando la mascota entra realmente en un medio donde la locomoción normal deja de ser “caminar sobre soporte”.

En agua se favorece seguimiento/swimming normal y se reanuda el plan gravitatorio al recuperar un estado de soporte sólido válido.

Una transición planificada que entra inesperadamente en fluido deja de considerarse válida y pasa a recovery/seguimiento acuático.

## 16. Superficies móviles

Los estados sobre entidades/plataformas móviles son volátiles.

Pueden existir, pero:

- una transición hacia ellos debe revalidarse inmediatamente antes de comprometerse;
- su coste/riesgo debe reflejar menor estabilidad predictiva;
- cualquier desplazamiento que invalide el landing esperado fuerza replanning/recovery.

## 17. Garantía de progreso

El algoritmo necesita detectar que está dando vueltas.

Se mide progreso respecto al objetivo/plan, no sólo “la navigation no está done”.

Si durante varios intentos no mejora:

1. invalidar temporalmente la maniobra que acaba de fallar;
2. ampliar el horizonte/local search;
3. considerar rutas con una transición adicional;
4. finalmente usar fallback seguro.

Una misma maniobra fallida no debe intentarse eternamente en el mismo punto.

## 18. Diagrama de flujo conceptual

```text
                 ┌──────────────────────┐
                 │ Estado actual dueño  │
                 │ + geometría actual   │
                 └──────────┬───────────┘
                            │
                            v
                 ┌──────────────────────┐
                 │ Construir conjunto   │
                 │ de estados objetivo  │
                 └──────────┬───────────┘
                            │
                            v
              ┌─────────────────────────────┐
              │ ¿Mascota en estado estable? │
              └───────┬─────────────┬───────┘
                      │sí           │no
                      v             v
        ┌────────────────────┐   ┌────────────────────┐
        │ ¿Ruta con gravedad │   │ Terminar/monitorizar│
        │ actual suficiente? │   │ transición o recovery│
        └──────┬────────┬────┘   └─────────┬──────────┘
               │sí      │no                │
               v        v                  │
      ┌─────────────┐  ┌───────────────────────────┐
      │ Caminar y   │  │ Generar sólo transiciones│
      │ seguir      │  │ físicamente seguras       │
      └──────┬──────┘  └────────────┬──────────────┘
             │                      │
             │                      v
             │            ┌────────────────────────┐
             │            │ Buscar ruta en grafo   │
             │            │ de estados de soporte  │
             │            └───────┬────────┬───────┘
             │                    │hay     │no hay
             │                    v        v
             │          ┌──────────────┐ ┌──────────────┐
             │          │ Ir al launch│ │ ampliar /     │
             │          │ + revalidar │ │ fallback seguro│
             │          └──────┬───────┘ └──────────────┘
             │                 │
             │                 v
             │          ┌──────────────┐
             │          │ Commit giro  │
             │          │ + física     │
             │          └──────┬───────┘
             │                 │
             │                 v
             │       ┌─────────────────────┐
             │       │ ¿soporte estable y  │
             │       │ seguro adquirido?   │
             │       └───────┬──────┬──────┘
             │               │sí    │desvío/peligro
             │               v      v
             └──────────> REPLAN   RECOVERY
```

## 19. Por qué prefiero este modelo

### Frente a breadcrumbs

- no depende de dónde estuvo el dueño;
- no confunde una posición pasada con una maniobra válida;
- se adapta a geometría cambiante;
- una mascota que parte desde otro sitio puede elegir otra ruta;
- el dueño puede hacer maniobras absurdamente complejas y la mascota seguir de forma sencilla si existe una ruta mejor.

### Frente a “pon la gravedad hacia el dueño”

- evita estamparse contra bloques intermedios;
- contempla el volumen real del animal;
- sabe dónde pretende terminar;
- puede resolver obstáculos que requieren alejarse primero del dueño.

### Frente a pathfinding 3D libre

- conserva el lenguaje físico del mod: caminar sobre una superficie y realizar transiciones gravitatorias reales;
- reutiliza navegación normal en cada superficie;
- el espacio de búsqueda está mucho más acotado;
- el resultado sigue pareciendo un animal de Minecraft, no un mob volador invisible.

## 20. Preguntas que deben pulirse con playtest antes de implementar

El algoritmo base puede fijarse ya, pero estos parámetros son deliberadamente de tuning:

- radio preferido de seguimiento antes de considerar necesario cambiar gravedad;
- cuánto debe ganar una ruta alternativa para romper la histéresis del plan actual;
- horizonte máximo de simulación de una transición;
- daño máximo aceptable (idealmente cero salvo situaciones de emergencia);
- cuánta zona libre/egress se exige tras el landing;
- cuándo declarar falta de progreso;
- a qué distancia activar fallback vanilla/teleport;
- tratamiento exacto de superficies móviles;
- si una futura versión de Reorientation permitirá planes con más de un cambio gravitatorio dentro de la misma fase aérea.

## 21. Criterio de éxito de diseño

Una mascota con capacidad gravitatoria debe poder seguir al dueño por suelo, paredes y techos sin necesitar conocer la ruta histórica del jugador y sin efectuar un cambio de gravedad cuya consecuencia predecible sea quedar atrapada, sufrir un impacto peligroso o chocar lateralmente contra geometría.

El objetivo observable no es que reproduzca las mismas maniobras. Es que, cuando exista una ruta razonable y segura, **encuentre su propia ruta y siga llegando viva**.