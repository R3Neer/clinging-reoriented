# Refinamiento — tiempo de reacción de la navegación gravitatoria

Estado: **DECISIÓN DE DISEÑO / SIN IMPLEMENTACIÓN**.

Este documento refina `MOB-GRAVITY-NAVIGATION-REDESIGN.md`, especialmente su apartado de mundo dinámico y reacción. Debe considerarse la dirección de diseño más reciente para el tiempo de reacción de mobs. No introduce todavía constantes definitivas ni estructura de código.

## 1. Objetivo

Queremos que un mob pueda reaccionar a cambios del mundo durante una ruta gravitatoria —por ejemplo, un bloque colocado delante de una trayectoria que antes era segura— sin tener reflejos sobrenaturales y sin mantener una tabla manual de tiempos de reacción para cada especie.

La reacción debe sentirse coherente con la agilidad que Minecraft ya comunica mediante las propias características del mob.

## 2. Proxy principal: velocidad base de movimiento

La señal principal será la **capacidad/base stat de velocidad de movimiento del mob**, no su velocidad instantánea en ese momento y, en particular, no la velocidad que haya adquirido cayendo.

Regla conceptual:

**mayor velocidad base de movimiento → menor latencia de reacción**

**menor velocidad base de movimiento → mayor latencia de reacción**

La razón de diseño no es que correr rápido implique literalmente pensar rápido, sino que la velocidad base ya funciona en Minecraft como un buen proxy general de agilidad. Hace que mobs que se perciben rápidos y nerviosos respondan antes, mientras criaturas lentas y pesadas reaccionan con más demora, sin mantener parámetros específicos por entidad.

Ejemplos de sensación buscada:

- una criatura muy ágil debe poder detectar con cierta antelación que su landing ha quedado bloqueado y corregir antes;
- una criatura lenta debe comprometerse más con la maniobra y ser más fácil de sorprender colocando geometría tarde;
- ninguna criatura debe responder instantáneamente en el mismo instante lógico en que aparece el obstáculo.

## 3. No usar velocidad física instantánea como reflejos

Hay que separar dos conceptos:

### Latencia de percepción/reacción

Depende principalmente del **stat de movimiento/agilidad del mob**.

### Urgencia y viabilidad física

Dependen de la **trayectoria real**, incluida velocidad instantánea, distancia al obstáculo y tiempo restante hasta el impacto.

Por tanto, caer a enorme velocidad no convierte mágicamente a un mob lento en uno con mejores reflejos. Al contrario: si su latencia normal consume casi todo el tiempo disponible antes del choque, puede que no llegue a reaccionar y se estrelle.

Esto permite situaciones legibles y justas:

- un mob rápido puede empezar a corregir antes;
- un mob lento puede ver conceptualmente el mismo cambio demasiado tarde para hacer algo útil;
- un bloque colocado literalmente a quemarropa puede provocar impacto incluso en un mob ágil.

## 4. Qué valor de velocidad usar

La referencia preferida debe representar la **agilidad característica** del mob, no oscilaciones accidentales de la física.

Por defecto:

- partir de su atributo/base capability de movimiento;
- no derivarlo de `deltaMovement`, velocidad de caída, knockback ni momentum actual;
- evitar que multiplicadores momentáneos de un goal conviertan artificialmente al mob en “más inteligente”;
- los efectos temporales que sólo alteran locomoción no deberían obligatoriamente alterar reflejos; si en playtest se decide que Speed/Slowness deben modular también reacción, eso será una decisión separada y explícita.

La intención es obtener un comportamiento por especie/variante a partir de datos que ya existen, no crear una lista manual de excepciones.

## 5. Mapeo no lineal y acotado

No se desea una relación lineal sin límites.

El mapeo velocidad→latencia debe ser:

- **monótono**: más agilidad nunca produce peor reacción;
- **suave**: pequeñas diferencias de stat no generan saltos bruscos de comportamiento;
- **acotado por abajo**: ni el mob más rápido obtiene reacción de cero ticks ni clarividencia;
- **acotado por arriba**: un mob lento pero capaz de navegar no tarda una eternidad en responder;
- **saturante** en los extremos: duplicar una velocidad ya muy alta no debe convertir la reacción en prácticamente instantánea.

No se fijan todavía tiempos mínimo/máximo. Deben salir de playtest con varios mobs representativos.

## 6. Tiempo de reacción no significa escaneo caro cada tick

El planificador puede detectar/monitorizar cambios de trayectoria con trabajo acotado y distribuido, pero una observación no se convierte en acción hasta superar la latencia correspondiente del mob.

