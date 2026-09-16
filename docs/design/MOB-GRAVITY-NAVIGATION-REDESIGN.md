# Rediseño general — navegación gravitatoria de mobs

Estado: **DISEÑO / SIN IMPLEMENTACIÓN**.

Este documento generaliza el trabajo inicial de `PET-GRAVITY-FOLLOW-REDESIGN.md`. El hallazgo central es que el problema no es específico de mascotas: cualquier mob que tenga capacidad legítima de cambiar gravedad puede usar el mismo razonamiento para perseguir, seguir, huir, patrullar o alcanzar cualquier objetivo de IA.

No se describen clases, mixins ni estructura de implementación. Se define la lógica del algoritmo y sus invariantes de gameplay, seguridad y coste.

## 1. Separación fundamental: objetivo vs. locomoción

La IA de alto nivel decide **qué quiere conseguir**. El planificador gravitatorio decide **cómo desplazarse físicamente para conseguirlo**.

Ejemplos de objetivo:

- mascota: mantenerse cerca de su dueño;
- zombie: entrar en alcance de ataque de su objetivo;
- mob que huye: aumentar su ventaja frente a una amenaza;
- patrulla: alcanzar una región o punto;
- wander: alcanzar una región elegida por la IA normal;
- regreso a casa/nido: alcanzar un volumen de destino.

El planificador no debe contener semántica específica de “owner”. Recibe un conjunto dinámico de estados/zonas deseables y las capacidades del mob.

Consecuencia de gameplay: un zombie con Clinging o Reorientation puede perseguir al jugador por suelo, paredes o techo si eso es una ruta útil y legal para él.

## 2. El mundo se razona como estados de soporte conectados por maniobras

La unidad básica sigue siendo un **estado de soporte estable**:

- posición/pose habitable para ese mob;
- dirección de gravedad;
- soporte efectivo bajo esa gravedad;
- capacidad local de empezar a desplazarse;
- contexto de medio relevante;
- capacidad gravitatoria disponible del mob.

Dos estados pueden conectarse mediante:

- **navegación superficial normal** bajo la misma gravedad;
- **transición gravitatoria**, entendida como maniobra completa desde una zona de lanzamiento hasta otro soporte;
- acciones de recuperación permitidas por la IA base, como el teletransporte vanilla de una mascota cuando corresponda.

No se voxeliza conceptualmente todo el espacio 3D. El mob sigue siendo una criatura que se mueve sobre superficies y realiza transiciones entre ellas, no un dron volador disfrazado.

## 3. Prioridad: usar navegación normal si ya resuelve el objetivo

Antes de explorar cambios gravitatorios se comprueba si el objetivo actual es razonablemente alcanzable caminando bajo la gravedad presente.

Si sí, se mantiene navegación normal.

Una transición gravitatoria sólo entra en consideración cuando aporta valor real: desbloquea una región inaccesible, acorta materialmente la ruta, permite perseguir/escapar mejor o recupera progreso perdido.

Esto evita que mobs con el efecto se conviertan automáticamente en acróbatas hiperactivos.

## 4. Una transición gravitatoria es una acción completa

No se evalúa “cambiar a EAST”. Se evalúa:

**llegar a una zona de lanzamiento → cambiar a EAST → recorrer la trayectoria física → contactar con una superficie concreta → quedar en un estado posterior utilizable**.

La valoración usa:

- tamaño/pose reales del mob;
- velocidad prevista al comprometer la maniobra;
- gravedad candidata y aceleración efectiva;
- geometría de colisión atravesada por todo el volumen corporal;
- superficie de llegada;
- espacio final y capacidad de seguir moviéndose;
- peligros del medio;
- daño esperado;
- incertidumbre de la predicción.

Un cambio no se considera completado cuando cambia el atributo de gravedad, sino cuando la criatura adquiere un soporte suficientemente estable bajo la nueva gravedad.

## 5. Imposibilidad, peligro y coste no son lo mismo

Se corrige el criterio inicial de “cualquier ruta dañina se descarta”. Eso sería demasiado conservador y produciría decisiones absurdas en persecución, huida o seguimiento urgente.

### 5.1 Restricciones duras

Se descartan estados/manobras que no constituyen una ruta física válida, por ejemplo:

