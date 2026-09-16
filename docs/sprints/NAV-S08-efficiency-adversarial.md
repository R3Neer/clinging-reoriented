# NAV-S08 — eficiencia, presupuestos y adversarial

Estado: **CERRADO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Objetivo

S08 demuestra y endurece el coste del sistema gravitatorio general construido en S05-S07. Los gates principales cuentan trabajo lógico —consultas de path, forecasts y sweeps— y verifican presupuestos deterministas; no se convierten benchmarks aislados en promesas de rendimiento porcentual.

## Invariantes de coste

1. **Vanilla primero.** Un objetivo que ya resuelve la navegación ordinaria no ejecuta forecasts gravitatorios.
2. **Sin capacidad, sin física cara.** Un mob sin Clinging/Reorientation puede atravesar los bridges de intención, pero no evalúa transiciones.
3. **Planner local acotado.** Una planificación estratégica usa una única navegación espejo y como máximo cuatro nodos de lanzamiento por cinco gravedades alternativas: `<= 20` forecasts físicos.
4. **No planner de superficie en vuelo.** Un mob `COMMITTED` usa sólo monitor volumétrico corto y reacción; no llama a pathfinding táctico hasta soporte/recovery.
5. **Horizonte de monitor ligado a reacción.** La identidad del landing comprometido se revalida aparte; el forecast dinámico cubre `reactionTicks + 2`, limitado por el horizonte máximo del monitor de 20 ticks. Un peligro descubierto más tarde que su propia latencia ya no sería legalmente esquivable.
6. **Presupuesto global grounded.** Nuevas planificaciones gravitatorias grounded están limitadas por nivel y tick: como máximo 32 globales y 4 dentro de cada región X/Z de 64×64 bloques.
7. **Equidad eventual.** Las solicitudes que no obtienen presupuesto pasan a `WAITING_PLAN`, conservan intención y vuelven a intentarlo en ticks posteriores; no continúan siguiendo una ruta parcial obsoleta mientras esperan.
8. **Cooldowns siguen mandando.** Un miss o una maniobra fallida no relanza el planner cada tick.
9. **Memoria acotada.** Las exclusiones de maniobras fallidas se limitan a ocho entradas por ejecutor y el estado reactivo se libera al perder ownership/entidad.
10. **Nada de trabajo para mobs quiescentes.** NONE/EXTERNAL sin `airUsed` sigue saliendo antes del runtime gravitatorio caro.

## S08-A — horizonte aéreo mínimo suficiente

`MobFlightReactor` calcula:

`monitorHorizon = min(20, reactionTicks + 2)`

con `reactionTicks` previamente acotado por `MobReactionTime` a 2–10 ticks.

La superficie comprometida no depende de ese horizonte corto: su `LandingSurfaces.Contact` se conserva y revalida de forma independiente. El monitor sólo busca cambios materiales del segmento próximo; no intenta volver a resolver toda la ruta.

Gates cerrados:

- el horizonte siempre supera la propia latencia de reacción;
- nunca excede el límite del monitor;
- mobs normales observan menos futuro que el horizonte fijo anterior de 20 ticks;
- un landing comprometido lejano sigue siendo revalidado aunque quede fuera del forecast corto;
- un bloqueo temprano conserva una ventana de reacción y uno demasiado tardío termina en impacto natural;
- el monitor no invoca navegación de superficie.

## S08-B — presupuesto global y regional de planificación

`MobGravityPlanningBudget` limita sólo el comienzo de **nuevas planificaciones gravitatorias grounded**. No limita navegación vanilla, física ya comprometida, ni el monitor aéreo.

Política cerrada:

- `GLOBAL_PLANS_PER_TICK = 32` por nivel;
- `REGION_PLANS_PER_TICK = 4`;
- región X/Z de `64×64` bloques (`REGION_SHIFT = 6`);
- el contador se renueva con `level.getGameTime()`;
- una planificación local consume un slot, mientras S05 mantiene su propio límite interno de `<=20` forecasts;
- una petición generic que no obtiene slot pasa a `WAITING_PLAN` sin perder la intención;
- pet follow comparte el mismo presupuesto en vez de mantener una vía paralela ilimitada;
- un mob sin capacidad no consume presupuesto gravitatorio.

Esta política evita dos extremos igualmente inútiles: permitir que una horda dispare todas las búsquedas caras en el mismo tick o imponer un límite global tan pequeño que una región distante pueda quedar hambrienta porque otra concentra todo el trabajo.

## Gates de trabajo lógico

Quedan cubiertos en tests de planner/runtime:

- effect-free blocked mob: 0 transition evaluations;
- powered reachable path: 0 transition evaluations;
- powered blocked local planner: `1..20` transition evaluations;
- exclusiones locales nunca elevan el máximo sobre 20;
- varios mobs bloqueados en el mismo tick respetan los límites global y regional;
- solicitudes diferidas entran en `WAITING_PLAN` y progresan en ticks siguientes cuando reciben presupuesto;
- un intent repetido mientras existe `APPROACH` no crea otro plan;
- un miss negativo conserva el throttle de 10 ticks;
- flight monitor no invoca navegación de superficie;
- pet follow y navegación genérica comparten presupuesto en lugar de duplicarlo.

## Adversarial cerrado

- obstáculo dinámico con margen: percepción inmediata, reacción sólo tras latencia y corrección únicamente si existe una maniobra legal;
- obstáculo demasiado tardío: impacto natural antes de poder reaccionar;
- soporte comprometido lejano desaparece fuera del horizonte corto: se detecta por revalidación del `Contact` guardado;
- target jitter sub-umbral: no thrash;
- target cambia sostenidamente durante vuelo largo: la intención se actualiza;
- Clinging gastado nunca recibe un segundo giro;
- Reorientation no acepta una corrección hacia target que aumente el riesgo sólo para ahorrar distancia;
- entrada inesperada en fluido libera la locomoción especial y deja recuperación al contexto correspondiente;
- geometría/chunk desconocido falla cerrado;
- memoria de maniobras fallidas permanece limitada a ocho entradas por ejecutor.

## Evidencia de cierre

Implementación principal de S08:

- `850105d7`: acota el flight monitor por horizonte de reacción.
- `2b925c77`: añade gates del horizonte dinámico.
- `ae78f39c`: introduce presupuesto grounded por nivel/región.
- `f6b27700`: cubre límites globales y regionales.
- `cb723a34`: difiere trabajo excedente mediante `WAITING_PLAN`.
- `9325b647`: integra pet follow en el mismo presupuesto.
- `29820358`: cubre ownership mientras una planificación está diferida.
- `8400842a`: registra los GameTests de presupuesto.

La convergencia posterior añadió además los gates de goals vivos, owner aéreo y flee sobre la misma infraestructura. El HEAD `b2de88e83e1e64416288d220c8d86d52aeca014d` pasó la matriz completa en **CI #1038 / run `35105097956`**: build/JUnit, GameTests de servidor, cliente base, First Person, Scale Brews server/client, Fresh Animations y validación de snapshots.

## Cierre

S08 queda cerrado. El trabajo restante de la campaña es S09: convergencia documental y verificación de que código, tests y documentación describen la misma semántica. No se publica ningún porcentaje de mejora de rendimiento sin perfilado reproducible de un servidor/modpack representativo.
