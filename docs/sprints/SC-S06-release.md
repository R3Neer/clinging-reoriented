# SC-S06 — Canonización, integración y release

Estado: **BLOQUEADO HASTA GATE ADVERSARIAL POST-MERGE**.

`v0.1.0-alpha.14` fue publicado desde `main` durante esta campaña con cambios de Gravity Fall, fluidos, seguridad y mace. Shulker Charge pasa por tanto a **0.1.0-alpha.15** y debe validarse sobre la combinación real con ese `main`, no sobre la antigua base alpha.13.

Migrar comportamiento estable a documentación canónica, registrar licencia/origen de cualquier asset que finalmente se incorpore, retirar docs temporales, versionar, validar rama, integrar a main, repetir CI y publicar prerelease desde el artefacto exacto validado.

Los assets 2D/3D de Shulker Charge presentes en la rama están ya considerados definitivos. El modelo 3D referencia en runtime `minecraft:entity/shulker/spark` sin redistribuir esa textura de Mojang.
