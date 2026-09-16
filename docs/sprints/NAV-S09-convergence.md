# NAV-S09 — convergencia final

Estado: **EN PROGRESO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Objetivo

S09 no añade una nueva mecánica. Su trabajo es demostrar que la campaña NAV-S01…S08 converge en un único producto coherente: código, tests, documentación y compatibilidad deben describir la misma semántica.

## Gates de código

- matriz completa: build/JUnit, server GameTests, client GameTests, First Person, Scale Brews server/client, Fresh Animations y snapshots;
- ningún breadcrumb de mascota en runtime;
- ningún goal vanilla reachable es secuestrado por el planner gravitatorio;
- owner airborne puede seguir siendo objetivo de mascota mediante anchor filtrado/proyección tangencial;
- chase/flee usan la misma locomoción genérica, sin listas por especie;
- daño sigue siendo coste y geometría imposible sigue siendo veto;
- reacción sigue siendo finita, dependiente de `MOVEMENT_SPEED` base y nunca instantánea;
- presupuestos S08 siguen acotando planificación sin matar goals vivos;
- no quedan rutas especiales capaces de girar remotamente antes de llegar a la frontera de lanzamiento.

## Gates documentales

### README / GUIDE
Deben describir el comportamiento actual de desarrollo, no beta.3:

- Gravity Fall ya no tiene steering especial con W;
- postura persistente + mirada + estabilización débil + drag transversal anisótropo;
- landing: 40 ticks de adquisición y 10 ticks máximos de presentación final;
- agua: WASD camera-relative con pitch, Space/Shift mundo ±Y, camera world-up en nado libre y support-up al estar apoyado;
- mascotas: planner history-free, sin breadcrumbs, owner airborne con tracking filtrado;
- mobs genéricos con efecto pueden usar transiciones gravitatorias para perseguir/escapar cuando vanilla no resuelve;
- Reorientation puede reaccionar en vuelo a geometría/target tras latencia; Clinging gastado no obtiene un giro extra.

### ARCHITECTURE
Debe reflejar:

- causalidad `look -> body attitude -> anisotropic aerodynamics -> trajectory`;
- predictor compartido `TrajectoryPrediction`;
- `LandingPrediction.ACQUISITION_TICKS = 40`;
- S05 launch-region hasta cuatro nodos × cinco direcciones (`<=20` forecasts);
- `MobGravityNavigation`, `PetGravityFollow`, `MobFlightMonitor/Reaction/Reactor` y `MobGravityPlanningBudget`;
- frame acuático separado de gravedad lógica;
- breadcrumbs sólo como historia de alpha.15, nunca arquitectura vigente.

### VALIDATION
Añadir sección de campaña beta.4/unreleased con:

- evidencia de sprints/gates finales;
- rojos útiles que cambiaron fixtures/contratos sin rebajar seguridad;
- distinguir validación automática de QA manual;
- no afirmar porcentajes de rendimiento sin perfilado reproducible de modpack real.

### CHANGELOG
Rellenar `Unreleased` con cambios de gameplay, IA, agua/landing/aerodinámica y eficiencia. Mantener alpha.15 breadcrumbs como registro histórico.

## Búsquedas legacy obligatorias

Antes del cierre, revisar ocurrencias actuales de:

- `breadcrumb` / `breadcrumbs`;
- `replay` aplicado a pet follow;
- `Air-diving with W` / `W air-diving` / steering de 6° como comportamiento vigente;
- `LandingPrediction.MAX_TICKS` descrito como 10;
- “max five forecasts” como límite del planner actual;
- owner airborne descrito como motivo para no seguir.

Las ocurrencias históricas en changelog/validation pueden permanecer si están claramente fechadas como comportamiento antiguo.

## Criterio de cierre

S09 sólo se marca **CERRADO** tras un HEAD documentalmente completo cuya matriz amplia esté verde. No se cambia versión, etiqueta, `main` ni se publica prerelease salvo instrucción explícita posterior.
