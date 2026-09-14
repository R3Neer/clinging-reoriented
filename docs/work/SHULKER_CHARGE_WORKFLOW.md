# Shulker Charge — workflow iterativo TM

Estado: **NORMATIVO TEMPORAL** para `feature/shulker-charge`.

El trabajo se ejecuta por sprints cerrables. Cada sprint sigue obligatoriamente:

1. **SCOPE**: requisitos SC-* aplicables, exclusiones y observables.
2. **REAL-STATE RESEARCH**: leer código actual del mod, vanilla/Fabric y dependencias reales antes de diseñar.
3. **PLAN LOOP**: arquitectura mínima, ownership, lifecycle y fallos previsibles. Revisar el plan hasta que no dependa de suposiciones materiales.
4. **ADVERSARIAL LOOP**: intentar romper el plan antes de implementarlo: concurrencia, entidades eliminadas, chunks, dimension/lifecycle, PvP, dispenser, Target Block y dobles eventos.
5. **IMPLEMENTATION**: cambios pequeños, server-authoritative y sin ampliar scope por conveniencia.
6. **TEST LOOP**: unit/kernel → server GameTest → client GameTest → compat lanes. Añadir snapshots cuando el observable sea visual o temporal.
7. **FAILURE CLASSIFICATION**: todo fallo vuelve explícitamente a una categoría: implementación, plan, requisito, arquitectura, test/fixture o entorno. No se parchea a ciegas.
8. **REVIEW LOOP**: revisar producción, tests, mixins/registries/lifecycle y efectos cruzados. Cualquier cambio reinicia la revisión pertinente.
9. **NO-CHANGE GATE**: pasada completa aplicable sin cambios sobre el HEAD exacto que se declara como evidencia.
10. **EVIDENCE**: documentar SHA, CI, tests, snapshots y deuda explícita antes de cerrar el sprint.

## Reglas duras

- `docs/work/SHULKER_CHARGE_SPEC.md` es la fuente normativa de comportamiento hasta S06.
- No se incorpora textura 2D final hasta que el usuario la suministre o apruebe un asset con licencia inequívoca.
- No se inventa una física paralela si vanilla `ShulkerBullet`, projectile ownership, `TargetBlock` o dispenser behaviour ya ofrecen el lenguaje necesario.
- Targeting, drops, consumo, daño y duplicación de shulkers son autoridad servidor.
- Una sola entidad/proyectil no puede producir más de un drop por carrera de eventos.
- No se falsifican resultados de CI ni snapshots; un fixture defectuoso se clasifica como fixture.
- Las capturas visuales acompañan a asserts cuando sea posible; una imagen bonita no sustituye una invariante.

## Gate global

S00–S05 deben estar cerrados antes de S06. S06 migra comportamiento estable a README/GUIDE/ARCHITECTURE/COMPATIBILITY/VALIDATION/CHANGELOG, retira `docs/work/*`, actualiza versión si corresponde y sólo entonces integra/publica.