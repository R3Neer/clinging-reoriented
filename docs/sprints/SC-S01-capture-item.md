# SC-S01 — Item, captura y Reorientation

Estado: **CERRADO / GATE VERDE**.

## Scope cerrado

SC-001..014 y SC-090..103 aplicables al recurso, captura y brewing. Lanzamiento, targeting, navegación y recaptura de una Charge ya disparada permanecen deliberadamente fuera de S01.

## Implementación

Cabeza de evidencia: `e9e49c2ac79aa52f60b9ab2ee9b1b423376f20de`.

- `Shulker Charge` registrada como item stack 64 con cooldown component de 0,5 s ya preparado para S02.
- Icono 2D aprobado incorporado en la rama por el trabajo de arte previo y preservado durante S01.
- `ShulkerBulletMixin` clasifica como captura una bullet golpeada por melee o por `AbstractArrow`, incluidas flechas ownerless de estilo dispenser.
- El fence `clinging$captured` se arma antes del drop, de modo que daños repetidos sobre la misma entidad no duplican el item.
- Destrucción genérica/no clasificada como intercepción activa no produce Charge. Los holdouts físicos de shield, impacto y expiry se vuelven a ejecutar en S03, donde existe ya el proyectil relanzado y pueden probarse end-to-end.
- Reorientation usa `Shulker Charge` para transformar Clinging/Long Clinging; `Shulker Shell` deja de ser ingrediente. Redstone, splash y lingering conservan sus rutas vanilla.
- Traducciones EN/ES añadidas.

## Tests y evidencia

CI **#475**, run `34852207815`, sobre el SHA exacto anterior: **success**.

Pasaron:

- build + unit tests;
- todos los server GameTests, incluidos stack 64, melee exactly-once, player arrow, ownerless/dispenser-style arrow, no-drop genérico y brewing nuevo/obsolete shell;
- client GameTests sin mods opcionales;
- First Person 2.7.2 + Not Enough Animations 1.12.4;
- Scale Brews beta.5 server/client;
- Fresh Animations 1.10.5 + Player Extension 1.1 + EMF 3.3.5 + ETF 7.2;
- validador de snapshots heredado de alpha.13 y upload de artefactos.

## Revisión

No se añadió entidad nueva ni renderer propio: el recurso no altera todavía la física de `ShulkerBullet`. El mixin de captura está aislado al hook server-side de daño y no cambia comportamiento de impacto. El brewing viejo queda rechazado explícitamente por GameTest.

## Gate

S01 cerrado. S02 puede crear la misma `EntityTypes.SHULKER_BULLET` como Charge lanzada y añadir adquisición/readquisición sin reabrir la economía básica salvo regresión demostrable.