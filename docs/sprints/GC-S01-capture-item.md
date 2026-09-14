# GC-S01 — Item, captura y Reorientation

Estado: **CERRADO / GATE VERDE**.

> Nota de canonización beta.1: el nombre provisional “Shulker Charge” usado durante el sprint se sustituyó antes de publicación por **Gravity Charge**. Las referencias de implementación finales de este documento usan el nombre canónico.

## Scope cerrado

GC-001..014 y GC-090..103 aplicables al recurso, captura y brewing. Lanzamiento, targeting, navegación y recaptura de una Gravity Charge ya disparada permanecen deliberadamente fuera de S01.

## Implementación

Cabeza de evidencia histórica: `e9e49c2ac79aa52f60b9ab2ee9b1b423376f20de`.

- `Gravity Charge` registrada como item stack 64 con cooldown component de 0,5 s ya preparado para S02.
- Icono 2D aprobado incorporado en la rama por el trabajo de arte previo y preservado durante S01.
- `ShulkerBulletMixin` clasifica como captura una bullet golpeada por melee o por `AbstractArrow`, incluidas flechas ownerless de estilo dispenser.
- El fence `clinging$captured` se arma antes del drop, de modo que daños repetidos sobre la misma entidad no duplican el item.
- Destrucción genérica/no clasificada como intercepción activa no produce Gravity Charge. Los holdouts físicos de shield, impacto y expiry se vuelven a ejecutar en S03, donde existe ya el proyectil relanzado y pueden probarse end-to-end.
- Reorientation usa `Gravity Charge` para transformar Clinging/Long Clinging; `Shulker Shell` deja de ser ingrediente. Redstone, splash y lingering conservan sus rutas vanilla.
- Traducciones `en_us` y `es_es` añadidas; beta.1 añade además gate automático de paridad de claves.

## Tests y evidencia

CI histórica **#475**, run `34852207815`: **success**.

Pasaron build/unit, todos los server GameTests aplicables, cliente base, First Person, Scale Brews server/client, Fresh Animations y el validador de snapshots heredado. La campaña beta.1 reejecuta esas invariantes con el nombre/ID final `gravity_charge`.

## Revisión

No se añadió entidad nueva ni renderer propio: el recurso no altera la identidad física de `ShulkerBullet`. El mixin de captura está aislado al hook server-side de daño y no cambia comportamiento de impacto. El brewing viejo con shell queda rechazado explícitamente por GameTest.

## Gate

S01 cerrado. S02 puede crear la misma `EntityTypes.SHULKER_BULLET` como Gravity Charge lanzada y añadir adquisición/readquisición sin reabrir la economía básica salvo regresión demostrable.
