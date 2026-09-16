# Gamefeel pendiente — aerodinámica y agua

Estado: **APARCADO deliberadamente mientras se rediseña el seguimiento gravitatorio de mascotas**.

Este documento conserva decisiones de diseño ya aceptadas para volver a ellas después del trabajo de mascotas. No es todavía una especificación de implementación.

## A — Aerodinámica de Gravity Fall

### Problema actual

La resistencia aerodinámica por postura ya existe de forma continua, pero la redirección del momentum hacia la mirada está ligada a mantener W. Eso hace que una parte de la física parezca una habilidad activada por input en vez de una consecuencia de la postura corporal.

### Dirección de diseño aceptada

La causalidad deseada es:

**mirada → actitud/postura del cuerpo → interacción aerodinámica → trayectoria**

No:

**W → redirección de trayectoria**.

La mirada debe expresar intención de orientación. El cuerpo debe seguirla gradualmente con la libertad de cuello/macro-body correspondiente. La velocidad debe responder a la postura corporal aunque no se pulse ninguna tecla.

### Modelo conceptual preferido

No convertir simplemente el redirect actual en una fuerza permanente. La respuesta debería parecer aerodinámica y no un imán angular.

- Descomponer conceptualmente la velocidad en componente longitudinal al eje del cuerpo y componente transversal.
- Penalizar más la componente transversal que la longitudinal.
- El resultado natural debe ser que la trayectoria tienda suavemente a alinearse con el eje longitudinal del cuerpo mientras se pierde energía por drag.
- No añadir energía ni velocidad gratuita.
- Mantener simetría cabeza/pies: caer alineado en cualquiera de los dos sentidos longitudinales puede ser aerodinámico; nunca forzar espontáneamente un giro de 180°.
- Evitar un bucle en el que la velocidad controle totalmente el cuerpo mientras el cuerpo controla totalmente la velocidad. La velocidad puede estabilizar/informar la postura, pero la actitud corporal debe convertirse en una variable física con persistencia propia.

### Gamefeel buscado

- Soltar W no debe apagar la física.
- Una postura visible debe explicar una trayectoria visible.
- El jugador debe poder aprender el sistema mirando el cuerpo, no recordando una excepción de teclado.
- Los cambios deben ser suaves, previsibles y conservar momentum.

## C — Agua: locomoción guiada por cámara

### Problema actual

En agua, Space/Shift ya se fuerzan a mundo +Y/-Y cuando Clinging/Reorientation posee la física, pero el marco completo de locomoción todavía puede no coincidir con la cámara/cuerpo visible cuando la gravedad almacenada es lateral o invertida.

### Dirección de diseño aceptada

Mientras el jugador esté nadando/libre en fluido, la gravedad almacenada **no debe poseer el marco de locomoción**.

Regla deseada:

- **W/S:** avanzar/retroceder según la dirección de mirada, incluyendo pitch.
- **A/D:** izquierda/derecha de pantalla/cámara.
- **Space:** mundo +Y, conservando el significado Minecraft de subir hacia la superficie.
- **Shift:** mundo -Y.
- La velocidad existente, corrientes, knockback e inercia siguen existiendo; el input expresa aceleración/intención, no teleport ni spectator movement.

En resumen: **WASD = cámara; Space/Shift = vertical mundial; gravedad = no define controles acuáticos**.

## C2 — Cámara acuática dependiente de soporte

### Nuevo fallo detectado en playtest

Con gravedad no-DOWN dentro del agua, la cámara puede permanecer ligada a una orientación gravitatoria aunque el jugador ya no esté apoyado en ese “suelo”. Eso produce una cámara inclinada durante nado libre aunque cuerpo y locomoción deberían volver a un marco acuático normal.

### Regla deseada

El agua debe distinguir dos estados de presentación:

1. **Submerged + supported:** si el jugador está realmente apoyado sobre una superficie que actúa como suelo bajo su situación actual, la cámara se alinea con ese suelo para que el contacto se lea como estar de pie/caminar sobre él.
2. **Submerged + unsupported/swimming:** en cuanto se pierde ese soporte, la cámara vuelve a una presentación world-up equivalente visualmente a gravedad DOWN, aunque la gravedad lógica almacenada siga siendo otra.

La vuelta a world-up es sólo de presentación/control acuático; no debe reescribir la gravedad lógica.

### Requisitos de gamefeel

- No oscilar entre ambos marcos por contacto intermitente de un tick en la frontera del agua o sobre geometría irregular.
- La transición debe sentirse continua, no como un snap accidental, salvo que el lenguaje visual final del mod justifique un snap deliberado.
- Al recuperar soporte bajo el agua, la cámara debe volver a comunicar claramente qué superficie es el suelo efectivo.
- Al abandonarlo, el jugador debe recuperar inmediatamente una referencia world-up legible para nadar.

## Orden de trabajo

Estas decisiones quedan aparcadas hasta terminar el rediseño del algoritmo de mascotas. Después deberán convertirse en una campaña específica de diseño/TM con tests de física, control y cámara.