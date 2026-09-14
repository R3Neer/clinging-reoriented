# GF-S00 — Foundation audit y seam extensible

Estado: **CERRADO / GATE VERDE**.

## Tesis

Al cerrar S00, Clinging tiene un mapa explícito del ownership actual y un contrato público mínimo para consultar soporte/sweep de futuros suelos sin depender de Scale Brews, demostrado por tests de provider base/fail-closed y por CI, sin cambiar todavía el gamefeel.

## Scope

Incluye FR-GF-030..035 y preparación estructural para FR-GF-020..027. No incluye nuevo daño, cámara, pose, landing commitment ni cambios de input.

## Estado final

- `AirChanges.grounded` separa gates de estado de la consulta de superficies y conserva bridges legacy posteriores para no cambiar semántica de compatibilidad.
- `ClingingReoriented.findTurnPlacement/fits` sigue siendo owner exclusivo del preflight y placement de giro.
- `LandingSurfaceProvider` aporta sólo geometría: soporte actual, sweep opcional y revalidación.
- `LandingSurfaces` posee registry, validación, wrapping de identidad y selección determinista.
- La identidad pública es `provider + localId + revision`, ligada además al `Direction` gravitatorio observado.
- Vanilla existe como provider base.
- Providers externos no deciden gravedad, cámara, placement ni ownership.

## Checklist de cierre

- [x] I1 Reconstruir owners de soporte, preflight, visual, fallDistance, bridges y CI.
- [x] I2 Crear `api.LandingSurfaceProvider` con query cardinal, contacto local, sweep fraccional y revalidación explícita.
- [x] I3 Crear `api.LandingSurfaces`: registry por `Identifier`, vanilla builtin, wrapping de identidad, validación central, selección determinista y fail-closed.
- [x] I4 Implementar provider vanilla conservando la geometría de soporte anterior.
- [x] I5 Migrar bloque/providers registrados en `AirChanges.grounded`; mantener bridges Scale legacy como fallback.
- [x] I6 Añadir GameTests de provider externo, unregister, excepción, duplicate owner, vanilla floor y side contact.
- [x] I7 CI aplicable integrada en la campaña acumulativa posterior.
- [x] I8 Revelar holdout y segunda pasada adversarial.
- [x] I9 Revisión posterior absorbida por S01–S05 sin regresiones del contrato.

## Holdout revelado: stale contact tras cambio de gravedad

La primera implementación vinculaba el contacto al provider pero no al frame gravitatorio. Un provider permisivo podía revalidar el mismo token después de DOWN→EAST. Se corrigió centralmente: `Contact` conserva el `Direction` de creación y `LandingSurfaces.revalidate` rechaza un frame distinto antes de consultar al provider. El GameTest externo cubre explícitamente esta mutación.

## Evidencia acumulada

S00 quedó validado de forma transitiva por los sprints posteriores: S02 usa el contrato para predicción/commitment, S04 usa la misma predicción para BODY_LANDING y S05 cruza providers, lifecycle y compatibilidad. El HEAD actual de la rama conserva esos tests dentro de la CI completa.

## Exclusiones que se mantuvieron

- predictor temporal/ETA completo: owner S02;
- landing commitment: owner S02;
- payloads nuevos y cámara: owner S03;
- Gravity Fall: owner S04;
- nuevo daño de impacto: owner S01;
- adaptación concreta a `Scale Brews collision.api`: fuera de alcance de esta campaña.
