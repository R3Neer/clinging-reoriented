# NAV-S07 — target tracking, mundo dinámico y tiempo de reacción

Estado: **EN PROGRESO**. Rama: `tm/gravity-navigation-gamefeel-beta4`.

## Objetivo

S07 añade percepción/reacción a la locomoción gravitatoria ya demostrada en S05/S06. No cambia la política de seguridad ni convierte a los mobs en entidades omniscientes.

La distinción central es:

**observar continuamente ≠ actuar continuamente**.

Un mob puede actualizar conocimiento del objetivo y del mundo durante una transición comprometida sin cancelar/replanificar cada tick. Las acciones sólo ocurren cuando el cambio es material, ha transcurrido la latencia de reacción y existe una respuesta físicamente legal.

## Reacción derivada de agilidad

`MobReactionTime` usa exclusivamente el **valor base** de `Attributes.MOVEMENT_SPEED`:

- nunca `deltaMovement`;
- no velocidad de caída;
- no knockback;
- no multiplicadores transitorios de Speed/Slowness en esta primera versión.

Curva saturante:

- mínimo absoluto: 2 ticks;
- máximo: 10 ticks;
- velocidad base mayor → reacción no más lenta;
- nunca 0 ticks;
- ganancias decrecientes a velocidades absurdamente altas.

Esto evita una tabla manual por especie y hace que la agilidad ya expresada por Minecraft informe también la rapidez de reacción.

## Observación del objetivo durante vuelo

Una transición `COMMITTED` conserva:

- forecast comprometido;
- objetivo/intención observada;
- anchor de objetivo usado al comprometer;
- última observación material;
- tick a partir del cual esa observación puede provocar reacción.

El objetivo se observa cada tick barato, pero un cambio sólo es **material** cuando supera un umbral espacial/estratégico. Pequeños quiebros no reinician la maniobra.

Para vuelos largos:

- el objetivo no se congela indefinidamente;
- desplazamiento sostenido/material actualiza la intención;
- la maniobra actual puede terminar si sigue siendo razonable;
- si deja de ser útil y Reorientation permite una corrección legal, ésta sólo puede ocurrir después de la latencia de reacción;
- Clinging gastado no inventa un segundo cambio: puede actualizar su próximo objetivo, pero debe obedecer su física hasta recuperar soporte.

Esto evita tanto `replan` por cada giro de cabeza como la mascota viajando kilómetros en una dirección obsoleta.

## Mundo dinámico durante vuelo

Mientras un segmento especial está `COMMITTED`, el controlador ejecuta un **flight monitor** acotado que vuelve a proyectar la trayectoria desde el estado físico actual con:

- AABB actual;
- gravedad actual;
- velocidad actual;
- `AirMotion.nextVelocity`;
- `LandingSurfaces.sweep`;
- horizonte restante pequeño/acotado.

No ejecuta pathfinding de superficie.

El monitor detecta:

- primer contacto distinto/adelantado;
- contacto que dejó de ser soporte;
- landing previsto que desapareció;
- geometría desconocida/no cargada;
- entrada inesperada en fluido.

Un cambio observado no provoca reacción instantánea. Se registra como evento y se vuelve accionable en `now + MobReactionTime.ticks(mob)`.

## El bloque colocado en la cara

Caso A, bloqueo con antelación suficiente:

1. el monitor ve que el primer contacto previsto cambió;
2. empieza el reloj de reacción;
3. antes de reaccionar el mob continúa su física normal;
4. al vencer la latencia, si todavía hay peligro y existe acción legal, corrige/replanifica.

Caso B, bloqueo demasiado tarde:

- el impacto ocurre antes de vencer la latencia;
- el mob se la pega;
- el estado físico posterior alimenta recuperación/replan.

No hay evasión en el mismo tick del bloque. Construir contra el mob sigue siendo gameplay válido.

## Acciones legales tras reaccionar

### Reorientation

Puede evaluar una corrección gravitatoria desde el estado aéreo real si la mecánica permite otro cambio. Debe pasar el mismo predictor volumétrico y no puede atravesar sólidos/unknown geometry.

### Clinging

Si `airUsed` ya está consumido, no puede ejecutar otro giro. La reacción puede cambiar intención futura o entrar en recovery, pero no fabricar capacidad.

### Sin solución legal

Mantener la física real. Impactar no es un bug cuando la amenaza apareció demasiado tarde o no existe una maniobra legal.

## Coste

El flight monitor sólo existe mientras hay una transición especial comprometida. No corre para todos los mobs del mundo.

Primera versión:

- una simulación corta por tick y por mob `COMMITTED`;
- sin pathfinding durante vuelo;
- acciones/replans sólo tras cambios materiales y latencia;
- S08 medirá estrés y podrá escalonar probes si muchos mobs vuelan a la vez.

## Gates S07

- curva de reacción monotónica y acotada;
- `deltaMovement` no cambia la latencia;
- objetivo que oscila poco no genera reacción estratégica;
- objetivo que se desplaza materialmente durante vuelo queda observado y, tras latencia, actualiza intención;
- un vuelo largo no mantiene eternamente un anchor obsoleto;
- bloque nuevo detectado con suficiente antelación genera reacción sólo después del delay;
- bloque puesto demasiado tarde produce impacto natural;
- Reorientation puede corregir sólo si la nueva maniobra es legal;
- Clinging gastado nunca recibe un segundo giro;
- el monitor no ejecuta pathfinding de superficie durante `COMMITTED`;
- desaparecido el peligro antes de vencer la latencia, no se ejecuta una maniobra fantasma.
