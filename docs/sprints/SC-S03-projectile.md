# SC-S03 — Navegación, impacto, redstone y recaptura

Estado: **REABIERTO / REVALIDACIÓN FINAL DE FIXTURES**.

La batería #495 dejó sólo dos holdouts fallando tras la corrección real del Target Block:

- Target Block recto y desalineado: **verdes**; la corrección de no tratar el propio target como obstáculo funciona.
- Shield: **verde** con lock explícito y Survival.
- Entity damage: el fixture había eliminado el suelo bajo la vaca sin desactivar gravedad; el objetivo podía caer fuera de la trayectoria. Se fija `noGravity` y se afirma el lock inicial.
- Shulker duplication: se separa reproducción de navegación larga; el disparo empieza a ~2 bloques, el shulker queda abierto/sin IA y el test prueba en secuencia impacto físico → daño vanilla → segundo shulker.

Clasificación TM actual: un bug de producción ya corregido + fixtures físicos corregidos. No se cambia semántica de usuario.

Gate pendiente: 105/105 y CI completa verde.
