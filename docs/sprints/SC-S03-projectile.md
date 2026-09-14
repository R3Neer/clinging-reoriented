# SC-S03 — Navegación, impacto, redstone y recaptura

Estado: **CERRADO / GATE VERDE**.

## Scope cerrado

SC-S03 cubre navegación cardinal de la Shulker Charge lanzada, impactos físicos, Target Block/redstone, daño y Levitation vanilla, recaptura y compatibilidad con la duplicación vanilla de shulkers.

## Resultado

La implementación de producción quedó estable. La última campaña de revalidación no necesitó nuevos cambios funcionales: los fallos restantes estaban en fixtures físicos y nondeterminismo del harness.

Correcciones de fixture relevantes:
- los escenarios de bloque usan coordenadas mundo absolutas una sola vez, evitando mezclar transformaciones relativas de GameTest con navegación cardinal en mundo;
- los corredores de proyectil mantienen sus chunks en nivel `ENTITY_TICKING` durante la prueba;
- la RNG de routing de la `ShulkerBullet` se fija sólo en GameTests para que una misma ruta no cambie entre JVMs;
- el jugador con shield queda inmóvil y sin gravedad;
- el shulker de duplicación se mantiene físicamente anclado sobre soporte sólido hasta el impacto. El antiguo fixture flotaba, ejecutaba un teleport ambiental antes de recibir la Charge y llegaba cerrado a `hitByShulkerBullet()`, produciendo un falso negativo.

## Evidencia

- HEAD funcional limpio: `986f1196751f54ad94bd3509eaa644b8a771ef58`.
- GitHub Actions run #624 (`34879371172`): build/JUnit, **105/105 GameTests servidor**, cliente base, First Person + Not Enough Animations, Scale Brews servidor/cliente, Fresh Animations y validación de snapshots, todo verde.
- El mismo conjunto de 105 GameTests volvió a pasar sobre `f21985c7b20319dedea874f5a8d8339eb03dbe42` en run #626, después de añadir únicamente evidencia visual S04.

## Gate

- [x] Impacto físico contra Target Block recto y con ruta ortogonal.
- [x] Bloque ordinario se consume sin devolver item.
- [x] Entidad recibe daño vanilla y Levitation.
- [x] Shield neutraliza sin drop.
- [x] Recaptura lanzada no duplica item.
- [x] Duplicación vanilla de shulker alcanza creación/alta real de la nueva entidad.
- [x] 105/105 GameTests completos verdes.
- [x] Pase completo sin cambios de producción.

Deuda trasladada: carreras combinadas, lifecycle y stress/concurrencia pertenecen a SC-S05, no reabren SC-S03 salvo regresión real.
