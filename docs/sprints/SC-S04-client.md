# SC-S04 — Cliente y lenguaje visual

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN**.

## Scope

SC-002, lenguaje visual de SC-030/031 y evidencia visual de lanzamiento, vuelo y adquisición. Los assets 2D/modelos ya presentes en la rama son **definitivos** salvo que un commit posterior del usuario los sustituya o corrija manualmente.

## Investigación real

- Minecraft 26.2 permite seleccionar item model por `minecraft:display_context` desde `assets/<namespace>/items/<item>.json`.
- El GUI puede usar `clinging_reoriented:item/shulker_charge_gui` sin afectar al modelo en mano.
- `ShulkerBulletModel` vanilla está formado por tres cubos/placas ortogonales: 8×8×2, 2×8×8 y 8×2×8. Esta geometría es la referencia del modelo 3D en mano.
- La bullet en vuelo ya conserva el renderer vanilla porque el EntityType sigue siendo `minecraft:shulker_bullet`; no se reemplaza su renderer.
- No se añade HUD de lock ni homing curvo.

## Plan

1. Añadir item definition con `minecraft:select` + `minecraft:display_context`.
2. `gui` usa el icono/modelo 2D definitivo existente.
3. Primera/tercera persona y fallback usan un modelo 3D de tres placas ortogonales equivalente a la geometría vanilla de la bullet; ground/fixed también pueden conservar la representación 3D.
4. Referenciar recursos vanilla donde corresponda en vez de redistribuir texturas de Mojang.
5. Añadir Client GameTest específico y snapshots estables para GUI, primera persona, tercera persona/proyectil cuando el harness lo permita.
6. Añadir asserts de existencia/resolución del item model; las capturas complementan, no sustituyen, los invariantes.
7. Revisar escala, orientación y legibilidad contra Wind/Fire Charge y contra la bullet en vuelo. Ajustes visuales reinician snapshots S04.

## Adversarial previo

- GUI no debe heredar el modelo 3D por fallback accidental.
- Mano izquierda/derecha no debe invertir una geometría asimétrica de forma rota.
- Primera persona no debe tapar el centro de pantalla de forma absurda.
- La bullet lanzada debe seguir usando su renderer vanilla, no el item model.
- Resource reload no debe producir missing model/texture.
- First Person/FA no deben alterar el item model de forma incompatible.

Gate: client lane + snapshots S04 coherentes y CI completa verde.
