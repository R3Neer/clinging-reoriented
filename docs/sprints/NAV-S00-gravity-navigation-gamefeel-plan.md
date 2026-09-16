# NAV-S00 — campaña TM: física, landing, agua y navegación gravitatoria

Estado: **ACTIVO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

Base: `main` tras el merge de compatibilidad Alchemical Leather. Esta campaña integra los diseños previos de `design/pet-follow-redesign` y los convierte en implementación incremental con gates verdes por sprint.

## Orden de implementación

### NAV-S00 — baseline, contratos y diseño consolidado
- Importar a la rama TM los documentos de aerodinámica, agua, landing y navegación gravitatoria.
- Fijar invariantes funcionales y de rendimiento.
- Confirmar baseline CI antes de cambios de producción.

### NAV-S01 — núcleo compartido de predicción física
Objetivo: crear una base común para predecir trayectorias volumétricas bajo gravedad arbitraria sin todavía cambiar gameplay.

Debe poder responder: evolución de velocidad/posición, primer contacto, soporte válido, ETA, daño estimado y estado terminal. Landing y navegación de mobs deberán consumir este mismo concepto para no mantener dos físicas incompatibles.

### NAV-S02 — aerodinámica corporal de Gravity Fall
Depende de S01 porque el predictor debe conocer la misma física que mueve realmente al jugador.

Cambiar la causalidad a `mirada -> postura corporal -> aerodinámica -> trayectoria`. Eliminar el papel especial de W en la redirección. Aplicar resistencia anisótropa longitudinal/transversal sin crear energía y conservar estabilidad cabeza/pies.

### NAV-S03 — free-fall -> landing anticipado
Depende de S01+S02.

Separar horizonte de adquisición de la ventana visual de 10 ticks. Mantener candidato con confianza/histéresis, usar el modelo físico compartido y lograr que la rotación termine en touchdown en vez de comenzar al impactar.

### NAV-S04 — agua: controles y cámara
Independiente del planner de mobs, pero se ejecuta aquí para cerrar el modelo de frames del jugador antes de reutilizar predictores/soportes en IA.

WASD sigue cámara; Space/Shift siguen vertical mundial. Con soporte submarino la cámara adopta el suelo efectivo; en nado libre vuelve suavemente a world-up sin reescribir la gravedad lógica.

### NAV-S05 — planner gravitatorio general de mobs
Depende de S01 porque las aristas gravitatorias necesitan simulación segura/costada.

El planner no conoce “mascotas”: recibe un objetivo de alto nivel y elige entre navegación normal y transiciones soporte->soporte. Daño es coste no lineal, imposibilidad geométrica es veto. Búsqueda local, perezosa y presupuestada.

### NAV-S06 — integración de objetivos de IA
- Follow owner para mascotas sin breadcrumbs.
- Chase/attack para hostiles.
- Flee para objetivos de huida, pudiendo elegir pared/techo si mejora la ventaja de escape.
- Mantener goals vanilla como propietarios de la intención; el planner sólo amplía locomoción.

### NAV-S07 — mundo dinámico, tracking móvil y reacción
Depende del planner funcional.

Target observado continuamente con filtrado e histéresis. Tiempo de reacción derivado de velocidad base de movimiento mediante curva suave, saturante y acotada. Obstáculos tardíos pueden causar impacto; cambios con margen permiten replan legal. Clinging/Reorientation nunca obtienen capacidades extra por reaccionar antes.

### NAV-S08 — eficiencia, adversarial y retirada del legado
- Presupuestos por tick y planificación distribuida.
- Reutilización/invalidation local de conocimiento geométrico.
- Casos adversariales de muchos mobs.
- Eliminar breadcrumbs y contratos obsoletos sólo cuando el reemplazo general esté demostrado.

### NAV-S09 — convergencia final
- Matriz completa de JUnit/GameTests/client snapshots/compat.
- Docs ARCHITECTURE/GUIDE/VALIDATION/CHANGELOG.
- Revisión adversarial final y preparación de prerelease sólo después de que toda la campaña esté verde.

## Gates TM
Cada sprint debe: (1) declarar invariantes antes de tocar producción, (2) añadir/actualizar tests que fallen con el comportamiento viejo cuando corresponda, (3) implementar el mínimo cambio coherente, (4) ejecutar gates focalizados, (5) ejecutar matriz amplia antes de cerrar dependencias públicas, y (6) documentar cualquier intento rojo útil en vez de rebajar tests.

## Invariantes globales
- No inventar capacidades gravitatorias: Clinging y Reorientation conservan exactamente sus reglas.
- Ningún predictor puede atravesar geometría desconocida como si fuese aire.
- El volumen corporal real manda, no raycasts puntuales.
- El mismo modelo conceptual de movimiento debe alimentar física real, landing y simulación de transiciones.
- Daño/riesgo es coste fuerte; geometría físicamente inválida es veto.
- El planner gravitatorio no sustituye pathfinding vanilla cuando éste ya resuelve el objetivo.
- El trabajo por tick debe estar acotado; muchos mobs no pueden provocar búsquedas globales simultáneas.
- Las transiciones visuales deben preservar continuidad de cámara/cuerpo y ownership.
