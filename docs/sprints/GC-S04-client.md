# GC-S04 — Cliente y lenguaje visual

Estado: **CERRADO / GATE VERDE**.

> Nota de canonización beta.1: el recurso se publica como **Gravity Charge** y todos los paths/snapshots finales usan `gravity_charge` / `gravity-charge`.

## Scope cerrado

GC-002 y el lenguaje visual de GC-030/031: icono GUI, presentación 3D en contextos no-GUI y conservación del renderer vanilla de `minecraft:shulker_bullet`. Los assets 2D/modelos presentes en la rama se mantienen como assets definitivos; esta fase no los rediseña.

## Implementación estable

- `assets/clinging_reoriented/items/gravity_charge.json` selecciona por `minecraft:display_context`: `gui` usa `gravity_charge_gui` y el resto cae en `gravity_charge_3d`.
- El modelo 3D conserva las tres placas ortogonales inspiradas en `ShulkerBulletModel` pero usa la **textura propia** `clinging_reoriented:item/gravity_charge`.
- La entidad lanzada sigue siendo exactamente `EntityTypes.SHULKER_BULLET`; su renderer sigue siendo vanilla y, por tanto, Minecraft conserva la propiedad de sus propios recursos de proyectil en runtime.
- No se añade HUD de lock ni homing curvo.

## Evidencia visual

La campaña histórica de S04 quedó verde en run #626 (`34881091571`). La corrección posterior `4e6e5660817062c868ce37cccf4debf02fc061c4` dejó el texture wiring final del item 3D en la textura propia del proyecto y el gate post-merge #714 (`34897063938`) lo revalidó.

Snapshots default finales de beta.1:
- `gravity-charge-inventory-icon`: GUI 2D.
- `gravity-charge-first-person-held`: contexto primera persona.
- `gravity-charge-third-person-held`: contexto tercera persona.
- `gravity-charge-projectile-renderer`: renderer vanilla de la bullet lanzada.
- `gravity-charge-fixed-3d`: evidencia aislada del fallback 3D mediante ItemFrame invisible.

Las capturas en mano no aíslan bien el objeto porque la skin/brazo ocupa gran parte del encuadre; se mantiene por ello el snapshot `FIXED`. El validador exige diferencias byte-a-byte entre presentaciones que deben ser visualmente distintas.

## Compatibilidad validada

- Cliente base.
- First Person 2.7.2 + Not Enough Animations 1.12.4.
- Scale Brews beta.5 client load.
- Fresh Animations 1.10.5 + Player Extension 1.1 + EMF 3.3.5 + ETF 7.2.

## Gate

- [x] GUI no hereda por accidente el fallback 3D.
- [x] Fallback 3D carga sin missing model/texture.
- [x] Primera/tercera persona resuelven sin romper el cliente.
- [x] La bullet lanzada conserva el renderer vanilla.
- [x] Evidencia 3D aislada y revisada.
- [x] Lanes First Person, Scale Brews y Fresh Animations verdes.
- [x] CI completa + validador de snapshots verdes.

Cualquier cambio posterior de arte/modelo reabre únicamente el gate visual correspondiente; el cierre actual no autoriza modificaciones cosméticas automáticas.
