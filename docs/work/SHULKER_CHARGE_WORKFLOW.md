# Shulker Charge — workflow iterativo TM

Estado: **NORMATIVO TEMPORAL** para `feature/shulker-charge`.

Cada sprint sigue: SCOPE → REAL-STATE RESEARCH → PLAN LOOP → ADVERSARIAL LOOP → IMPLEMENTATION → TEST LOOP → FAILURE CLASSIFICATION → REVIEW LOOP → NO-CHANGE GATE → EVIDENCE.

## Reglas duras

- `docs/work/SHULKER_CHARGE_SPEC.md` es la fuente normativa hasta S06.
- Los modelos/iconos de Shulker Charge presentes en la rama son los assets definitivos salvo sustitución/corrección posterior realizada por el usuario.
- No se inventa física paralela si vanilla `ShulkerBullet`, projectile ownership, `TargetBlock` o dispenser behaviour ya ofrecen el lenguaje necesario.
- Targeting, drops, consumo, daño y duplicación de shulkers son autoridad servidor.
- Una entidad/proyectil no puede producir más de un drop por carrera de eventos.
- No se falsifican CI ni snapshots; un fixture defectuoso se clasifica como fixture.
- Capturas visuales acompañan a asserts cuando proceda; una imagen bonita no sustituye una invariante.
- Todo fallo se clasifica explícitamente como implementación, plan, requisito, arquitectura, test/fixture o entorno antes de corregirse.

## Gate global

S00–S05 deben estar cerrados antes de S06. S06 migra comportamiento estable a README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION/CHANGELOG, retira `docs/work/*`, actualiza versión, valida rama exacta, integra, vuelve a validar `main` y publica prerelease desde artefacto exacto.
