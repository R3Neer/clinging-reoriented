# PERF-S00 — Campaña TM de rendimiento para beta.3

Estado: **CÓDIGO CERRADO / RELEASE-PREP**.

## Objetivo

Reducir coste de CPU, churn de heap y picos de trabajo de Clinging: Reoriented sin cambiar gameplay, autoridad, timings visuales ni compatibilidad. La campaña nace de `0.1.0-beta.2` (`5dfc36500f681e4ed8394c8e91eeb4950b9917df`) y prepara la prerelease pequeña `0.1.0-beta.3`, dedicada a rendimiento y estabilidad.

No se atribuyen a este mod congelaciones observadas en un modpack grande sin perfilado reproducible. El objetivo ha sido eliminar trabajo objetivamente innecesario en rutas calientes y acotar operaciones capaces de concentrar miles de comprobaciones en un solo tick. No se publican porcentajes de mejora sin una medición reproducible.

## Principios TM

1. Cada optimización conserva primero un contrato funcional observable.
2. Se cambia un hot path por sprint lógico y se ejecutan los tests relevantes antes de ampliar alcance.
3. Ninguna mejora depende de reducir cobertura, radio, cadencia, precisión de cámara o compatibilidad.
4. Los casos raros caros se presupuestan entre ticks; no se omiten candidatos ni se cambia su orden lógico.
5. La matriz completa de release sigue siendo obligatoria: JUnit, server GameTests, cliente normal, First Person, Scale Brews server/client, Fresh Animations y snapshots.
6. Las notas públicas describen resultados observables, no detalles internos de Java ni cifras no medidas.

## Resultado por sprint

### PERF-S01 — Render y contexto global

- La ruta de render deja de crear/eliminar estado ThreadLocal para cada entidad renderizada: sólo los avatars entran en el bookkeeping de Gravity Fall y el almacenamiento por hilo se reutiliza.
- El contexto necesario para el limitador de caída de Gravity Changer conserva la semántica anterior pero reutiliza la entrada ThreadLocal en vez de borrarla/recrearla por cada `LivingEntity.tick`.
- Un intento de interceptar únicamente la llamada añadida por el mixin de Gravity Changer falló de forma roja en run **#822** porque MixinExtras no puede ver una INVOKE introducida por otro mixin en esa fase. Se descartó ese enfoque; no se relajó `require` ni se ocultó el fallo.

### PERF-S02 — Mobs y lookup de efecto

- Los mobs en estado quiescente `NONE`/`EXTERNAL`, sin trabajo pendiente, dejan de entrar en la máquina de estado de gravedad cada tick.
- La referencia al efecto Clinging de Alex's Mobs se resuelve y cachea en vez de reconstruir la búsqueda de registry en cada comprobación de ownership.

### PERF-S03 — Recuperación presupuestada

- La retirada del jugador y la restauración de mobs conservan exactamente el radio legacy de 4 bloques y los **2.108** offsets válidos en el mismo orden `radius → y → x → z`.
- Se prueban como máximo **64 candidatos por llamada**, continuando el cursor en ticks posteriores; tras agotar un ciclo completo se conserva la pausa legacy de 20 ticks.
- Los accesos internos necesarios para completar la misma transición original se resuelven una vez con MethodHandles.
- `RecoveryBudgetTest` reconstruye de forma independiente el bucle legacy y exige igualdad exacta de conjunto/orden y presupuesto.

### PERF-S04 — Landing

- El orden de `LandingSurfaceProvider` se mantiene como snapshot inmutable y sólo se reconstruye al registrar/desregistrar providers, en vez de asignar y ordenar una lista durante cada sweep.
- La revalidación del provider vanilla compara coordenadas AABB ya parseadas en vez de regenerar cadenas hexadecimales por shape. El formato de identidad pública no cambia.

### PERF-S05 — Compatibilidad y superficies móviles

- Scale Brews/Anatomy siguen enlazándose dinámicamente y sin dependencia de compilación, pero las llamadas frecuentes usan MethodHandles resueltos una vez en vez de `Method.invoke(Object...)` con varargs temporales.
- La detección de ciclos de superficies móviles reutiliza scratch por hilo.
- La integración de colisiones legacy evita copiar listas cuando no existe ninguna superficie compatible real.
- `VisualTransitions` evita lock/lookup del mapa cuando Clinging no posee ninguna transición visual activa.

### PERF-S06 — Gravity Charge y cámara

- Gravity Charge conserva 32 bloques, cono de 15°, prioridad de Target Block central, fan de Target Blocks, LOS, ranking y cadencia de reacquisición, reduciendo objetos temporales durante el scoring.
- La cámara full-sphere conserva el mismo ownership y API defensiva; el hot path de render copia a storage reutilizable y compone directamente sobre buffers de `Camera`, reduciendo quaternions/vector scratch por frame.

## Evidencia de cierre de código

HEAD de código antes de versionar: `be04f41ebe1289127837eac4e03b867e9d6e6db3`.

Run **#831** (`35008292945`) pasó la matriz completa:

- paridad `en_us` / `es_es`;
- build + JUnit;
- server GameTests;
- default client GameTests;
- First Person;
- Scale Brews server/client;
- Fresh Animations/EMF/ETF;
- snapshots semánticos, incluidos los holdouts CAM de beta.2 y Gravity Charge.

## Gate de release

Beta.3 sólo se integra si el HEAD final de release-prep vuelve a pasar la matriz completa. Después `main` debe pasar la misma matriz y `v0.1.0-beta.3` se publicará desde los JAR de ese run exacto, sin recompilar.
