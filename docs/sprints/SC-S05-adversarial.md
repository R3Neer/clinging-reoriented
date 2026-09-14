# SC-S05 — Campaña adversarial

Estado: **CERRADO / GATE VERDE**.

## Tesis

S05 no añade una mecánica nueva: intenta romper, en combinación, las invariantes de Shulker Charge construidas por S00-S04. El cierre exige autoridad de servidor, captura exactamente una vez, locks estables, reacquisition desde posición actual, dispenser vanilla-real, Target Blocks reales, concurrencia independiente y ausencia de regresiones en las lanes opcionales del mod completo.

## Ataques cubiertos

- dispenser real con block entity, redstone, facing y consumo exacto;
- lock válido que no puede ser robado por un candidato posterior mejor puntuado;
- Charge inicialmente libre que adquiere automáticamente un objetivo aparecido después;
- target transferido de dimensión que invalida el lock y permite adquirir otro objetivo local;
- carreras mixtas arrow + melee sobre proyectil propio y ajeno con un único drop;
- dos Charges simultáneas con estado/target/reacquisition independientes;
- stress de múltiples Charges sin target atravesando varios ciclos de retry sin locks fantasma;
- cobertura acumulada de shield, expiry, Target Block, LOS, persistencia, shulker duplication y recaptura de sprints previos.

## Incidencias y clasificación

### Run #701 — fixture de simulación

El holdout `freeFlightAutomaticallyAcquiresTargetThatAppearsLater` llegaba al tick 42 con el proyectil prácticamente congelado (`tickCount=4`) en la lane opcional de Scale Brews. El patrón reproducía la salida del área realmente simulada, no una interacción de producción con Scale.

El primer helper sólo mantenía `ENTITY_TICKING` el chunk de nacimiento. Se generalizó el fixture para ticketear el corredor de vuelo completo: desde el punto de spawn hasta 32 bloques en la dirección de intent, más un chunk de margen. No se añadió ninguna rama especial `if Scale` ni se tocó producción.

### Run #706 — entorno externo

La primera ejecución tras el hardening del corredor falló antes de compilar: Modrinth devolvió HTTP 503 al resolver Gravity Changer, Alex's Mobs Continued, CodxLib y Cloth Config. Clasificación: **environment**. No justificó cambios de código ni de tests.

## Evidencia de cierre

- **Run #710** (`34895769482`): matriz completa verde en la rama Shulker antes de sincronizar con el `main` publicado de alpha.14. Pasaron build/JUnit, server GameTests, cliente por defecto, First Person, Scale Brews servidor/cliente, Fresh Animations y validación semántica de snapshots.
- Después se fusionó el `main` actual en `feature/shulker-charge`, incorporando alpha.14 y el hardening posterior de pet breadcrumb pursuit sin reescribir la feature.
- **Run #714** (`34897063938`), cabeza `a16d16faaa2cbd3c4b51678f08310ebc9a8bf681`: matriz completa post-merge verde. Pasaron build/JUnit, server GameTests, cliente por defecto, First Person 2.7.2, Scale Brews beta.5 servidor y cliente, Fresh Animations/FA Player/EMF/ETF y el validador de snapshots.

La revisión final de S05 no exige cambios de producción. Los únicos arreglos posteriores a los holdouts fueron de fixture/entorno y de presentación del asset ya aprobado.

## Gate

Cumplido: matriz combinada verde sobre la combinación real con alpha.14, snapshots coherentes, fallos clasificados antes de corregirlos, revisión sin parche de producción y CI completa posterior sobre el HEAD exacto de evidencia. S06 puede canonizar y versionar Shulker Charge como **0.1.0-alpha.15**.
