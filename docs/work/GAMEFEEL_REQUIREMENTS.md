# Clinging: Reoriented — requisitos temporales de gamefeel, cámara e impactos

Estado: **fuente normativa temporal** de la rama `chatgpt-gamefeel-camera`. Se retirará cuando el trabajo quede integrado en la documentación canónica. La implementación no puede redefinir estos requisitos por conveniencia técnica.

## 1. Objetivo y lenguaje de diseño

La actualización debe preservar la identidad de Clinging y dar a Reorientation una identidad aérea propia:

- **Clinging:** una única decisión aérea sobre hacia dónde caer antes de encontrar un nuevo suelo.
- **Reorientation:** decisiones discretas ilimitadas sobre la aceleración gravitatoria, conservando momentum.
- **Elytra:** vuelo aerodinámico continuo gobernado por orientación y mirada.

Regla rectora: **Elytra controla continuamente el movimiento; Clinging/Reorientation controlan discretamente la aceleración.** No se añadirá steering hacia cámara, sustentación, impulso, magnetismo a superficies, frenado automático ni rotación artificial del momentum.

## 2. Física y momentum

FR-GF-001. Un cambio voluntario de gravedad DEBE conservar exactamente el vector de velocidad mundial existente. Sólo cambia la aceleración futura.

FR-GF-002. Una inversión de gravedad debe frenar físicamente el movimiento anterior, cruzar velocidad cero y sólo después invertir el desplazamiento. No puede existir impulso instantáneo de inversión.

FR-GF-003. La física sigue siendo server-authoritative. La presentación cliente no puede modificar posición, gravedad, velocidad, colisiones ni daño.

## 3. Cámara: el giro pertenece al aterrizaje, no al cambio de gravedad

FR-GF-010. Aceptar un cambio de gravedad NO DEBE iniciar un giro de cámara. Esto se aplica también al Clinging ordinario y a giros de 90°/180°.

FR-GF-011. Al quedar airborne se conserva como frame visual estable el quaternion que realmente se estaba mostrando. Cambios físicos posteriores de gravedad no alteran ese frame durante vuelo libre.

FR-GF-012. La cámara pertenece al jugador: no sigue automáticamente velocidad, cuerpo, aceleración ni gravedad.

FR-GF-013. `selectionLook` debe seguir representando la mirada realmente renderizada del jugador y podrá usarse para seleccionar una nueva gravedad durante vuelo libre.

## 4. Landing acquisition y landing commitment

FR-GF-020. Una superficie sólo puede convertirse en futuro suelo si puede proporcionar soporte real bajo los pies de la entidad en la gravedad física actual y la trayectoria prevista produce contacto real.

FR-GF-021. La predicción debe considerar posición, AABB/dimensiones reales, velocidad mundial, gravedad actual, intensidad y collision geometry. Debe reutilizar física/transformaciones existentes antes de crear una aproximación paralela.

FR-GF-022. El landing snap debe comenzar cuando el ETA previsto al contacto sea aproximadamente la duración nominal del giro, para que la transición concluya alrededor del touchdown.

FR-GF-023. Se conservan inicialmente los timings actuales como lenguaje visual: 90° = 180 ms; 180° = 240 ms.

FR-GF-024. Si el cambio se produce tan tarde que no queda tiempo nominal, la cámara no debe acelerar de forma extrema. Empieza inmediatamente y puede terminar ligeramente después del contacto. La física jamás se modifica para acomodar la cámara.

FR-GF-025. Una vez iniciado el landing snap, el aterrizaje queda `LANDING_COMMITTED`: no se aceptan nuevos cambios voluntarios de Clinging/Reorientation hasta recuperar soporte real.

FR-GF-026. Los inputs gravitatorios recibidos durante `LANDING_COMMITTED` se descartan; no se encolan ni se ejecutan tras tocar suelo. Tampoco deben robar el input vanilla de salto.

FR-GF-027. El commitment sólo se cancela si el aterrizaje deja físicamente de ser posible por una causa externa real (bloque movido/destruido, knockback suficiente, teleport, dimensión, muerte, Elytra, vehículo/lifecycle equivalente). Al cancelarse se conserva el quaternion actualmente mostrado, sin snap-back.

## 5. Superficies extensibles: contrato público y ownership

FR-GF-030. Clinging debe exponer un contrato público y pequeño para que otros mods definan o aporten **superficies válidas de soporte/landing** y predigan contacto sin que Clinging conozca sus clases internas.

FR-GF-031. El contrato NO puede depender de Scale Brews ni de `SurfaceContact`, `AnatomyApi` u otros tipos externos. La integración concreta con Scale Brews queda fuera de esta actualización y deberá vivir del lado consumidor/adaptador correspondiente.

