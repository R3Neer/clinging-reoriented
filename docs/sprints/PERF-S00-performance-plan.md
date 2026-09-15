# PERF-S00 — Plan TM de rendimiento para beta.3

Estado: **EN CURSO — ANÁLISIS / GATES DEFINIDOS**.

## Objetivo

Reducir coste de CPU, churn de heap y picos de trabajo de Clinging: Reoriented sin cambiar gameplay, autoridad, timings visuales ni compatibilidad. La campaña nace de `0.1.0-beta.2` (`5dfc36500f681e4ed8394c8e91eeb4950b9917df`) y prepara una prerelease pequeña `0.1.0-beta.3` dedicada a rendimiento/estabilidad.

No se atribuyen a este mod congelaciones observadas en un modpack grande sin perfilado reproducible. El objetivo es eliminar trabajo objetivamente innecesario en rutas calientes y acotar operaciones que hoy pueden concentrar miles de comprobaciones en un solo tick.

## Principios TM

1. Cada optimización conserva primero un contrato funcional observable.
2. Se cambia un hot path por sprint lógico y se ejecutan los tests relevantes antes de ampliar alcance.
3. Ninguna mejora depende de reducir cobertura, radio, cadencia, precisión de cámara o compatibilidad.
4. Los casos raros caros se presupuestan entre ticks; no se omiten candidatos ni se cambia su orden lógico.
5. La matriz completa de release sigue siendo obligatoria: JUnit, server GameTests, cliente normal, First Person, Scale Brews server/client, Fresh Animations y snapshots.
6. No se publican porcentajes de mejora sin una medición reproducible. Las notas de usuario describirán escenarios beneficiados, no detalles de implementación ni cifras inventadas.

## Hallazgos de entrada

### P1 — Render global

`GravityFallRenderMixin` toca un `ThreadLocal<ArrayDeque<Boolean>>` alrededor de `EntityRenderDispatcher.submit` y elimina el valor al vaciarse. En escenas con muchas entidades esto provoca trabajo y churn de heap aunque la entidad renderizada no pueda usar Gravity Fall.

**Contrato:** el push/pop de PoseStack debe seguir perfectamente balanceado, incluidos renders anidados; First Person/Fresh Animations conservan el mismo root visual.

### P2 — Tick global de LivingEntity

`DirectionalFallGraceMixin` hace `ThreadLocal get/set/remove` alrededor de cada `LivingEntity.tick`. `MobGravityMixin` llama a `MobGravity.tick` para cada entidad viva; la ruta ordinaria puede consultar efectos y pasajeros aunque la entidad no tenga ownership del mod.

**Contrato:** timers direccionales, mounts/pets, préstamos de gravedad y ownership externo mantienen exactamente las mismas transiciones.

### P3 — Restauración / retirada puntual

`ClingingReoriented.retire` y `MobGravity.restore` pueden recorrer miles de posiciones de recuperación en una sola llamada. Cada candidato puede disparar colisión, chunks y compatibilidad.

**Contrato:** se conserva el mismo conjunto y orden de candidatos; únicamente se reparte la búsqueda entre ticks con presupuesto acotado.

### P4 — Predicción de landing

Cada sweep crea y ordena un snapshot nuevo del registro de `LandingSurfaces`, pese a que el registro cambia raramente.

**Contrato:** orden estable por provider id y prioridad de soporte en empate permanecen idénticos.

### P5 — Compatibilidad y superficies móviles

Scale/Anatomy usan reflection en llamadas frecuentes y `MovingSurface.canBind` crea estructuras de ciclo por candidato.

**Contrato:** ausencia/incompatibilidad de Scale Brews sigue fallando cerrada y la detección de ciclos no puede aceptar un grafo antes rechazado.

### P6 — Interpolación visual

`GravitySnapMixin` consulta `VisualTransitions.override` en cada `GravityRotationAnimation.getRotation`; la ruta normal sin transición activa entra igualmente en el mapa sincronizado.

**Contrato:** HOLD/LAND/SNAP conservan ownership, secuencias y tiempos; cuando no hay ownership Clinging, Gravity Changer permanece completamente autoritativo.

### P7 — Gravity Charge

La reacquisición está acotada pero crea objetos temporales evitables. No se reducirá el cono, alcance, fan de Target Blocks ni cadencia de reintento.

**Contrato:** targeting/ranking/LOS/direct-ray priority y routing permanecen idénticos.

### P8 — Cámara/quaternions

`Quaternionf`/`Vector3f` son objetos Java mutables. La cámara beta.2 realiza copias defensivas correctas, pero algunas copias por frame pueden sustituirse por destinos/scratch sin compartir estado mutable.

**Contrato:** CAM-S01…S05, full-sphere 360°, 1ª↔3ª persona y snapshots deben quedar bit-semánticamente equivalentes según los invariantes existentes.

## Sprints

- **PERF-S01:** fast paths de render y contexto global.
- **PERF-S02:** fast path de MobGravity y cache de lookup de efecto.
- **PERF-S03:** búsqueda de recuperación presupuestada + equivalencia del orden legacy.
- **PERF-S04:** cache de providers de landing y reducción de churn de geometría.
- **PERF-S05:** compatibilidad/superficies móviles y fast path de interpolación visual.
- **PERF-S06:** Gravity Charge y allocations de cámara/body de riesgo bajo-medio.
- **PERF-S07:** campaña adversarial, docs y release beta.3.

## Gate de release

Beta.3 sólo se integra si:

- todos los contratos funcionales existentes siguen verdes;
- las nuevas pruebas de equivalencia de recuperación y fast paths están verdes;
- no se introduce un nuevo camino de gameplay específico de mod de compatibilidad;
- la matriz completa pasa en el HEAD final de rama y vuelve a pasar en el commit exacto integrado en `main`;
- `v0.1.0-beta.3` se publica desde los JAR del run verde de `main`, sin recompilar.
