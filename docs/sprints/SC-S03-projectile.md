# SC-S03 — Navegación, impacto, redstone y recaptura

Estado: **REABIERTO / REVALIDACIÓN DE IMPACTOS AISLADOS**.

La batería real de 105 GameTests descubrió primero un bug de producción: el propio Target Block se trataba como obstáculo. Esa corrección ya está aplicada y los dos holdouts de Target Block pasan.

Los fallos restantes mezclaban impactos simples con navegación larga o estado del fixture. Se aíslan ahora:

- daño/Levitation: vaca inmóvil a corta distancia, lock inicial obligatorio;
- bloque ordinario: pared a corta distancia y vector de intención transformado a coordenadas mundo;
- duplicación: shulker abierto con IA normal (vanilla `teleportSomewhere()` devuelve false si `isNoAi()`), disparo cercano y secuencia impacto → daño → segundo shulker;
- los tests de routing largo quedan en los holdouts específicos de Target Block ya verdes.

No se modifica producción en esta iteración. Si alguno de estos impactos aislados sigue fallando, se clasifica como bug de implementación/arquitectura y no se amplía el timeout como sustituto de diagnóstico.
