# PERF-S07 — Adversarial, canonización y release beta.3

Estado: **EN CURSO — RELEASE-PREP**.

## Entrada

La campaña de rendimiento se desarrolló sobre `0.1.0-beta.2` sin añadir features ni cambiar contratos de gameplay.

El intento PERF-S01 de interceptar una llamada introducida por otro mixin quedó rojo en run **#822** y se descartó. La implementación final conserva el ownership original del limitador de caída con contexto reutilizable, sin ocultar el fallo ni rebajar el gate.

HEAD de código cerrado: `be04f41ebe1289127837eac4e03b867e9d6e6db3`.

Run **#831** (`35008292945`) quedó completamente verde: localización, JUnit, server, default client, First Person, Scale Brews server/client, Fresh Animations y snapshots.

## Invariantes de beta.3

- No cambian controles, física, targeting, tiempos visuales ni autoridad servidor/cliente.
- Gravity Charge mantiene alcance, cono, prioridad, LOS, fan y cadencia.
- La cámara beta.2 mantiene exactamente sus invariantes CAM de handedness, polos, 360°, 1ª↔3ª persona, body separation y salida vanilla-compatible.
- La recuperación mantiene los 2.108 offsets legacy y su orden; sólo se reparte el trabajo en lotes de 64.
- Scale Brews sigue siendo integración opcional, fail-closed y fuera del compile classpath de producción.
- First Person y Fresh Animations conservan ownership y snapshots obligatorios.

## Canonización beta.3

Release-prep debe:

1. fijar `mod_version=0.1.0-beta.3`;
2. añadir CHANGELOG con lenguaje de usuario sobre rendimiento/estabilidad, sin porcentajes ni atribución no demostrada de freezes;
3. registrar la campaña en VALIDATION y mantener PERF-S00 como autoridad técnica;
4. instalar un publisher one-shot `release-beta3.yml` con política exact-artifact;
5. pasar de nuevo la matriz completa sobre el HEAD exacto de release-prep;
6. integrar a `main` sólo tras verde de rama;
7. repetir la matriz en el commit exacto de `main`;
8. publicar `v0.1.0-beta.3` desde los JAR de ese run, sin segunda compilación.

El cierre histórico de PERF-S07 sólo se escribe después de verificar tag, target commit, workflow y SHA-256 de ambos JAR.
