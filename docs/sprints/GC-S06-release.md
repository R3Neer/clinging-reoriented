# GC-S06 — Canonización, integración y release

Estado: **CERRADO / BETA.1 PUBLICADA**.

La línea alpha quedó cerrada con **0.1.0-alpha.15**, dedicada a pet gravity-breadcrumb pursuit. **Gravity Charge** inaugura deliberadamente la línea beta como **0.1.0-beta.1**.

## Entrada a S06

El gate adversarial de la feature ya había quedado verde antes de la promoción de versión:

- run **#710** (`34895769482`): matriz completa verde tras el hardening del corredor de vuelo;
- run **#714** (`34897063938`) sobre `a16d16faaa2cbd3c4b51678f08310ebc9a8bf681`: matriz completa post-alpha.14 verde, incluidas Scale Brews servidor/cliente, First Person, Fresh Animations y snapshots.

Alpha.15 se publicó desde `main` antes de beta.1 y quedó preservada como último alpha histórico.

## Canonización beta.1 completada

Beta.1 usa de forma canónica:

- nombre externo: **Gravity Charge**;
- español de España: **Carga de gravedad**;
- registry ID: `clinging_reoriented:gravity_charge`;
- clases/tests: `GravityCharge*`;
- assets: `gravity_charge*`;
- snapshots: `gravity-charge-*`;
- sprints: `GC-S00` … `GC-S06`;
- arte editable: `docs/art/gravity-charge/`.

El nombre provisional “Shulker Charge” y el ID `shulker_charge` no llegaron a ninguna release pública, por lo que no existe alias legacy. La receta de Reorientation usa `GravityCharges.ITEM`; los GameTests prueban además que **Shulker Shell ya no produce Reorientation**.

Los assets GUI/held y la textura de Gravity Charge son originales GPL-3.0-or-later del proyecto. La entidad lanzada conserva el renderer vanilla únicamente porque sigue siendo exactamente `minecraft:shulker_bullet`; no se redistribuyen assets de Mojang.

CI incorpora un gate explícito de paridad `en_us` ↔ `es_es`, incluyendo rechazo de claves españolas ausentes o vacías.

## Evidencia final

- **Rama final:** run **#754** (`34900246377`) sobre `6a22223bc20591a9e320bef84521fb402501df56` — verde completa: localización EN/ES, build/JUnit, servidor, cliente base, First Person, Scale Brews servidor/cliente, Fresh Animations y snapshots.
- **Integración:** `main` avanzó por fast-forward al mismo commit `6a22223bc20591a9e320bef84521fb402501df56`; no hubo un merge posterior que cambiara el árbol validado.
- **Main final:** run **#758** (`34901126185`) — misma matriz completa verde sobre el commit exacto de release.
- **Publicación:** workflow **Publish beta.1 prerelease #11** (`34902013181`) — success.
- **Tag/release:** `v0.1.0-beta.1`, target exacto `6a22223bc20591a9e320bef84521fb402501df56`.
- **JAR regular SHA-256:** `41d0d9f3d0fab9c504cab619facd740508442ba4350d2d680b39f126ed5630fb`.
- **Sources JAR SHA-256:** `c761f923dbbe45f51b5286a462525190d998f8564bfa6e9cf7ecda672d2a0215`.

El workflow de release descargó los dos JAR del artefacto exacto de #758 y **no recompiló** para publicar.

## Qué significa entrar en beta

Beta no relaja los gates. Significa que, con Gravity Charge integrada, el núcleo de gravedad, cámara, landing, lifecycle, compatibilidad y gameplay principal se considera suficientemente coherente para una fase de validación más amplia.

Sigue siendo prerelease: pueden aparecer bugs, tuning y compatibilidad adicional. No se declara feature-freeze ni estabilidad absoluta.

## Gate de salida — resultado

1. [x] Sincronizar con el último alpha publicado.
2. [x] Mantener alpha.15 como hito histórico independiente.
3. [x] Pasar la matriz completa en rama con nombre/ID beta finales.
4. [x] Integrar sólo después del verde de rama.
5. [x] Repetir la matriz completa sobre `main` exacto.
6. [x] Publicar desde el artefacto exacto de ese run, con hashes registrados.
7. [x] Crear `v0.1.0-beta.1` contra el mismo commit validado, sin segunda compilación.

GC-S06 queda cerrado. Gravity Charge es oficialmente la feature de entrada de Clinging: Reoriented en beta.
