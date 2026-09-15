# PERF-S07 — Adversarial, canonización y release beta.3

Estado: **CERRADO / BETA.3 PUBLICADA**.

## Entrada

La campaña de rendimiento se desarrolló sobre `0.1.0-beta.2` sin añadir features ni cambiar contratos de gameplay.

El intento PERF-S01 de interceptar una llamada introducida por otro mixin quedó rojo en run **#822** y se descartó. La implementación final conserva el ownership original del limitador de caída con contexto reutilizable, sin ocultar el fallo ni rebajar el gate.

HEAD de código cerrado: `be04f41ebe1289127837eac4e03b867e9d6e6db3`.

Run **#831** (`35008292945`) quedó completamente verde: localización, JUnit, server, default client, First Person, Scale Brews server/client, Fresh Animations y snapshots.

## Invariantes de beta.3

- No cambian controles, física, targeting, tiempos visuales ni autoridad servidor/cliente.
- Gravity Charge mantiene alcance, cono, prioridad, LOS, fan y cadencia.
- La cámara beta.2 mantiene sus invariantes CAM de handedness, polos, 360°, 1ª↔3ª persona, body separation y salida vanilla-compatible.
- La recuperación mantiene los 2.108 offsets legacy y su orden; sólo se reparte el trabajo en lotes de 64.
- Scale Brews sigue siendo integración opcional, fail-closed y fuera del compile classpath de producción.
- First Person y Fresh Animations conservan ownership y snapshots obligatorios.

## Canonización y gates

- **Release-prep:** `4dfd3ed64d55b0ab55fef6b302cf1b98dbe2a316` fijó `0.1.0-beta.3`, CHANGELOG, VALIDATION y el publisher exact-artifact.
- **Run #832** (`35009515014`): matriz completa verde sobre ese HEAD exacto de release-prep.
- **PR #29** integró la campaña en `main`; merge `d45511e0fa8829c8bc451b5f3db3a652f13f3d9f`. El merge conserva el mismo tree de release-prep.
- **Main run #834** (`35010672290`): matriz completa verde de nuevo sobre el commit exacto de release.
- **Publish beta.3 prerelease #1** (`35011714128`): success. Descargó los JAR del artefacto de #834 y publicó sin recompilar.
- **Publish beta.3 prerelease #2** (`35011719523`): skipped por el gate one-shot una vez existente la release.
- **Release/tag:** `v0.1.0-beta.3`, target `d45511e0fa8829c8bc451b5f3db3a652f13f3d9f`.
- **Regular JAR SHA-256:** `675b7edbc8579cbfa471e6a372afceab5d23306ed50caf356e8de315b944757d`.
- **Sources JAR SHA-256:** `ce04d04d940461f6e16a2136927b610c4a6c64eba14936bef1bc07e4820dcf4b`.

## Resultado público

Beta.3 comunica mejoras de rendimiento y estabilidad en términos observables para el jugador:

- menor trabajo de fondo en mundos con muchas entidades;
- menor presión de memoria temporal durante render y cámara de Gravity Fall, favoreciendo tiempos de frame más consistentes en clientes y modpacks pesados;
- recuperación de gravedad rara repartida entre ticks para evitar concentrar toda la búsqueda de colocación segura en un único tick;
- menos trabajo repetido en landing, colisiones y superficies móviles;
- menor overhead de las rutas opcionales de Scale Brews;
- menor trabajo temporal durante la reacquisición de Gravity Charge sin modificar sus reglas de targeting.

No se publica ningún porcentaje de mejora no medido ni se afirma que beta.3 elimine por sí sola el congelón de varios segundos observado en un modpack grande, porque ese síntoma no se aisló a este mod.

## Cierre

PERF-S07 queda cerrado. `v0.1.0-beta.3` permanece fijada al commit exacto validado `d45511e0fa8829c8bc451b5f3db3a652f13f3d9f`; el presente cierre documental es posterior a la publicación y no modifica el artefacto liberado.
