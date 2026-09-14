# SC-S04 — Cliente y lenguaje visual

Estado: **CERRADO / GATE VERDE**.

## Scope cerrado

SC-002 y el lenguaje visual de SC-030/031: icono GUI, presentación 3D en contextos no-GUI y conservación del renderer vanilla de `minecraft:shulker_bullet`. Los assets 2D/modelos presentes en la rama se mantienen como assets definitivos; esta fase no los rediseña.

## Implementación estable

- `assets/clinging_reoriented/items/shulker_charge.json` selecciona por `minecraft:display_context`: `gui` usa `shulker_charge_gui` y el resto cae en `shulker_charge_3d`.
- El modelo 3D conserva las tres placas ortogonales inspiradas en `ShulkerBulletModel` y referencia el recurso vanilla `minecraft:entity/shulker/spark` en vez de redistribuir texturas de Mojang.
- La entidad lanzada sigue siendo exactamente `EntityTypes.SHULKER_BULLET`; no existe renderer paralelo.
- No se añade HUD de lock ni homing curvo.

## Evidencia visual

Run #626 (`34881091571`), HEAD `f21985c7b20319dedea874f5a8d8339eb03dbe42`, gate completo verde. Artefacto `clinging-reoriented-build` id `10363342754`, digest `sha256:17cdedbf85446efbfc0bab2c84b1999395c8f58c99dbc82bf5035c384c469ed2`.

Snapshots default validados a 854×480:
- `shulker-charge-inventory-icon`: GUI 2D.
- `shulker-charge-first-person-held`: contexto primera persona.
- `shulker-charge-third-person-held`: contexto tercera persona.
- `shulker-charge-projectile-renderer`: renderer vanilla de la bullet lanzada.
- `shulker-charge-fixed-3d`: evidencia aislada del fallback 3D mediante ItemFrame invisible.

La revisión manual detectó que las capturas en mano no aíslan bien el objeto porque la skin/brazo del jugador ocupa gran parte del encuadre. Se clasificó como **gap de evidencia**, no como fallo del asset. Se añadió por ello el snapshot `FIXED`, sin modificar el arte ni el modelo definitivo. El snapshot aislado confirma que el fallback 3D resuelve y renderiza; también permanece estable bajo Fresh Animations. El validador exige además que sea byte-distinto del icono GUI y del renderer de proyectil.

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
- [x] Evidencia 3D aislada y revisada manualmente.
- [x] Lanes First Person, Scale Brews y Fresh Animations verdes.
- [x] CI completa + validador de snapshots verdes.

Cualquier cambio posterior de arte/modelo reabre únicamente el gate visual correspondiente; el cierre actual no autoriza modificaciones cosméticas automáticas.
