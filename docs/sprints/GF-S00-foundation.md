# GF-S00 — Foundation audit y seam extensible

Estado: **IMPLEMENTACIÓN / HOLDOUT REVELADO**.

## Tesis

Al cerrar S00, Clinging tendrá un mapa explícito del ownership actual y un contrato público mínimo para consultar soporte/sweep de futuros suelos sin depender de Scale Brews, demostrado por tests de provider base/fail-closed y por CI verde, sin cambiar todavía el gamefeel.

## Scope

Incluye FR-GF-030..035 y preparación estructural para FR-GF-020..027. No incluye nuevo daño, cámara, pose, landing commitment ni cambios de input.

## Estado actual reconstruido

- `AirChanges.grounded` mezclaba gates de estado, soporte vanilla y bridges Scale/Anatomy.
- `ClingingReoriented.findTurnPlacement/fits` sigue siendo owner exclusivo del preflight y placement de giro.
- `ClingingReoriented.attempt` publica hoy la transición visual junto al cambio físico; S03 separará ese acoplamiento.
- `write`/`writeTransition` resetean `fallDistance`; S01 será owner del cambio de daño.
- `AnatomyBridge`/`ScaleBridge` son integraciones reflectivas concretas existentes. No forman parte de la API nueva.
- La CI ya conserva screenshots de client GameTests y tiene lanes First Person/Scale opcionales.

## Estado objetivo / contrato elegido

- `LandingSurfaceProvider` sólo aporta geometría: soporte actual, sweep opcional y revalidación.
- `LandingSurfaces` posee registry, validación, wrapping de identidad y selección determinista.
- La identidad pública es `provider + localId + revision`, y el `Contact` queda además ligado al `Direction` gravitatorio en el que fue observado.
- Vanilla es provider base.
- Los bridges Scale actuales siguen como fallback legacy posterior para no cambiar semántica de compatibilidad durante S00.
- Providers externos no pueden decidir gravedad, cámara, placement ni ownership.

## Plan / checklist

- [x] I1 Reconstruir owners de soporte, preflight, visual, fallDistance, bridges y CI.
- [x] I2 Crear `api.LandingSurfaceProvider` con query cardinal, contacto local, sweep fraccional y revalidación explícita.
- [x] I3 Crear `api.LandingSurfaces`: registry por `Identifier`, vanilla builtin, wrapping de identidad, validación central, selección determinista y fail-closed.
- [x] I4 Implementar provider vanilla conservando la geometría de soporte actual.
- [x] I5 Migrar sólo bloque/providers registrados en `AirChanges.grounded`; mantener bridges Scale legacy como fallback.
- [x] I6 Añadir GameTests de provider externo, unregister, excepción, duplicate owner, vanilla floor y side contact.
- [ ] I7 CI completa.
- [x] I8 Revelar holdout y segunda pasada adversarial.
- [ ] I9 Revisión final cero-cambios, evidencia y cierre.

## Modelo adversarial revisado

Cubierto por contrato/validación:

- excepción de provider → fail-closed, warning once;
- duplicate ID → rechazo explícito;
- contacto no finito / normal no unitaria → constructor rechaza y registry captura como fallo de provider;
- sweep fraction NaN/fuera de [0,1] → rechazo;
- provider desaparece antes de revalidar → false;
- dos providers → orden estable por ID, earliest fraction para sweep;
- ausencia externa → vanilla + compat legacy existente;
- no hay tipos Scale en la API.

### Holdout revelado: stale contact tras cambio de gravedad

La primera implementación vinculaba el contacto al provider pero no al frame gravitatorio. Un provider permisivo podía revalidar el mismo token después de DOWN→EAST. Se corrigió centralmente: `Contact` conserva el `Direction` de creación y `LandingSurfaces.revalidate` rechaza un frame distinto antes de consultar al provider. El GameTest externo cubre explícitamente esta mutación.

## Exclusiones explícitas

- predictor temporal/ETA completo;
- landing commitment;
- payloads nuevos;
- cámara;
- Gravity Fall;
- nuevo daño de impacto;
- adaptación a `Scale Brews collision.api`.
