# Flujo TM por sprints — Gamefeel / Gravity Fall

Estado: protocolo operativo temporal para `chatgpt-gamefeel-camera`. Se inspira en el flujo de `scale-brews:chatgpt-editing`, adaptado a una actualización donde física y presentación deben validarse juntas.

## Ciclo obligatorio

```text
SCOPE
  ↓
INVESTIGACIÓN DEL ESTADO REAL
  ↓
PLAN ↺
  ↓
MODELO ADVERSARIAL ↺
  ↓
IMPLEMENTACIÓN ↺
  ↓
TESTS + SNAPSHOTS
  ↓
CLASIFICAR FALLOS
  ├─ implementación → implementación
  ├─ plan → plan
  ├─ requisito → requisitos
  ├─ arquitectura → arquitectura/plan
  ├─ test/snapshot → tests
  └─ entorno → corregir evidencia
  ↓
REVISIÓN FINAL ↺
  ↓
PASADA COMPLETA SIN CAMBIOS
  ↓
EVIDENCIA + COMMIT
```

`↺`: si una revisión produce cambios, la revisión de esa fase comienza de nuevo hasta una pasada completa sin cambios.

## Antes de implementar cada sprint

El registro de sprint debe fijar:

- tesis técnica única;
- FR/NFR incluidos;
- invariantes vecinas;
- exclusiones;
- estado actual leído del código;
- estado objetivo demostrable;
- checklist concreta;
- modelo adversarial previo;
- al menos un holdout no codificado todavía.

No se empieza si no puede escribirse: «al cerrar este sprint X será cierto, demostrado mediante Y, sin asumir Z».

## Revisión de plan

Pasadas mínimas:

1. requisitos;
2. ownership/arquitectura;
3. código real/callers/mixins;
4. lifecycle/error/fail-closed;
5. compatibilidad y APIs externas;
6. simplicidad;
7. verificabilidad;
8. gamefeel: preguntarse explícitamente si cada añadido mejora legibilidad/control o sólo añade sofisticación.

## Modelo adversarial

Considerar conscientemente:

- fronteras exactas de ticks/ETA/velocidad cero;
- 90°/180° y secuencias rápidas;
- invalidación entre predicción y contacto;
- identidad stale de superficies externas;
- NaN/∞/vectores degenerados;
- dos cambios legítimos en el mismo tick;
- input durante commitment;
- teleport/dimension/death/respawn;
- mounts/passengers;
- water/Elytra/Creative;
- cliente vs servidor y latencia;
- camera/body divergence;
- Fresh Animations/First Person;
- surfaces externas sin Scale presente;
- mutation mindset (invertir una comparación, quitar lock, resetear airborne timer, rotar momentum, etc.).

Reservar al menos un holdout por sprint para revelarlo después de implementar.

## Tests y snapshots

Los tests no validan detalles internos sino observables de requisito.

Matriz obligatoria:

`ataque/propiedad → requisito → nivel → resultado → evidencia`

Niveles: unit/kernel, server GameTest, client GameTest, compat fixture, snapshot visual, QA humana.

### Política de snapshots

Los client GameTests deben capturar snapshots en checkpoints nombrados de forma estable. No se acepta una única imagen final para un comportamiento temporal.

Cada snapshot importante debe ir acompañado, cuando sea posible, de asserts de:

- gravedad física;
- velocidad mundial;
- frame visual/quaternion;
- estado AIRBORNE/SUSTAINED/LANDING_COMMITTED;
- tiempo/fracción de landing;
- lock de input.

Se permite un programa temporal que inspeccione/coteje snapshots (dimensiones, diferencias, regiones, silhouettes, metadatos o manifests). No debe convertirse en runtime del mod.

## Cierre

Un sprint sólo cierra cuando:

- implementación converge sin cambios en revisión completa;
- batería aplicable está verde;
- snapshots esperados se produjeron y son coherentes con asserts;
- holdouts fueron revelados y superados;
- evidencia queda registrada en el documento del sprint;
- deuda fuera de scope queda explícita.

Un test verde no reemplaza la revisión; una revisión favorable no reemplaza tests ni snapshots.