FR-GF-032. Vanilla debe existir como provider base. Un provider externo debe poder contestar al menos: disponibilidad, contacto/soporte válido para una gravedad cardinal, tiempo/fracción prevista de contacto y, cuando proceda, identidad estable suficiente para revalidar el compromiso.

FR-GF-033. Los providers deben fallar cerrados: ausencia, excepción controlable, datos no finitos, contacto huérfano o superficie invalidada no pueden transformarse en suelo válido.

FR-GF-034. Clinging no debe realizar scans globales para descubrir superficies externas. El contrato debe permitir consulta acotada desde el contexto de la entidad/trayectoria.

FR-GF-035. El provider base vanilla y los providers externos deben compartir una semántica observable común; ningún consumer puede saltarse el preflight físico de Clinging.

## 6. Estados conceptuales de presentación

La implementación debe equivaler a cuatro estados aunque no use un enum explícito:

1. `GROUNDED`: soporte real.
2. `AIRBORNE`: caída/jump normal; cámara ya desacoplada de nuevos cambios de gravedad.
3. `SUSTAINED_GRAVITY_FALL`: caída prolongada; el cuerpo empieza a seguir la velocidad.
4. `LANDING_COMMITTED`: snap hacia el futuro suelo y bloqueo de nuevos cambios.

FR-GF-040. El contador airborne empieza al perder soporte y no se reinicia por cambiar gravedad.

FR-GF-041. Valor inicial de prototipo: entrada en sustained gravity fall a **12 ticks airborne** si no existe landing committed ni estado incompatible.

FR-GF-042. Valor inicial de prototipo: **6 ticks de blend** hacia la orientación completa de gravity fall.

FR-GF-043. 12/6 son valores de tuning de playtesting. No se sustituyen antes de probarlos.

## 7. Animación corporal y Fresh Animations

FR-GF-050. Esta actualización NO debe introducir una animación keyframed nueva de brazos/piernas ni requerir un pipeline propio de Blockbench.

FR-GF-051. Debe reutilizar el estado natural de falling/jumping/landing del renderer activo. No se falsificarán `SWIMMING`, `FALL_FLYING`, crawling u otros estados vanilla sólo para obtener una pose.

FR-GF-052. Con Fresh Animations: Player Extension instalado, FA debe conservar ownership de brazos, piernas, head tracking, equipos y microanimación de caída/landing. Clinging sólo añade la transformación corporal macroscópica necesaria.

FR-GF-053. Fresh Animations/EMF/ETF son compatibilidad prioritaria del entorno objetivo, pero NO dependencias obligatorias del mod.

FR-GF-054. Tras el blend, el eje cabeza-pies del cuerpo se orienta suavemente según el vector de velocidad mundial. No según gravedad, cámara ni dirección recién elegida.

FR-GF-055. Una nueva gravedad de 90°/180° no rota instantáneamente el cuerpo; sólo lo hace cuando la velocidad real se curva/invierte.

FR-GF-056. Cerca de velocidad cero se conserva el último frame corporal fiable. No se normaliza un vector degenerado ni se introducen flips por ruido numérico.

FR-GF-057. El roll/twist alrededor del vector de velocidad debe mantener continuidad temporal y aplicar la mínima rotación necesaria; no puede recalcularse desde un up-vector global que provoque barrel rolls accidentales.

FR-GF-058. La cabeza continúa expresando la mirada mediante el renderer/FA dentro de sus límites normales; la transformación global del cuerpo no secuestra yaw/pitch del jugador.

FR-GF-059. Al comenzar `LANDING_COMMITTED`, el cuerpo deja de perseguir la velocidad y converge al frame corporal del futuro suelo durante la misma ventana del landing snap.

## 8. Controles

FR-GF-060. La cámara desacoplada no autoriza nuevo air steering. Se conservan las magnitudes físicas existentes.

FR-GF-061. W/A/S/D deben continuar siendo intuitivos respecto al frame visual que el jugador ve. No se puede usar ciegamente la gravedad lógica como frame de input si eso contradice la cámara estable.

FR-GF-062. Elytra conserva prioridad absoluta. Iniciar `fall_flying` cancela la presentación Gravity Fall/landing incompatible y devuelve ownership a Elytra.

FR-GF-063. Agua conserva el arbitraje existente: no se entra en sustained gravity fall mientras se nada; no se falsifica Falling para competir con la animación acuática.

## 9. Nuevo modelo de impacto

FR-GF-070. Mientras Clinging/Reorientation poseen la física gravitatoria, `fallDistance` deja de ser la magnitud física primaria para el daño de caída.

