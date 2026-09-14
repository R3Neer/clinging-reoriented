# Shulker Charge — especificación temporal

Estado: **ESPECIFICACIÓN / NO IMPLEMENTADO**.

Rama de trabajo: `feature/shulker-charge`, basada en `main` / `v0.1.0-alpha.13` (`be3b47284a71fbf4fcacb2ab2b2cb3776af290d3`).

Este documento fija el comportamiento acordado antes de implementar. La implementación no debe redefinir estas reglas por conveniencia técnica; cualquier cambio deliberado debe volver primero a esta especificación.

## 1. Objetivo

Introducir **Shulker Charge** como objeto capturable y relanzable que materializa una shulker bullet y que sustituye a la Shulker Shell como ingrediente de Reorientation.

La identidad buscada es:

- Fire Charge: fuego.
- Wind Charge: impulso.
- **Shulker Charge: proyectil guiado / levitación capturado de un shulker.**

La mecánica debe sentirse derivada del lenguaje vanilla de shulkers, proyectiles y Target Blocks, no como un misil moderno injertado sobre Minecraft.

## 2. Objeto

SC-001. `Shulker Charge` es un objeto apilable hasta **64**, igual que Fire Charge y Wind Charge.

SC-002. Para renderizado en mano se reutilizará/adaptará el **modelo 3D de la shulker bullet / Shulker Charge**. El icono 2D de inventario queda pendiente de seleccionar o crear con licencia compatible.

SC-003. La Shulker Charge es un recurso consumible al lanzar y vuelve a existir como item sólo si el proyectil es interceptado activamente según las reglas de captura.

## 3. Captura de shulker bullets

SC-010. Una shulker bullet vanilla/natural destruida por **ataque melee** deja exactamente **1 Shulker Charge**.

SC-011. Una shulker bullet vanilla/natural destruida por **flecha** deja exactamente **1 Shulker Charge**. Esto incluye flechas disparadas por jugador y por dispenser.

SC-012. Bloquearla con escudo la neutraliza sin drop.

SC-013. Impactar contra un bloque, impactar normalmente contra una entidad, expirar o desaparecer por una causa no clasificada como interceptación activa no deja Shulker Charge.

SC-014. La obtención puede ser renovable/automatizable en late game; no se debe introducir artificialmente una regla que lo impida.

## 4. Uso manual y por dispenser

SC-020. Clic derecho con una Shulker Charge lanza una Shulker Charge projectile y consume una unidad.

SC-021. Un dispenser puede lanzar la misma entidad-proyectil usando su `facing` como dirección de intención.

SC-022. El punto inicial de tuning para uso manual es un **cooldown de 0,5 s**, siguiendo el precedente de Wind Charge. Es un valor de playtesting, no una excusa para cambiar otras reglas de identidad.

SC-023. Si no existe target válido al lanzar, el proyectil sale igualmente hacia delante; el uso nunca falla silenciosamente sólo por ausencia de objetivo.

## 5. Navegación y lenguaje visual

SC-030. El proyectil debe conservar el lenguaje de movimiento de una shulker bullet: navegación ortogonal/cardinal, tramos y giros de 90°, partículas/feedback compatibles y capacidad de rodear obstáculos.

SC-031. No se sustituye ese lenguaje por homing curvo continuo.

SC-032. El proyectil debe mantener el comportamiento ofensivo esencial de la shulker bullet: impacto, daño y Levitation compatibles con vanilla salvo que los tests revelen una razón explícita para adaptar algún detalle.

## 6. Adquisición de objetivo

SC-040. Para un jugador, la intención inicial es el rayo `ojos -> dirección real de cámara/crosshair` al lanzar.

SC-041. Para un dispenser, la intención inicial es `boca/centro de salida -> facing`.

SC-042. Los candidatos son **entidades válidas** y **Target Blocks**.

SC-043. Un Target Block alcanzado directamente por el rayo de intención tiene prioridad absoluta.

SC-044. En adquisición asistida, gana principalmente el candidato más cercano angularmente/a menor distancia perpendicular respecto de la línea de intención; la distancia al proyectil/tirador actúa como criterio secundario. No se selecciona simplemente “la entidad más cercana”.

SC-045. Se parte de un alcance de adquisición aproximado de **32 bloques** y un cono estrecho de alrededor de **10–15°** como valores de prototipo sujetos a playtesting.

SC-046. La adquisición inicial de una entidad requiere línea de visión. Tras adquirirla, perder línea de visión por una esquina u obstáculo no cancela el lock: la navegación de shulker debe poder rodear obstáculos.

SC-047. El proyectil no abandona arbitrariamente un objetivo que sigue siendo válido para elegir uno mejor.

## 7. Readquisición durante el vuelo

SC-050. Un proyectil sin objetivo puede adquirir uno nuevo durante el vuelo.

SC-051. Un proyectil cuyo objetivo deja de existir o deja de ser válido puede adquirir uno nuevo. Casos claros: entidad muerta/eliminada/desconectada/cambio de dimensión; Target Block retirado o sustituido.

SC-052. Ocultarse detrás de una pared no invalida por sí solo el objetivo.

