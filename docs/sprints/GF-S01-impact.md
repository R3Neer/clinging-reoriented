# GF-S01 — Impact kernel y retirada de fallDistance segmentado

Estado: **CERRADO / GATE VERDE**.

## Tesis

Una entidad cuya física gravitatoria pertenece a Clinging/Reorientation recibe daño según la velocidad realmente absorbida por una colisión, independientemente de cuál sea su gravedad en ese tick. Frenar físicamente reduce el daño; cambiar de frame en el último instante no lo borra. La severidad se expresa como caída vanilla equivalente y se entrega al pipeline vanilla/bloque cuando existe una superficie real.

## Scope

FR-GF-001..003 y FR-GF-070..079. No incluye landing predictor, cámara ni Gravity Fall.

## Modelo final

### Observable de impacto

Para cada `Entity.move` gestionado se captura movimiento solicitado y desplazamiento realmente permitido. Una componente cuenta como absorbida sólo cuando la colisión reduce su magnitud en el mismo sentido de viaje más allá de epsilon. Las componentes absorbidas de un mismo movimiento se combinan como un único vector de impacto; no se cobra daño por eje.

### Velocidad → caída equivalente

`ImpactPhysics` convierte la velocidad preimpacto a una caída vanilla equivalente usando la recurrencia estándar de gravedad/drag y satura monotónicamente en el rango letal. La gravedad fuerte no vuelve a multiplicar el daño: ya está incorporada en la velocidad alcanzada.

### Pipeline vanilla

- Si existe bloque de impacto real, se delega en `fallOn` con la distancia equivalente.
- Para colisiones laterales/arbitrary-normal se usa un probe acotado post-`Entity.move` para localizar la superficie bloqueante dominante.
- Si no existe bloque, se delega en `causeFallDamage` con daño vanilla de caída.
- Se conserva el pipeline de `LivingEntity`, incluyendo Feather Falling, inmunidades y comportamiento de bloques.
- Elytra real no recibe daño duplicado por este sistema.

### Ownership

El motor se arma sólo cuando Clinging posee realmente la física. Una vez armado no se desarma por retirada/expiración mientras la entidad siga airborne, evitando borrar una caída peligrosa por salir de ownership justo antes del impacto. Se limpia tras soporte/impacto resuelto o lifecycle fuerte.

## Checklist de cierre

- [x] I1 `ImpactPhysics`: kernel puro de componentes absorbidas + velocidad + caída equivalente.
- [x] I2 `ImpactState` por LivingEntity para movimiento capturado/armed lifecycle.
- [x] I3 hook de `Entity.move/checkFallDamage` que evita acumulación distance-based cuando el tracker está armado.
- [x] I4 localización acotada de bloque impactado lateralmente y fallback sin bloque.
- [x] I5 conservación del pipeline vanilla y exclusión de doble daño Elytra.
- [x] I6 tests unitarios y GameTests de high/low/tangential/multiaxis/ownership.
- [x] I7 retirada de resets por giro en player/mob tras quedar cubierta la semántica nueva.
- [x] I8 holdouts y hardening acumulativo en S05.

## Holdout reservado y resultado

La secuencia `DOWN high-speed → EAST un tick antes → impacto DOWN + ownership retirado inmediatamente después del giro` queda absorbida por la cobertura de impacto tardío, ownership persistente y arbitrary-normal. El resultado esperado sigue siendo un único evento de daño según la velocidad absorbida, no según el frame gravitatorio final.

## Deuda explícita

S05 conserva como obligación revisar el cruce combinado de giro tardío + alta velocidad + normal de colisión distinta a gravedad sobre el HEAD exacto antes de declarar la campaña cerrada.