Esto mantiene separados:

- **coste computacional de vigilar**;
- **tiempo de reacción que percibe el jugador**.

No es necesario recalcular una ruta gravitatoria completa cada tick. La monitorización puede primero hacer comprobaciones baratas de la envolvente próxima y sólo activar una replanificación costosa si aparece una desviación relevante y ha transcurrido el tiempo de reacción.

## 7. Obstáculo aparecido durante una transición

Secuencia conceptual:

1. la maniobra fue validada y el mob se compromete;
2. el mundo cambia;
3. la monitorización detecta que la trayectoria prevista ya no coincide con el mundo;
4. se inicia/reconoce la ventana de reacción del mob;
5. si el impacto ocurre antes de que pueda reaccionar, el mob impacta;
6. si hay tiempo suficiente, reevalúa las acciones que sus capacidades permiten;
7. sólo ejecuta una corrección que sea legal para Clinging/Reorientation y físicamente viable;
8. después de cualquier impacto o corrección, el estado físico real vuelve a ser la fuente de verdad.

El jugador debe poder interceptar o sorprender a un mob mediante cambios tardíos del mundo. La IA no debe ganar por conocer el futuro.

## 8. Relación con Clinging y Reorientation

El tiempo de reacción no concede capacidades nuevas.

- **Clinging:** si el cambio aéreo ya fue consumido, detectar el peligro antes no autoriza un segundo giro. El mob puede preparar recovery, aprovechar locomoción legítima o aceptar el impacto.
- **Reorientation:** puede existir una corrección aérea adicional si el planificador determina que es útil, ha transcurrido la reacción y se superan las reglas de histéresis/seguridad/coste.

Una criatura puede percibir un peligro y, aun así, no tener una acción física válida para evitarlo.

## 9. Interacción con objetivos móviles

La misma filosofía se aplica al seguimiento de un owner/target que cambia de rumbo:

- el objetivo se observa continuamente;
- pequeños cambios no disparan una decisión;
- un cambio material debe persistir o superar umbrales suficientes;
- la capacidad del mob para reaccionar a esa nueva intención puede modularse por la misma noción de agilidad, pero con umbrales distintos de una emergencia de colisión;
- reaccionar más rápido no significa replanificar a cada tick.

En especial, durante vuelos muy largos el target nunca queda congelado: se sigue actualizando la intención estratégica, pero con filtrado, histéresis y presupuesto de planificación.

## 10. Eficiencia que debe conservarse

Este refinamiento no cambia los principios del planificador general:

- reutilizar el plan vigente mientras funcione;
- usar navegación vanilla/normal antes de invocar planificación gravitatoria;
- explorar sólo transiciones locales/prometedoras;
- presupuestar búsquedas a lo largo de varios ticks;
- repartir replans de distintos mobs;
- reutilizar topología/geometría estática cuando sea posible;
- reservar simulaciones volumétricas más caras para candidatos que ya pasaron filtros baratos;
- replanificar de emergencia sólo ante cambios materiales.

El tiempo de reacción variable no puede convertirse en una excusa para hacer más trabajo por tick.

## 11. Playtest necesario

La curva definitiva velocidad-base→reacción debe compararse, como mínimo, entre:

- un mob claramente lento;
- uno de movilidad media;
- uno claramente rápido;
- mascota siguiendo;
- hostil persiguiendo;
- mob huyendo;
- Clinging con giro aéreo agotado;
- Reorientation con posibilidad de corrección;
- obstáculo colocado con mucha, media y casi ninguna antelación.

Criterio perceptual: **un mob rápido debe sentirse más despierto, no omnisciente; uno lento debe sentirse menos reactivo, no deliberadamente estúpido**.

## 12. Decisión consolidada

La dirección actual queda así:

**El planificador gravitatorio es general para mobs con capacidad de Clinging/Reorientation. La IA de alto nivel aporta el objetivo; el planificador elige locomoción sobre superficies y transiciones gravitatorias. El daño es coste fuerte y no veto absoluto. Los mobs pueden usar cambios de gravedad para perseguir, seguir o huir. Los objetivos móviles se siguen también durante el vuelo mediante tracking filtrado. El mundo dinámico puede invalidar rutas y el mob reacciona tras una latencia finita. Esa latencia se deriva por defecto de su velocidad base de movimiento como proxy de agilidad, mediante una curva suave, saturante y acotada. El sistema mantiene planificación jerárquica, perezosa y presupuestada para seguir siendo viable con muchos mobs simultáneos.**
