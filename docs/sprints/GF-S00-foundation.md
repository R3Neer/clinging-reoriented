# GF-S00 — Foundation audit y seam extensible

Estado: **EN CURSO**.

## Tesis

Al cerrar S00, Clinging tendrá un mapa explícito del ownership actual y un contrato público mínimo para consultar futuros suelos sin depender de Scale Brews, demostrado por tests de provider base/fail-closed y por una CI baseline verde, sin cambiar todavía gamefeel.

## Scope

Incluye FR-GF-030..035 y preparación estructural para FR-GF-020..027. No incluye aún nuevo daño, cámara, pose ni cambio de input.

## Investigación requerida

- paths que deciden grounded/support;
- current turn preflight;
- visual publication y `GravitySnapMixin`;
- fallDistance y fall hooks;
- AnatomyBridge/ScaleBridge actuales para evitar duplicar ownership;
- client tests y generación de screenshots;
- mecanismo de CI y fixtures opcionales.

## Modelo adversarial previo

- provider devuelve contacto no finito;
- provider falla/lanza o desaparece;
- surface candidate deja de existir entre query y commit;
- dos providers reclaman superficies distintas;
- provider externo no está instalado;
- consulta externa intenta convertir ausencia en fallback inseguro;
- la API filtra un tipo interno de otro mod;
- provider provoca scan no acotado por diseño;
- vanilla provider diverge del soporte actual;
- una API demasiado poderosa permite saltarse preflight/ownership.

### Holdout reservado

Se reserva una combinación concreta de provider stale + cambio de gravedad entre query y revalidación para la fase post-implementación.

## Checklist

- [ ] leer código afectado y documentar estado actual;
- [ ] fijar contrato API mínimo;
- [ ] implementar registry/provider vanilla;
- [ ] migrar sólo el consumidor necesario para usar la frontera sin cambiar semántica;
- [ ] tests unit/GameTest del contrato;
- [ ] ejecutar CI baseline;
- [ ] segunda pasada adversarial;
- [ ] revisión cero-cambios;
- [ ] registrar evidencia y cerrar.
