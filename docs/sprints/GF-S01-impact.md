# GF-S01 — Impact kernel y retirada de fallDistance segmentado

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN**.

## Tesis

Al cerrar S01, una entidad cuya física gravitatoria pertenece a Clinging/Reorientation recibe daño según la velocidad realmente absorbida por una colisión, independientemente de cuál sea su gravedad en ese tick. Frenar físicamente reduce el daño; cambiar de frame en el último instante no lo borra. La severidad se expresa como caída vanilla equivalente y se entrega al pipeline vanilla/bloque cuando existe una superficie de bloque real.

## Scope

FR-GF-001..003 y FR-GF-070..079. No incluye landing predictor, cámara ni Gravity Fall.

## Estado real investigado

- Gravity Changer captura dentro de `Entity.move` el movimiento solicitado antes de `collide` y el movimiento permitido después.
- `GravityMovementEntity.gravitychanger$getLastMoveDelta()` expone el movimiento intentado.
- Gravity Changer redirige `checkFallDamage` para acumular distancia usando la componente local del movimiento según la gravedad actual.
- Clinging resetea hoy `fallDistance` en `write`, `writeTransition` y `MobGravity.commitTurn` al cambiar de dirección.
- Ese reset evita mezclar segmentos, pero permite que el historial de velocidad no sea la magnitud física primaria.

## Modelo elegido

### Observable de impacto

Para cada `Entity.move` gestionado se capturan:

- posición inicial;
- movimiento solicitado/intencionado;
- posición final antes de `checkFallDamage`;
- desplazamiento realmente permitido.

Una componente se considera absorbida sólo cuando la colisión reduce su magnitud en el mismo sentido de viaje más allá de epsilon. Las componentes absorbidas del mismo movimiento se combinan como un único vector de impacto; no se cobra daño por eje.

### Velocidad → caída equivalente

`ImpactPhysics` simula/invierte la caída vanilla estándar (gravedad 0.08 y drag 0.98) para obtener la distancia de caída que produciría aproximadamente una velocidad preimpacto dada. La tabla/loop sólo necesita cubrir el rango de supervivencia ordinaria; por encima se satura en una distancia ya letal. No se multiplica de nuevo por gravity strength.

### Pipeline vanilla

- Si el impacto coincide con el suelo gravitatorio y `checkFallDamage` ya proporciona `BlockState/BlockPos`, se invoca el `fallOn` de ese bloque con la distancia equivalente.
- Si el impacto es lateral respecto al frame actual, se localiza de forma acotada el bloque cuya collision shape toca la cara bloqueada dominante del AABB final y se invoca su `fallOn`.
- Si no existe bloque (p.ej. una collision shape externa), se delega directamente en `causeFallDamage` con `damageSources().fall()`.
- Se mantiene el resto del pipeline vanilla de LivingEntity (Feather Falling, inmunidades, sonidos/daño). No se crea una tabla paralela de bloques.

### Ownership

El motor se arma cuando Clinging realmente posee física:

- player: `ClingingReoriented.controlsPhysics`;
- non-player: ownership `OWNED_EFFECT` o `BORROWED_RIDER` válido.

Una vez armado, no se desarma por expiración/retiro mientras la entidad continúe airborne: así salir de ownership no puede borrar una velocidad peligrosa. Se desarma tras soporte/impacto resuelto o lifecycle fuerte.

## Plan

- [ ] I1 `ImpactPhysics`: kernel puro de componentes absorbidas + velocidad + caída equivalente.
- [ ] I2 `ImpactState` por LivingEntity para movimiento capturado/armed lifecycle.
- [ ] I3 mixin de `Entity.move/checkFallDamage` que cancela la acumulación distance-based sólo cuando el tracker está armado.
- [ ] I4 localizar bloque impactado lateralmente con probe acotado; fallback a `causeFallDamage` sin bloque.
- [ ] I5 conservar pipeline vanilla y evitar duplicado Elytra.
- [ ] I6 tests unitarios de geometría/curva y GameTests de drop baseline, giro tardío, frenado, tangencial y multieje.
- [ ] I7 sólo después de verde, retirar resets por giro en player/mob y reemplazar tests antiguos por invariantes de momentum/impacto.
- [ ] I8 holdout adversarial + revisión cero-cambios.

## Modelo adversarial previo

- delta solicitado cero/no finito;
- step-up hace que un eje real avance más que el solicitado;
- collision epsilon produce micro-daño al caminar;
- choque diagonal bloquea X+Z y cobra dos veces;
- cambio DOWN→EAST justo antes de suelo DOWN;
- inversión temprana reduce velocidad y debe reducir daño;
- exit de efecto un tick antes de impacto;
- Slime/hay/water/Feather Falling;
- entity collider sin BlockState;
- Elytra no puede recibir doble kinetic damage;
- teleport/piston no debe reutilizar un move sample stale;
- cliente no puede autorizar daño;
- gravedad fuerte: velocidad ya incorpora la fuerza, no multiplicar dos veces;
- velocidades por encima de terminal vanilla deben saturar monotónicamente, nunca bajar daño.

### Holdout reservado

Se reserva una secuencia `DOWN high-speed → EAST un tick antes → impacto DOWN + ownership retirado inmediatamente después del giro`. Debe seguir dañando una sola vez según la velocidad absorbida.
