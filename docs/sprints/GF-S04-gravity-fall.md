# GF-S04 — Sustained Gravity Fall, body frame y controles

Estado: **CERRADO / GATE VERDE**.

## Tesis

Al cerrar S04, una caída prolongada controlada por Clinging/Reorientation adquiere una presentación corporal propia sin convertirse en Elytra: tras 12 ticks airborne el cuerpo mezcla durante 6 ticks hacia una orientación cuyo eje cabeza-pies sigue la velocidad mundial mediante transporte mínimo continuo; la cámara sigue siendo independiente. Al aproximarse a soporte, el cuerpo converge al frame del futuro suelo aunque no haga falta girar cámara. W/A/S/D siguen siendo intuitivos respecto al frame visual sin añadir steering ni modificar la magnitud física existente.

## Scope

FR-GF-040..063 y FR-GF-082..083. No añade animaciones keyframed propias, partículas, FOV, sonidos de vuelo ni mecánicas nuevas.

## Estado real investigado

- Minecraft 26.2 llama a `EntityRenderDispatcher.submit`, traduce al origen de la entidad y después invoca el renderer.
- Gravity Changer inyecta inmediatamente después de esa traslación y multiplica el `PoseStack` por su quaternion visual de gravedad.
- Por tanto Clinging puede añadir una transformación raíz inmediatamente antes de `EntityRenderer.submit`: ésta afecta sólo al modelo/render del avatar, no a la cámara.
- `AvatarRenderState.id` permite resolver al jugador real sin añadir identidad paralela.
- Fresh Animations/EMF continúan animando las partes internas del avatar dentro de ese root ya transformado.
- Gravity Changer transforma `moveRelative` usando **la gravedad física**. Con una cámara retenida en otro frame, W podría dejar de coincidir con el frente visual; esto debe corregirse sin aumentar air control.

## Estados corporales conceptuales

1. `NORMAL`: animación/render normal del renderer activo.
2. `GRAVITY_FALL_BLEND`: entrada progresiva 12→18 ticks.
3. `GRAVITY_FALL`: root orientado por velocidad.
4. `BODY_LANDING`: root converge al frame canónico del futuro suelo.

`BODY_LANDING` es deliberadamente independiente de `LANDING_COMMITTED`: si cámara y suelo ya coinciden no hace falta bloquear input ni girar cámara, pero un cuerpo que viene cabeza primero sí debe prepararse para posar los pies.

## Activación

- El contador airborne es el ya server-authoritative de `LandingState` y no se reinicia por cambios de gravedad.
- La presentación especial sólo empieza si Clinging/Reorientation **poseen actualmente la física**. Llevar el efecto y simplemente caerse no activa Gravity Fall.
- A tick 12 se publica `START` si sigue airborne, no hay estado incompatible y no hay soporte/landing corporal inmediato.
- Blend inicial: 6 ticks.
- Si el jugador activa Clinging después de llevar >12 ticks airborne, `START` puede producirse inmediatamente: el criterio es tiempo real airborne, no tiempo desde el giro.

## Sincronización

No se envían quaternions por tick.

El servidor publica un evento discreto `gravity_fall_visual_v1` a self + trackers con:

- entity id + UUID;
- fase (`START`, `LAND`, `RESUME`, `RESET`);
- target gravity para `LAND`;
- secuencia monotónica por jugador.

La orientación continua se deriva en cada cliente de la velocidad ya sincronizada de la entidad. Al comenzar tracking, si el jugador está en una fase activa se envía un snapshot de esa fase.

## Orientación por velocidad

- El eje corporal canónico cabeza→pies se trata como eje vertical local; en caída sostenida el extremo de la cabeza apunta aproximadamente en el sentido de `velocity`.
- No se recalcula una orientación absoluta desde un up-vector global cada frame.
- Se conserva `lastDirection` y `bodyQuaternion`; entre direcciones válidas se aplica la rotación mínima que lleva la dirección anterior a la nueva. Éste es transporte paralelo discreto y conserva twist/roll de manera continua.
- Velocidad por debajo de un epsilon físico conserva la última orientación estable.
- Un giro físico de gravedad no modifica directamente `bodyQuaternion`; sólo la evolución posterior de `velocity` lo hace.