- el volumen corporal no cabe;
- la trayectoria atraviesa geometría sólida inevitablemente de forma incompatible con la física prevista;
- el estado terminal implica atrapamiento/sufocación geométrica sin salida razonable;
- sale de límites válidos del mundo;
- depende de geometría no disponible que no puede tratarse razonablemente como libre;
- requiere una capacidad gravitatoria que el mob no posee.

### 5.2 Riesgo como coste

El daño posible o seguro **no convierte automáticamente la ruta en imposible**.

El coste debe penalizar fuertemente:

- corazones/vida que previsiblemente se pierden;
- fracción de vida total que representa ese daño;
- riesgo de impacto adicional por incertidumbre;
- aterrizajes estrechos o de escasa robustez;
- atravesar hazards que dañan a ese tipo de mob.

La penalización por daño debe crecer de manera fuertemente no lineal: perder medio corazón puede ser aceptable; perder la mayor parte de la vida debe resultar extraordinariamente caro. Aun así, una IA con un objetivo suficientemente prioritario puede escoger una ruta dañina si todas las alternativas son peores.

Esto permite comportamientos emergentes razonables: una criatura acorralada puede tirarse a una pared inferior para escapar aun sabiendo que recibirá daño.

### 5.3 Muerte casi segura

No necesita convertirse conceptualmente en “imposible” salvo que la geometría lo sea, pero su coste debe ser tan extremo que sólo gane cuando el objetivo/estado alternativo sea igualmente catastrófico. La política concreta puede variar por tipo de objetivo o mob.

## 6. Función de coste dependiente del objetivo

Existe una base común:

- tiempo estimado;
- distancia/navegación restante;
- número de cambios gravitatorios;
- tiempo sin soporte;
- daño esperado;
- fragilidad/incertidumbre del aterrizaje;
- coste de abandonar un plan ya estable.

Pero el objetivo añade su propia utilidad.

### Seguir dueño

Favorecer recuperar proximidad y no quedar rezagado. Compartir gravedad con el dueño es una preferencia secundaria, nunca obligación.

### Perseguir/atacar

Favorecer tiempo hasta alcance efectivo, línea de acceso y capacidad de continuar persiguiendo después del aterrizaje.

### Huir

No equivale a “andar hacia atrás respecto del enemigo”. El mob debe buscar estados que aumenten su **ventaja de escape**.

Una pared o techo puede ser mejor que retroceder por el mismo suelo si:

- aumenta el tiempo que la amenaza necesita para alcanzarlo;
- rompe línea de visión o acceso directo;
- coloca al perseguidor en una topología menos favorable;
- abre más rutas de continuación.

Por tanto, un mob con Clinging/Reorientation puede escapar enlazándose deliberadamente a una pared o techo si esa maniobra mejora el escape.

Un buen criterio conceptual de huida no es sólo distancia euclídea, sino algo parecido a:

**tiempo estimado del perseguidor hasta mí − tiempo que necesito para alcanzar el siguiente estado seguro**, combinado con cobertura, rutas posteriores y daño esperado.

No hace falta resolver exactamente el pathfinding completo del perseguidor en cada decisión; basta una estimación local razonable y acotada.

## 7. Seguimiento de objetivos móviles

El objetivo se observa continuamente, también durante una transición aérea. Se descarta la idea de congelarlo por completo hasta el próximo aterrizaje.

Pero observar no significa obedecer cada cambio instantáneo.

### 7.1 Target filtrado

La IA mantiene una **intención de destino** que se actualiza cuando ocurre alguna de estas cosas:

- el objetivo se ha desplazado materialmente respecto del plan;
- su tendencia de movimiento persistente cambia la región a la que conviene dirigirse;
- el plan actual deja de reducir la distancia/coste hacia el objetivo;
- ha pasado suficiente tiempo como para que una previsión antigua deje de ser representativa;
- el objetivo teleporta/cambia de dimensión o existe una discontinuidad equivalente.

Pequeños zigzags o cambios gravitatorios momentáneos del objetivo no deben provocar un nuevo plan completo.

### 7.2 Vuelo/transición comprometida

Durante una maniobra aérea el mob sigue observando el objetivo y recalculando si el **destino estratégico** de la maniobra sigue teniendo sentido.

No debe perseguir cada giro en tiempo real, pero tampoco puede mantener durante miles de bloques un destino que quedó obsoleto al principio del vuelo.

Se distinguen tres situaciones:

