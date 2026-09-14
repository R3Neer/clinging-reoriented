# SC-S03 — Navegación, impacto, redstone y recaptura

Estado: **REABIERTO / FIXTURES FÍSICOS EN COORDENADAS MUNDO**.

La batería real de 105 GameTests ya demuestra que daño/Levitation y duplicación vanilla funcionan. Los cuatro fallos restantes de #503/#88264e1 son Target Block recto/desalineado, bloque ordinario y shield.

Diagnóstico:
- el bloque ordinario muestra movimiento estable a `0.15` durante 82 ticks, por lo que el proyectil no está inmóvil;
- los fixtures de bloque combinaban transformaciones relativas de GameTest con navegación cardinal en mundo;
- el jugador del shield fixture tenía el suelo retirado y no estaba fijado contra gravedad.

Corrección de fixture:
- Target Blocks y pared se colocan y validan mediante `ServerLevel` en coordenadas mundo absolutas derivadas una sola vez de `absolutePos`;
- el projectile se crea directamente en coordenadas mundo con intención hacia el centro físico del bloque;
- el test de pared exige explícitamente que no haya autoaim target;
- el jugador con shield queda `noGravity`, inmóvil y con lock explícito.

No se modifica producción en esta iteración. Si un bloque absoluto situado dos metros delante sigue sin registrar impacto, se reclasificará como bug de colisión/arquitectura.
