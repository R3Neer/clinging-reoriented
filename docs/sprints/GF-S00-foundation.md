# GF-S00 — Foundation audit y seam extensible

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN**.

## Tesis

Al cerrar S00, Clinging tendrá un mapa explícito del ownership actual y un contrato público mínimo para consultar soporte/sweep de futuros suelos sin depender de Scale Brews, demostrado por tests de provider base/fail-closed y por CI verde, sin cambiar todavía el gamefeel.

## Scope

Incluye FR-GF-030..035 y preparación estructural para FR-GF-020..027. No incluye nuevo daño, cámara, pose, landing commitment ni cambios de input.

## Estado actual reconstruido

### Soporte/grounded

`AirChanges.grounded` tiene actualmente cuatro responsabilidades mezcladas:

1. gates de estado (`onGround`, passenger, Elytra, Creative flight);
2. validación de movimiento respecto al feet-side de la gravedad;
3. soporte de bloques vanilla mediante `getBlockCollisions` + `FaceGeometry.touching`;
4. compatibilidad concreta mediante `AnatomyBridge.supported` y el fallback legacy de `ScaleBridge`.

El soporte de bloques es una política válida para convertirse en provider base. Los bridges Scale existentes siguen siendo compatibilidad legacy y no deben convertirse en la nueva API pública.

### Preflight de giro

`ClingingReoriented.findTurnPlacement/fits` es owner de la colocación segura al cambiar frame. Usa AABB de Gravity Changer, center-aligned fallback y `noCollision`; Anatomy/Scale añaden clearance opcional. Ningún surface provider nuevo podrá saltarse este preflight.

### Presentación

`ClingingReoriented.attempt` publica hoy `Payloads.visual(...)` inmediatamente antes de `writeTransition`. `VisualTransitions` captura el quaternion mostrado, aplica compensación/yaw gauge y hace SLERP a la gravedad objetivo. Éste es el acoplamiento que S03 separará; S00 no lo modifica.

### Caída

`write` y `writeTransition` resetean `fallDistance` tras cambios reales. `FallTrackerMixin` desactiva el límite direccional de Gravity Changer para gravedad poseída. S01 será owner del nuevo modelo de impacto; S00 no toca estos paths.

### Optional Scale

`AnatomyBridge` y `ScaleBridge` contienen integración reflectiva concreta. Scale Brews `chatgpt-editing` ya expone una API de contacto propia (`collision.api`), pero integrarla aquí violaría el scope. La nueva API de Clinging debe aceptar futuros adapters sin importar ningún tipo de Scale.

### Tests / snapshots

La CI ya ejecuta server GameTests, client GameTests, First Person y lanes Scale opcionales, y ya sube `build/run/clientGameTest/screenshots/*.png`. La infraestructura de screenshots existe; S03-S05 la ampliarán a checkpoints temporales intensivos.

## Estado objetivo de S00

Clinging expondrá un pequeño seam neutral:

- un `LandingSurfaceProvider` aporta contacto actual y sweep geométrico opcional;
- cada contacto tiene identidad opaca serializable-neutral (`localId` + `revision`) envuelta por el ID del provider;
- el core valida datos finitos, normal unitaria y fracciones;
- la registry decide de forma determinista y fail-closed;
- Vanilla se registra como provider base;
- `AirChanges.grounded` consulta primero ese seam para bloques/consumidores registrados;
- los bridges Scale actuales se conservan después como compatibilidad legacy, sin formar parte de la API.

La API no decide gravedad, cámara, placement ni ownership físico.

## Plan de implementación convergido

- [x] I1 Reconstruir owners de soporte, preflight, visual, fallDistance, bridges y CI.
- [ ] I2 Crear `api.LandingSurfaceProvider` con query cardinal, contacto local, sweep fraccional y revalidación explícita.
- [ ] I3 Crear `api.LandingSurfaces`: registry por `Identifier`, vanilla builtin, wrapping de identidad, validación central, selección determinista y fail-closed por provider.
- [ ] I4 Implementar provider vanilla conservando exactamente la geometría de soporte actual basada en `getBlockCollisions` + `FaceGeometry.touching`; la identidad será geométrica y su revalidación exigirá que el mismo contacto siga existiendo.
- [ ] I5 Migrar sólo la rama de soporte de bloque/registered providers de `AirChanges.grounded` a `LandingSurfaces.currentSupport`; mantener los bridges Scale legacy como fallback posterior.
- [ ] I6 Añadir tests GameTest para provider externo, cierre/unregister, excepción, dato inválido y no-regresión vanilla/side-contact.
- [ ] I7 Añadir tests puros de registro duplicado/validación si aportan señal sin necesitar runtime Minecraft.
- [ ] I8 Ejecutar CI completa y revisar artefactos/logs.
- [ ] I9 Revelar holdout y realizar segunda pasada adversarial.
- [ ] I10 Revisión final completa hasta pasada cero-cambios, registrar evidencia y cerrar.

## Revisiones del plan

### Requisitos

El plan cubre FR-GF-030..035 sin adelantar S02. `sweep` se expone ahora para no congelar una API que sólo pueda responder al soporte actual, pero S00 no implementa aún predictor/ETA.

### Ownership

Los providers sólo contestan geometría. Clinging conserva selection, preflight, gravedad, landing policy y posterior revalidación. Esto impide que un consumer externo convierta una superficie en suelo saltándose autoridad.

### Compatibilidad

No se elimina ni modifica la integración Scale existente. Un futuro adapter podrá registrar un provider desde su propio lado. Ausencia de providers externos equivale al comportamiento vanilla actual + fallbacks legacy existentes.

### Fail-closed

Una respuesta ausente, excepción, NaN/∞, normal inválida, fracción fuera de [0,1] o revalidación falsa se trata como ausencia de superficie para ese provider. No se transforma en `noCollision` ni en soporte vanilla por conveniencia.

### Simplicidad

No se introduce networking ni codec para contactos: el commitment dura pocos ticks y la identidad sólo necesita vivir server-side. El ID de provider + `localId` + `revision` basta para revalidar sin filtrar tipos externos.

### Verificabilidad

S00 cierra sólo si `AirChanges` conserva semántica vanilla y un provider de fixture puede aportar soporte sin dependencia externa, mientras providers rotos no lo consiguen.

## Modelo adversarial previo

- provider devuelve contacto con normal no finita/no unitaria;
- provider devuelve sweep con fracción negativa, >1 o NaN;
- provider lanza una excepción en query o revalidación;
- provider se desregistra entre query y revalidación;
- surface candidate deja de existir entre query y commit;
- dos providers reclaman superficies: el resultado debe ser determinista; sweep elige la menor fracción y desempata por ID;
- provider externo no está instalado;
- consulta externa intenta convertir ausencia en fallback inseguro;
- la API filtra un tipo interno de otro mod;
- provider provoca scan no acotado: la API no lo puede impedir internamente, pero no debe pedir scans ni exponer una API global; la query parte de entity/AABB locales;
- vanilla provider diverge del soporte actual;
- una API demasiado poderosa permite saltarse preflight/ownership;
- duplicate provider ID intenta reemplazar silenciosamente otro provider.

### Holdout reservado

Tras implementar se probará una combinación concreta de contacto externo válido, cambio de gravedad y contacto stale antes de revalidación. La identidad no debe poder reutilizarse bajo otro frame.

## Exclusiones explícitas

- predictor temporal/ETA completo;
- landing commitment;
- payloads nuevos;
- cámara;
- Gravity Fall;
- nuevo daño de impacto;
- adaptación a `Scale Brews collision.api`.