1. **Cambio pequeño:** se mantiene la maniobra actual.
2. **Cambio material pero la maniobra aún termina pronto y en un estado útil:** se termina la maniobra y el siguiente plan ya usa el objetivo actualizado.
3. **Divergencia grande/prolongada:** se reevalúa la estrategia durante el vuelo. Si las capacidades del mob permiten corregir de forma legal y segura, puede elegirse una nueva maniobra; si no, se conserva la mejor trayectoria físicamente disponible y se prepara recuperación/fallback.

Clinging sigue limitado por su regla de un cambio aéreo antes de recuperar soporte. Reorientation puede permitir cambios adicionales, pero deben ser decisiones discretas con histéresis, no seguimiento servo de cada movimiento del objetivo.

### 7.3 Vuelos extremadamente largos

Una transición no puede permanecer “comprometida” indefinidamente sólo porque todavía no haya aterrizado. Si no aparece soporte dentro de un horizonte útil o el objetivo ha migrado radicalmente, el estado pasa a planificación aérea/recovery según las capacidades reales del mob.

Esto evita el caso absurdo de un dueño que recorre una distancia enorme mientras la mascota sigue volando obedeciendo una intención antigua en dirección contraria.

## 8. Mundo dinámico y tiempo de reacción

El plan no presupone que el mundo permanezca congelado. Bloques, entidades y superficies móviles pueden alterar una trayectoria ya validada.

Sin embargo, la IA tampoco debe tener reflejos sobrenaturales.

### 8.1 Monitorización de una maniobra

Mientras ejecuta una transición, se comprueba periódicamente si la envolvente próxima de la trayectoria continúa siendo válida.

Un bloque colocado posteriormente delante del mob puede convertir una trayectoria segura en una colisión.

### 8.2 Reacción deliberadamente no instantánea

Se introduce conceptualmente un **tiempo de percepción/reacción**.

- Si aparece un obstáculo con margen suficiente antes del impacto, el mob puede reconocerlo y buscar una corrección/replanificación permitida por sus capacidades.
- Si aparece demasiado tarde, el mob se lo come. Eso es correcto: un jugador que coloca un bloque literalmente en su cara no debería ser derrotado por clarividencia algorítmica.
- Tras el golpe, la IA reevalúa desde el estado físico real resultante.

La reacción puede depender en el futuro del tipo de criatura/dificultad, pero el primer diseño debe usar un comportamiento coherente y legible, no reflejos de un tick.

### 8.3 Emergencia

Si el obstáculo aparece dentro del tiempo de reacción pero existe una corrección legal clara, se prioriza evitar un resultado catastrófico aunque la nueva ruta se aleje temporalmente del objetivo.

Una mascota puede intentar salvarse antes de continuar hacia el dueño; un mob en huida puede aceptar daño si la alternativa es dejarse alcanzar.

## 9. Eficiencia como requisito del algoritmo

El planificador debe ser utilizable por muchos mobs simultáneos. No puede ejecutar una búsqueda 3D completa y costosa para cada zombie en cada tick.

### 9.1 Jerarquía de trabajo

Orden de menor a mayor coste:

1. reutilizar el plan vigente si sigue siendo válido;
2. navegación normal con gravedad actual;
3. estimación barata de si una transición gravitatoria aportaría una mejora material;
4. búsqueda local de transiciones relevantes;
5. ampliación progresiva del horizonte sólo si lo anterior falla;
6. fallback permitido por la IA concreta.

### 9.2 Búsqueda local y perezosa

No se enumeran todos los soportes del mundo. Sólo se exploran estados cercanos/relevantes para el objetivo y transiciones que podrían mejorar el plan.

Las posiciones de lanzamiento se buscan prioritariamente en regiones que tengan sentido estratégico: fronteras de la superficie alcanzable, zonas orientadas hacia regiones prometedoras, lugares desde los que aparece una superficie terminal útil, etc.

### 9.3 Presupuesto temporal

La planificación puede extenderse durante varios ticks. Cada mob recibe trabajo acotado y conserva el frente de búsqueda entre actualizaciones conceptualmente, en vez de recomenzar todo cada frame lógico.

El gameplay debe preferir un mob que tarda unas décimas en decidir una maniobra razonable a un servidor que decide brillantemente la ruta de 80 zombies y deja de respirar durante medio segundo.

### 9.4 Reutilización de conocimiento del mundo

Resultados sobre geometría estática o topología local pueden reutilizarse mientras esa región no cambie. Una modificación relevante del mundo invalida sólo el conocimiento afectado, no toda la navegación gravitatoria global.