## Entrada/blend

Al recibir `START`:

- se captura la orientación corporal realmente mostrada en ese frame;
- se memoriza la primera dirección de velocidad fiable;
- durante 6 ticks/render-time equivalente se interpola desde ese frame hacia la orientación transportada por velocidad;
- después se sigue el transporte mínimo continuamente.

No se cambia `Pose`, `FALL_FLYING`, `SWIMMING` ni estados que Fresh Animations utilice como semántica propia.

## Body landing

El servidor usa la misma predicción de soporte de S02 aunque el visual frame de cámara ya coincida con la gravedad.

Al entrar en la ventana máxima de 5 ticks hacia soporte válido:

- publica `LAND(targetGravity)`;
- el cliente captura el `bodyQuaternion` actual;
- converge mediante SLERP al quaternion corporal canónico de ese target en el tiempo previsto hasta touchdown, acotado a la misma escala 180/240 ms;
- si la predicción se invalida sin camera commitment, publica `RESUME`: el cliente parte del quaternion corporal actual y vuelve a transporte por velocidad, sin snap-back;
- touchdown publica/deriva `RESET` y elimina el root extra.

`BODY_LANDING` por sí solo NO bloquea nuevos giros. Sólo `LANDING_COMMITTED` de cámara lo hace.

## Root transform de render

Después de que Gravity Changer haya aplicado `Q_visual` al `PoseStack`, Clinging quiere que el modelo completo termine en `Q_body`.

Se aplica un quaternion extra calculado con la convención de JOML validada por test, conceptualmente:

`Q_extra = inverse(Q_visual) · Q_body`

(o el orden equivalente que demuestre el test de composición del PoseStack).

El mixin sólo actúa para jugadores en una fase Gravity Fall activa. Sin FA funciona con el modelo vanilla; con FA, las animaciones internas permanecen intactas.

La revisión visual descubrió además que aplicar ese root alrededor del origen del renderer hacía orbitar el avatar alrededor de los pies. El root final se aplica alrededor del centro lógico del avatar (`boundingBoxHeight / 2`), conservando el mismo centro visual en DOWN/EAST/WEST y durante BODY_LANDING.

## Controles W/A/S/D

Regla de diseño: **no se añade air steering; sólo se corrige el frame direccional de un control que ya existe**.

Mientras el frame visual esté retenido respecto a la gravedad física:

- W debe corresponder al frente visual proyectado sobre el plano físico de movimiento perpendicular a la gravedad;
- A/D usan la base lateral visual coherente;
- la magnitud y normalización del input siguen siendo exactamente las de Minecraft/Gravity Changer;
- si el frente visual es casi paralelo a la gravedad y su proyección degenera, se usa una base lateral proyectada/último heading estable, nunca ruido numérico.

La corrección se aplica sólo al jugador local porque el servidor recibe el movimiento resultante/predicho, no una segunda interpretación autoritativa de WASD. Debe verificarse que no se crea divergencia de predicción.

## Fresh Animations / First Person

- Fresh Animations Player Extension conserva Falling/Jumping/Landing y animaciones de miembros. No se copian assets ni se modifica su state machine.
- El root extra se aplica por fuera del modelo animado.
- First Person puede mostrar el cuerpo transformado, pero la cámara no consume el root de Gravity Fall.
- Elytra real cancela/impide Gravity Fall y conserva su lenguaje propio.
- First Person 2.7.2 + Not Enough Animations queda cubierto por lane real de CI. El fixture Fresh Animations/EMF/ETF queda deliberadamente para la campaña S05, donde se identificará la versión exacta compatible del entorno objetivo antes de añadir una lane.

