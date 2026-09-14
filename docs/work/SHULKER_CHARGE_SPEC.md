# Shulker Charge — especificación temporal

Estado: **NORMATIVO TEMPORAL / S00–S03 IMPLEMENTADOS**.

Rama: `feature/shulker-charge`, basada en `main` / `v0.1.0-alpha.13` (`be3b47284a71fbf4fcacb2ab2b2cb3776af290d3`).

La implementación no redefine estas reglas por conveniencia técnica; cualquier cambio deliberado vuelve primero a esta especificación.

## 1. Objetivo

Introducir **Shulker Charge** como objeto capturable y relanzable que materializa una shulker bullet y sustituye a la Shulker Shell como ingrediente de Reorientation.

Identidad: Fire Charge = fuego; Wind Charge = impulso; **Shulker Charge = proyectil guiado / levitación capturado de un shulker**.

## 2. Objeto y arte

SC-001. `Shulker Charge` apila hasta **64**.

SC-002. El inventario/GUI usa el **icono 2D definitivo presente en la rama**. En mano se usa una representación **3D** basada en la geometría de la shulker bullet. Los modelos/iconos presentes en la rama se consideran definitivos salvo que un commit posterior del usuario los sustituya o corrija manualmente.

SC-003. Se consume al lanzar y vuelve a existir como item sólo si el proyectil es interceptado activamente según las reglas de captura.

## 3. Captura

SC-010. Bullet natural destruida por melee deja exactamente 1 Charge.
SC-011. Bullet natural destruida por flecha deja exactamente 1 Charge, incluida flecha de dispenser.
SC-012. Escudo neutraliza sin drop.
SC-013. Impacto normal, bloque, expiry o causa no clasificada como interceptación no dejan drop.
SC-014. La obtención puede ser renovable/automatizable en late game.

## 4. Uso manual y dispenser

SC-020. Clic derecho lanza y consume una unidad.
SC-021. Dispenser lanza la misma entidad usando su facing como intención.
SC-022. Cooldown inicial **0,5 s**.
SC-023. Sin target válido el proyectil sale igualmente hacia delante.

## 5. Navegación

SC-030. Conserva navegación ortogonal/cardinal de shulker bullet, tramos y giros de 90°, partículas/feedback compatibles y rodeo de obstáculos.
SC-031. No homing curvo continuo.
SC-032. Impacto, daño y Levitation compatibles con vanilla.

## 6. Targeting

SC-040. Jugador: intención = ojos/cámara/crosshair.
SC-041. Dispenser: intención = salida/facing.
SC-042. Candidatos: entidades válidas y Target Blocks.
SC-043. Target Block directamente bajo el rayo tiene prioridad absoluta.
SC-044. Asistido: prioridad por error angular/perpendicular; distancia secundaria. No nearest-entity simple.
SC-045. Prototipo: alcance ~32 bloques, cono ~15°.
SC-046. Adquisición inicial de entidad requiere LOS; perder LOS tras lock no lo cancela.
SC-047. No se abandona un target válido sólo porque aparezca otro mejor.

## 7. Readquisición

SC-050. Sin objetivo puede adquirir durante vuelo.
SC-051. Si objetivo desaparece/deja de ser válido puede adquirir otro.
SC-052. Ocultarse tras pared no invalida target vivo.
SC-053. Readquisición conserva la intención original y usa posición actual de la Charge.
SC-054. Scan periódico y acotado; prototipo ~4 ticks sin target válido.

## 8. Target Blocks

SC-060. Puede fijar/golpear `minecraft:target`.
SC-061. Señal por impacto real de proyectil/Target Block, nunca activación remota.
SC-062. Dispenser + Charge + Target Block es interacción intencionada.

## 9. Recaptura

SC-070. Charge lanzada golpeada melee vuelve a 1 item.
SC-071. Charge lanzada destruida por flecha vuelve a 1 item.
SC-072. Aplica a proyectil propio o ajeno; es transferencia, no duplicación.
SC-073. Escudo neutraliza sin item.
SC-074. Impacto normal/expiry consume la Charge.
SC-075. No se sustituye por redirección estilo Wind Charge.

## 10. Shulkers

SC-080. Una Charge lanzada conserva deliberadamente la duplicación vanilla de shulkers.
SC-081. No hay excepción por origen jugador/dispenser.
SC-082. Renovabilidad/automatización resultante es gameplay emergente buscado.

## 11. Reorientation

SC-090. Reorientation deja de usar `Items.SHULKER_SHELL` y consume Shulker Charge.
SC-091. Se mantienen rutas posteriores de potion/splash/lingering.
SC-092. Se desacopla Reorientation del coste de shulker boxes y se liga a capturar el proyectil.

## 12. Multiplayer y autoridad

SC-100. Targeting, navegación, drops, consumo, daño y duplicación son server-authoritative.
SC-101. Ownership/attribution suficiente para daño/PvP/hooks y evitar drops duplicados.
SC-102. Dos intercepciones concurrentes no producen dos items.
SC-103. Dispenser usa las mismas reglas salvo fuente de intención/attribution.

## 13. Testing mínimo

Captura melee/flecha/dispenser-arrow; shield/colisión/expiry sin drop; stack 64; consumo y cooldown; player/dispenser launch; targeting angular; prioridad Target Block; redstone por impacto; no-target; readquisición; lock a través de pérdida de LOS; navegación cardinal; daño/Levitation; shulker duplication; recaptura own/other; rechazo Shell en brewing; concurrencia; client tests/snapshots de modelos y vuelo.

## 14. Fuera de scope

Cambios generales a Gravity Fall/cámara; nuevas recetas de Clinging; modificar Wind/Fire Charge; steering continuo; HUD de lock salvo evidencia futura que lo justifique.