FR-GF-071. El observable primario debe ser la velocidad normal realmente absorbida por una colisión. Debe derivarse del movimiento/velocidad precolisión y de las componentes que la geometría impide, no del eje gravitatorio actual.

FR-GF-072. Cambiar gravedad justo antes de impactar no puede borrar daño si la entidad sigue golpeando una superficie a alta velocidad.

FR-GF-073. Frenar físicamente mediante gravedad opuesta sí debe reducir/eliminar daño cuando la velocidad real de impacto haya bajado.

FR-GF-074. Colisiones tangenciales deben producir poco o ningún daño; una colisión multieje en un único movimiento debe resolverse como un único evento de impacto, no daño duplicado por eje.

FR-GF-075. La curva final no se inventará en paralelo. Debe convertir la velocidad de impacto a una **caída vanilla equivalente** y delegar en el pipeline vanilla tanto como sea posible, preservando Feather Falling, hay bales, slime, agua, inmunidades y callbacks pertinentes.

FR-GF-076. Una gravedad más fuerte no multiplica el daño una segunda vez: su efecto ya está incorporado en la velocidad alcanzada.

FR-GF-077. Elytra real conserva su kinetic damage/pipeline y no debe recibir daño duplicado por este sistema.

FR-GF-078. La entrada/salida de ownership no puede convertirse en exploit que borre o duplique una caída peligrosa.

FR-GF-079. Los resets actuales de `fallDistance` por giro sólo se eliminan después de demostrar por tests que la nueva semántica cubre giros, frenado, impactos tardíos, expiry, mounts/mobs relevantes y lifecycle.

## 10. Mobs, monturas y alcance visual

FR-GF-080. Esta actualización no crea Gravity Fall visual genérico para mobs. Sí debe preservar física, navegación y semántica de impactos de entidades cuya gravedad pertenezca a Clinging.

FR-GF-081. Mounts/pets no pueden sufrir regresiones en ownership, snap actual, jerarquía de pasajeros o heading transport.

FR-GF-082. Los jugadores remotos deben mostrar la misma semántica corporal de Gravity Fall siempre que pueda derivarse determinísticamente de estado ya sincronizado. No se añadirán paquetes por tick sólo para animación.

FR-GF-083. First Person 2.7.2 es compatibilidad prioritaria: el cuerpo puede orientarse por velocidad pero esa transformación no puede realimentar la cámara.

## 11. Restricciones de alcance

Fuera de esta actualización:

- Gravitator;
- Shulker Charge;
- cambios de receta de Reorientation;
- estaciones/campos gravitatorios;
- cambios a Calibrated Lodestones;
- eliminación de Clinging de beacons;
- nuevas partículas, trails, HUD, FOV o sonidos específicos de gravity flight;
- integración concreta con Scale Brews.

## 12. Requisitos de testing y snapshots

NFR-GF-001. Cada sprint debe seguir TM iterativo y el protocolo de `GAMEFEEL_SPRINT_WORKFLOW.md`.

NFR-GF-002. Los tests deben incluir unit/kernel, GameTest servidor, client GameTest y lanes de compatibilidad cuando el requisito lo necesite.

NFR-GF-003. La fase visual debe usar snapshots intensivamente. Los client GameTests deben capturar imágenes en checkpoints semánticos, no una única captura final.

NFR-GF-004. Como mínimo deben existir snapshots de: airborne pre-Gravity-Fall, blend inicial, sustained fall a velocidad cardinal, trayectoria curva tras cambio 90°, frenado/inversión 180°, comienzo/mitad/final de landing snap, touchdown, First Person, Fresh Animations target y comparación Elytra/Reorientation.

NFR-GF-005. Los snapshots deben acompañarse de asserts numéricos de quaternion/orientación/estado cuando sea posible. Una imagen bonita no sustituye a una invariante física.

NFR-GF-006. Se permiten harnesses/programas temporales de análisis de imágenes, comparación geométrica o generación de fixtures. Deben vivir fuera de producción o retirarse al cerrar el trabajo si no aportan valor permanente.

NFR-GF-007. La CI debe conservar artefactos de screenshots, logs, XML de tests y reportes para la revisión adversarial.

NFR-GF-008. La aceptación manual debe comprobar específicamente mareo/legibilidad, diferencia visual con Elytra y que Clinging corto siga sintiéndose preciso.

## 13. Criterio final

La actualización sólo se acepta si ambas frases son ciertas:

- Clinging: «He elegido otro sitio hacia el que caer; el mundo se coloca justo cuando voy a convertirlo en suelo.»
- Reorientation: «Conservo momentum, elijo discretamente hacia dónde acelero, mi cuerpo cuenta mi trayectoria y mi cámara sigue siendo mía.»

Prioridad: **control → legibilidad → comodidad visual → continuidad física → espectáculo**.