## Plan

- [x] I1 kernel puro de transporte mínimo de orientación y zero-speed hold + tests quaternion/vector.
- [x] I2 estado server/player y payload discreto `GravityFallVisual` con START/LAND/RESUME/RESET + tracking snapshot.
- [x] I3 detección START a 12 ticks y BODY_LANDING usando `LandingPrediction`, separada de camera commitment.
- [x] I4 estado cliente derivado por UUID y actualización desde velocity sincronizada.
- [x] I5 mixin root en `EntityRenderDispatcher.submit`, después del visual gravity transform y antes del avatar renderer.
- [x] I6 blend 6 ticks, landing body blend, cancel/resume, touchdown/reset.
- [x] I7 corregir frame de `moveRelative` del LocalPlayer sin cambiar magnitud de control.
- [x] I8 client tests + snapshots de pre-12, blend, sustained, 90° curve, 180° zero crossing/reverse, body landing y touchdown.
- [x] I9 lane First Person; fixture Fresh Animations/EMF explicitado como trabajo de S05.
- [x] I10 holdout adversarial y revisión cero-cambios.

## Modelo adversarial previo

- velocidad cero exactamente al invertir gravedad;
- velocity cambia casi 180° entre dos packets/interpolaciones;
- jitter muy pequeño alrededor de cero;
- body START justo cuando landing prediction aparece;
- camera canonical pero body horizontal: debe hacer BODY_LANDING sin input lock;
- camera landing invalidada mientras body landing también activo;
- remote entity sale/entra del tracking en pleno Gravity Fall;
- entity id reutilizado con UUID distinto;
- First Person no puede recibir roll de root como roll de cámara;
- EMF/FA no puede sobrescribir el root ni ser sobrescrito en sus limbs;
- visual gravity quaternion held no puede contarse dos veces en `Q_extra`;
- W mirando exactamente paralelo a gravedad: basis degenerada;
- cambiar gravity 90° no puede provocar salto instantáneo de body antes de cambiar velocity;
- Elytra activada durante Gravity Fall debe resetear root inmediatamente.

### Holdout reservado

`EAST velocity → gravedad WEST → frenado hasta |v|≈0 → inversión WEST`: el cuerpo debe conservar twist/orientación estable en el cruce por cero y sólo iniciar el giro cuando vuelva a existir una dirección WEST fiable. La cámara permanece exactamente en su frame retenido durante toda la maniobra.

## Evidencia de cierre

Commit de cierre lógico/visual: `e2b394710e50fa58253cbd69d55c927996832936`.

GitHub Actions run 427 pasó todas las lanes: 73/73 server GameTests, client GameTests, First Person 2.7.2 + Not Enough Animations, Scale Brews optional server y Scale Brews optional client load.

La campaña cliente produjo checkpoints estables de pre-start, blend start/mid, sustained DOWN, curva EAST, zero hold, reverse WEST, BODY_LANDING begin/mid/final, RESUME y touchdown/reset. Los asserts verifican quaternion/body axis y forward de cámara junto a las imágenes.

Revisión visual manual de los artefactos del run 427:

- `gravity-fall-sustained-down`: cabeza hacia la velocidad DOWN, centro corporal estable;
- `gravity-fall-curve-east`: giro corporal 90° sin desplazamiento orbital;
- `gravity-fall-reverse-west`: inversión 180° coherente tras zero hold;
- `gravity-fall-landing-begin`: conserva el frame WEST visible al entrar;
- `gravity-fall-landing-mid`: pose intermedia real, no endpoint anticipado;
- `gravity-fall-landing-final`: frame canónico DOWN y cámara sin feedback del root.

El holdout EAST→zero→WEST pasó numéricamente y visualmente. La última corrección afectó sólo al harness para separar el tiempo de presentación de los ticks consumidos por screenshots; la ejecución posterior quedó completamente verde y la revisión de sus snapshots no exigió cambios de producción.