SC-053. La readquisición conserva la **dirección de intención original** del lanzamiento como referencia de puntería y utiliza la posición actual de la Charge para determinar candidatos alcanzables.

SC-054. El scan debe ser periódico y barato, no global ni cada tick por necesidad ficticia. Punto de partida: cada ~4 ticks mientras no haya target válido.

## 8. Target Blocks y redstone

SC-060. La Shulker Charge puede fijar y golpear `minecraft:target`.

SC-061. El impacto debe pasar por el comportamiento real de proyectil/Target Block para producir señal redstone vanilla, no mediante activación remota artificial.

SC-062. La combinación dispenser + Shulker Charge + Target Block es una interacción de construcción/redstone intencionada.

## 9. Intercepción de una Charge ya lanzada

SC-070. Una Shulker Charge projectile golpeada por melee se destruye y deja **1 Shulker Charge item**.

SC-071. Una Shulker Charge projectile destruida por flecha también deja **1 Shulker Charge item**.

SC-072. Esto se aplica aunque la Charge la hubiera lanzado otro jugador o el propio interceptor. Es transferencia/recuperación del mismo recurso, no duplicación.

SC-073. Un escudo neutraliza el proyectil sin devolver el item.

SC-074. Un impacto normal contra entidad/bloque o la expiración consume definitivamente esa Charge y no genera drop.

SC-075. No se implementa la semántica de Wind Charge de “golpear para redirigir” como sustituto de la captura. La identidad propia de Shulker Charge es que puede **volver a capturarse**.

## 10. Shulkers y duplicación vanilla

SC-080. Una Shulker Charge lanzada conserva deliberadamente la capacidad de una shulker bullet de participar en la **duplicación/reproducción vanilla de shulkers** cuando impacta a un shulker y se cumplen las condiciones vanilla pertinentes.

SC-081. No se añade una excepción por origen jugador/dispenser que desactive esa interacción.

SC-082. Esta consecuencia renovable/automatizable se considera parte del gameplay emergente buscado, no un exploit a eliminar de antemano.

## 11. Reorientation

SC-090. La receta de Reorientation deja de usar `Items.SHULKER_SHELL` y pasa a consumir **Shulker Charge**.

SC-091. Lo mismo se aplica a las variantes de brewing que actualmente derivan la potion normal y long desde Clinging con Shulker Shell; las rutas posteriores de redstone/splash/lingering se conservan.

SC-092. El objetivo de diseño es desacoplar Reorientation del coste de shulker boxes y ligar su fantasía a la captura del propio proyectil gravitatorio/levitador del shulker.

## 12. Multiplayer, ownership y seguridad

SC-100. Daño, target, navegación, drops, consumo y duplicación de shulkers son server-authoritative.

SC-101. Debe conservarse ownership/attribution suficiente para daño, PvP, advancement/stat hooks pertinentes y para evitar drops duplicados bajo impactos simultáneos.

SC-102. Dos eventos de intercepción concurrentes no pueden producir dos items a partir de un único proyectil.

SC-103. Los dispensers deben usar exactamente las mismas reglas funcionales de target/readquisición/intercepción que un lanzamiento manual salvo la fuente de la dirección de intención y attribution que corresponda.

## 13. Testing mínimo antes de integrar

- captura melee de bullet natural;
- captura con flecha de jugador;
- captura con flecha de dispenser;
- escudo sin drop;
- colisión/expiración sin drop;
- stack 64 y consumo exacto;
- lanzamiento manual + cooldown;
- lanzamiento de dispenser;
- adquisición de entidad por proximidad a ray/cone, no mera distancia al jugador;
- prioridad de Target Block apuntado directamente;
- Target Block produce señal por impacto real;
- no-target launch;
- readquisición tras muerte del target;
- readquisición tras retirar Target Block;
- no pérdida de lock sólo por perder línea de visión;
- navegación ortogonal y rodeo de obstáculos;
- impacto ofensivo/Levitation;
- shulker duplication conservada;
- recaptura melee/flecha de Charge lanzada;
- recaptura por otro jugador sin duplicación;
- escudo contra Charge lanzada sin devolución;
- receta Reorientation con Charge y rechazo de Shulker Shell;
- GameTests de autoridad/duplicación concurrente y client tests visuales cuando proceda.

## 14. Arte pendiente

El objeto en mano usará representación 3D. Antes de implementar el inventario/GUI se debe elegir entre:

1. crear un icono 2D propio derivado del diseño de la Charge;
2. usar un recurso existente sólo si su licencia permite inequívocamente redistribución/modificación en este proyecto.

No se incorporará una textura simplemente por aparecer en una wiki, resource pack o imagen pública sin comprobar copyright/licencia y atribución requerida.

## 15. Fuera de scope de esta especificación

- cambios generales al sistema Gravity Fall/cámara de alpha.13;
- nuevas recetas de Clinging;
- modificación de Wind Charge o Fire Charge vanilla;
- convertir Shulker Charge en arma de steering continuo;
- añadir HUD de lock-on salvo que futuros tests demuestren que la adquisición no es legible sin feedback adicional.
