# SC-S03 — Navegación, impacto, redstone y recaptura

Estado: **REABIERTO / BUG DE ROUTING CORREGIDO, REVALIDACIÓN EN CURSO**.

La primera batería fiable de 105 GameTests expuso un gap de cobertura previo. Tras endurecer fixtures, #492 redujo los fallos a cuatro y aisló un defecto de producción real:

- el routing de Target Blocks reutilizaba la regla vanilla de evitar la siguiente celda sólida;
- como un Target Block es sólido, la Charge lo trataba como obstáculo justo antes de impactarlo;
- corrección: la celda sólida se evita salvo cuando es exactamente el `clinging$targetBlock` activo, en cuyo caso se permite la colisión física normal.

Los demás supervivientes se clasificaron como fixtures:
- el test ortogonal observaba `deltaMovement` en tick 0, antes del primer steering vanilla-like;
- el fixture de duplicación dejaba que la IA del shulker pudiera cerrarlo antes del impacto;
- el fixture de shield no garantizaba Survival/lock ni una geometría corta y determinista.

Ninguna semántica de usuario cambia respecto a la spec. El sprint vuelve a cerrado sólo cuando los 105 tests pasen con esta corrección.