El estado específico del mob sigue importando porque tamaño, salud, efectos, velocidad y capacidades pueden hacer que una transición sea válida para uno e inútil para otro.

### 9.5 Planificación distribuida

No es necesario que todos los mobs replanifiquen en el mismo tick. Salvo emergencia, las revisiones completas pueden repartirse temporalmente.

### 9.6 Complejidad creciente sólo cuando aporta gameplay

El algoritmo base debe poder producir buenas decisiones con una o pocas transiciones de horizonte. Las búsquedas profundas sólo se justifican cuando no existe una solución próxima.

## 10. Histéresis y estabilidad

Una ruta alternativa debe ser materialmente mejor para desplazar a una ruta vigente que sigue funcionando.

Se penalizan:

- cambiar de plan sin ganancia clara;
- encadenar cambios gravitatorios por pequeñas fluctuaciones del objetivo;
- volver inmediatamente a una superficie que acaba de abandonarse salvo que cambie el contexto.

Durante persecución intensa puede reducirse esta resistencia; durante follow pasivo puede aumentarse.

## 11. Ejemplos

### 11.1 Zombie con Reorientation persigue al jugador en un techo

- El zombie intenta primero una ruta normal en su suelo actual.
- Detecta que no puede entrar en alcance útil.
- Explora transiciones locales a pared/techo.
- Compara tiempo, riesgo y continuidad posterior.
- Camina hasta la zona de lanzamiento elegida.
- Revalida, cambia gravedad y completa la transición.
- Desde el nuevo soporte vuelve a usar navegación normal hacia el jugador.

No está “imitando” un cambio que haya hecho el jugador. Está resolviendo su propio problema de persecución.

### 11.2 Mob intenta huir

- Retroceder por el suelo deja al perseguidor a dos segundos.
- Una transición EAST hacia una pared causa un corazón de daño pero rompe acceso directo y deja varias rutas laterales.
- La segunda opción puede ganar porque el coste de daño es grande pero la ganancia de supervivencia es mayor.

### 11.3 Jugador coloca un bloque en la trayectoria

- La maniobra era segura cuando se comprometió.
- El mundo cambia.
- Si el obstáculo aparece con suficiente antelación, tras el tiempo de reacción el mob intenta una corrección legal.
- Si aparece a quemarropa, impacta.
- El impacto no se interpreta como fallo del planificador original: es nueva información. Se replanifica desde el resultado real.

### 11.4 Mascota siguiendo un vuelo largo

- La mascota sigue observando al dueño durante su transición.
- No reacciona a cada pequeño giro.
- Si la tendencia del dueño cambia de forma persistente y el destino previsto deja de ser útil, actualiza su objetivo estratégico.
- Si puede corregir legalmente, lo hace con histéresis; si Clinging no permite otro giro, termina/recupera de la mejor forma física posible y replanifica cuanto antes.

## 12. Relación con el diseño inicial de mascotas

`PET-GRAVITY-FOLLOW-REDESIGN.md` sigue siendo útil como primera exploración y como caso de uso de seguimiento. Este documento lo generaliza y modifica en cuatro puntos importantes:

1. el planificador es de mobs, no de mascotas;
2. el objetivo es intercambiable (follow/chase/flee/etc.);
3. el daño es principalmente un coste fuerte, no un veto absoluto;
4. durante el vuelo el objetivo sigue siendo observado mediante tracking filtrado y puede forzar una replanificación material.

Estas cuatro decisiones deben considerarse la dirección de diseño más reciente.

## 13. Preguntas abiertas para playtest/diseño

- ¿Cuánto daño esperado debe aceptar una mascota para recuperar a su dueño?
- ¿Debe variar la aversión al riesgo según mob/objetivo?
- ¿Cuál es el tiempo de reacción que parece inteligente sin parecer precognitivo?
- ¿Cuánta divergencia del objetivo justifica romper un plan aéreo?
- ¿En qué casos Reorientation debe permitir una segunda transición aérea planificada?
- ¿Qué profundidad máxima de búsqueda produce comportamiento convincente sin coste excesivo?
- ¿Qué diferencias de dificultad deberían afectar capacidad de planificación, si alguna?

La respuesta a estas preguntas debe salir de escenarios de playtest reproducibles, no de constantes elegidas por intuición aislada.