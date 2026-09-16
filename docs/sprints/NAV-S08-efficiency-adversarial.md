# NAV-S08 — eficiencia, presupuestos y adversarial

Estado: **EN PROGRESO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Objetivo

S08 demuestra y endurece el coste del sistema gravitatorio general construido en S05-S07. No se aceptan benchmarks de pared como contrato: los gates principales cuentan trabajo lógico (consultas de path, forecasts y sweeps) y verifican presupuestos deterministas.

## Invariantes de coste

1. **Vanilla primero.** Un objetivo que ya resuelve la navegación ordinaria no ejecuta forecasts gravitatorios.
2. **Sin capacidad, sin física cara.** Un mob sin Clinging/Reorientation puede atravesar los bridges de intención, pero no evalúa transiciones.
3. **Planner local acotado.** Una planificación estratégica usa una única navegación espejo y como máximo cuatro nodos de lanzamiento por cinco gravedades alternativas: `<= 20` forecasts físicos.
4. **No planner de superficie en vuelo.** Un mob `COMMITTED` usa sólo monitor volumétrico corto y reacción; no llama a pathfinding táctico hasta soporte/recovery.
5. **Horizonte de monitor ligado a reacción.** La identidad del landing comprometido se revalida aparte; el forecast dinámico sólo necesita cubrir `reactionTicks + margen`, acotado por el máximo del monitor. Un peligro detectado más tarde que su propia latencia ya no sería legalmente esquivable.
6. **Presupuesto global grounded.** Varios mobs bloqueados en el mismo tick no pueden iniciar simultáneamente un número ilimitado de planificaciones gravitatorias caras. Las solicitudes excedentes se difieren sin inventar una ruta vanilla ni perder la intención.
7. **Equidad eventual.** El presupuesto por tick no puede condenar siempre a los mismos mobs; una solicitud diferida debe adquirir turno en ticks posteriores si continúa siendo válida.
8. **Cooldowns siguen mandando.** Un miss o una maniobra fallida no relanza el planner cada tick.
9. **Memoria acotada.** Las exclusiones de maniobras y estados reactivos no crecen sin límite y se limpian al perder ownership/entidad.
10. **Nada de trabajo para mobs quiescentes.** NONE/EXTERNAL sin `airUsed` sigue saliendo antes de `MobGravity.tick`/`MobFlightReactor.tick`.

## S08-A — horizonte aéreo mínimo suficiente

El monitor dinámico se ejecuta sólo para mobs realmente en vuelo owned. Su horizonte será:

`monitorHorizon = min(DEFAULT_HORIZON, reactionTicks + REACTION_MARGIN_TICKS)`

con un margen pequeño explícito. La landing comprometida lejana no depende de ese horizonte porque su `Contact` se conserva y revalida directamente.

Gates:

- mínimo de reacción conserva un horizonte > reacción;
- máximo de reacción nunca supera `DEFAULT_HORIZON`;
- velocidades de mob normales producen menos trabajo que el horizonte fijo de 20 ticks;
- la regresión de landing comprometida lejana sigue verde;
- bloqueo temprano sigue permitiendo reacción y bloqueo demasiado tardío sigue produciendo impacto natural.

## S08-B — presupuesto global de planificación

Se introduce un presupuesto server-side por `ServerLevel` y tick para **nuevas planificaciones gravitatorias grounded**. No limita física ya comprometida ni navegación vanilla.

Primera política:

- tokens renovados por tick;
- coste de una planificación local = un token independientemente de cuántos candidatos descarte internamente, porque S05 ya limita ese trabajo a `<=20` forecasts;
- solicitudes genéricas que no obtienen token conservan su intención en una fase `WAITING_PLAN` y se reintentan en ticks posteriores;
- `PetGravityFollow` no necesita reclamar ownership para esperar: simplemente no despierta/replica planificación hasta disponer de token;
- no se consume token si el contexto ya demuestra que no hay capacidad gravitatoria.

El número de tokens se mantiene pequeño y explícito; S08 lo documentará con tests de equidad, no con porcentajes inventados.

## Gates de trabajo lógico

- effect-free blocked mob: 0 transition evaluations;
- powered reachable path: 0 transition evaluations;
- powered blocked local planner: `1..20` transition evaluations;
- exclusiones locales nunca elevan el máximo sobre 20;
- varios mobs bloqueados en el mismo tick respetan el presupuesto global;
- solicitudes diferidas progresan en ticks siguientes;
- un intent repetido mientras ya existe APPROACH no crea otro plan;
- un miss negativo respeta el throttle de 10 ticks;
- flight monitor no invoca navegación de superficie.

## Adversarial

- obstáculo dinámico aparece con margen: percepción inmediata, reacción sólo tras latencia, corrección legal si existe;
- obstáculo aparece demasiado tarde: impacto natural antes de poder reaccionar;
- soporte comprometido lejano desaparece fuera del horizonte corto: se detecta por revalidación del `Contact` guardado;
- target jitter sub-umbral: no thrash;
- target cambia sostenidamente durante vuelo largo: intención se actualiza;
- Clinging gastado nunca recibe segundo giro;
- Reorientation no toma una corrección que aumente riesgo sólo para ahorrar distancia de target;
- entrada inesperada en fluido cancela ownership de locomoción especial y deja recuperación al contexto acuático;
- geometría/chunk desconocido falla cerrado;
- memoria de maniobras fallidas sigue limitada a ocho entradas por ejecutor.

## Criterio de cierre

S08 se cierra sólo con matriz amplia verde y evidencia de que el coste caro está acotado por construcción. No se publican cifras de rendimiento porcentuales sin perfilado reproducible del servidor/modpack real.
