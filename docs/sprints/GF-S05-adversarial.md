# GF-S05 — Campaña adversarial visual y hardening

Estado: **CERRADO / GATE VERDE**.

## Tesis

S05 no añade una mecánica nueva: intenta romper en combinación las invariantes construidas por S00–S04. En el cierre exacto de esta campaña la física sigue siendo server-authoritative y conserva momentum; la cámara sigue perteneciendo al jugador; el cuerpo cuenta la trayectoria sin secuestrar la vista; el landing commitment sigue siendo fail-closed; e impacto, agua, Elytra, mounts/pets, tracking, respawn y teleport no dejan ownership visual stale ni abren una cola diferida de input.

El cierre se demuestra mediante GameTests combinados, client GameTests con asserts numéricos, lanes reales de compatibilidad, snapshots semánticos y una revisión completa de producción seguida de CI verde sobre el HEAD exacto de evidencia.

## Scope

S05 cruza requisitos ya implementados, en especial FR-GF-001..003, 010..013, 020..035, 040..063, 070..083 y NFR-GF-002..008. No cambia los timings 12/6 ni 180/240 ms, no añade steering, FOV, partículas, sonidos, keyframes ni integración concreta con Scale Brews.

## Baseline y cabeza de cierre

Baseline de entrada: `e2b394710e50fa58253cbd69d55c927996832936`, run CI 427, cierre lógico/visual de S04.

Cabeza de evidencia S05: `a074ff1a1c6f40bef2e23048c420dc9ab1751ec0`, run CI **464** (`34838805270`).

La run 464 pasó:

- build + JUnit;
- **86/86** server GameTests;
- client GameTests sin mods opcionales;
- First Person 2.7.2 + Not Enough Animations 1.12.4;
- Scale Brews beta.5, servidor y cliente;
- Fresh Animations v1.10.5 + FA+Player v1.1 + EMF 3.3.5 + ETF 7.2;
- validación automática de la matriz de snapshots;
- upload de JARs, logs, XML, reportes, snapshots por lane y manifiesto SHA-256.

## Qué se endureció durante S05

La campaña descubrió y corrigió, entre otros, estos límites reales:

- entrada en agua/lava debía soltar inmediatamente el root de Gravity Fall;
- pérdida de ownership físico debía liberar también una cámara retenida por Clinging;
- teleport debía limpiar landing/presentación transitoria antes del cambio de mundo/posición;
- respawn/replacement debía conservar monotonía de epoch sin aceptar identidad visual stale;
- retracking remoto necesitaba un epoch fresco y sólo debía replicar ownership visual efectivo;
- mounts y pets debían mantener ownership independiente al reproducir breadcrumbs del owner montado;
- los fixtures adversariales de pets debían atravesar `FollowOwnerGoal` real sin invalidar artificialmente su trail.

Estos cambios quedaron acompañados por tests antes del cierre.

## Matriz de ataques y evidencia

| Ataque | Resultado de cierre | Evidencia principal |
|---|---|---|
| `EAST→UP→NORTH→DOWN→WEST→UP` | momentum físico no se rota; cámara mundial permanece retenida; body sólo responde a velocity | server holdouts + `s05-six-turn-retained-camera` |
| high speed + giro tardío + impacto arbitrary-normal + salida de ownership | el impacto sigue cobrando una sola vez según componente realmente absorbida | `ImpactGameTests.actualLateTurnAndOwnershipExitCannotEraseArbitraryNormalImpact` |
| landing válido → invalidación/cancel | beta.6 parte del quaternion parcial sin snap y recupera el HOLD previo en 200 ms; input committed no se encola | `GameFeelAdversarialGameTests` + `s05-cancelled-landing-recovers-hold` |
| velocity cardinal → jitter sub-epsilon → inversión | zero-speed hold estable, sin NaN/flip/twist espurio | `s05-gravity-fall-zero-jitter` + kernel/body tests |
| gravedad cambia antes que velocity | el body no sigue gravedad instantáneamente; espera a que cambie la trayectoria real | `s05-gravity-fall-velocity-not-gravity` |
| Gravity Fall → Elytra | root/landing se liberan y Elytra conserva ownership | lifecycle/client tests + `s05-language-elytra` |
| Gravity Fall → agua/lava | presentación sostenida se libera inmediatamente, sin pose Falling falsa | water/lifecycle server+client tests |
| mount + rider + pet | préstamo al mount y ownership propio del pet no se mezclan; breadcrumbs reales se reproducen | `GameFeelAdversarialGameTests` |
| respawn / teleport | no queda state stale; epochs/UUID protegen contra paquetes de instancia anterior | `GameFeelLifecycleGameTests` |
| remote retracking | snapshot discreto reconstruye sólo ownership visual efectivo con epoch fresco | tracking holdouts S05 |
| First Person + Gravity Fall + BODY_LANDING | el cuerpo puede rotar y aterrizar; camera forward permanece independiente | `firstperson-gravity-fall-root`, `firstperson-gravity-fall-landing` |
| Fresh Animations/EMF/ETF | FA mantiene limbs/microanimación mientras Clinging conserva el root macroscópico | lane fijada + snapshots FA sustained/landing |
| Falling / Gravity Fall / Elytra | lenguajes visuales distinguibles sin añadir espectáculo artificial | snapshots `s05-language-falling`, `s05-language-elytra` y campaña Gravity Fall |

## Snapshot matrix de cierre

La CI conserva por separado las matrices `default`, `first_person` y `fresh_animations`. Un validador stdlib comprueba presencia única, firma PNG, resolución mínima, tamaño plausible y diferencias byte a byte en checkpoints que deben ser visualmente distintos. Run 464 produjo el manifiesto:

- default: six-turn retained camera, zero-jitter, velocity-not-gravity, cancelled landing recovery, Falling, Elytra;
- First Person: Gravity Fall root, BODY_LANDING;
- Fresh Animations: sustained DOWN, landing mid, landing final.

Todos los checkpoints del manifiesto son **854×480**. La revisión visual de los artefactos exactos confirmó:

- cancelación de landing desde un frame intermedio real, sin snap, seguida de recuperación al HOLD previo;
- Falling y Elytra claramente diferentes;
- First Person conserva framing/cámara mientras el cuerpo entra en landing;
- Fresh Animations conserva animación interna mientras el root corporal progresa de sustained a landing mid y al frame final.

## Incidencia adversarial durante el cierre

El primer intento del nuevo holdout de impacto tardío, commit `906df3c85b25cbc1d9f0f8025ab082efa8a4c3dd`, falló en CI 462. La traza demostró que **no era un bug de producción**: tras el giro real DOWN→EAST el AABB rotado consumía parte del desplazamiento y la componente absorbida quedaba en `0.58`, equivalente a una caída vanilla de aproximadamente `2.75` bloques, correctamente por debajo del umbral de daño.

Se corrigió sólo el fixture en `c7a3cf78e1865dc2b2266e7f216e67bb5568403f` para solicitar velocidad suficiente (`-2.0 Y`) y garantizar que el componente bloqueado permaneciese dañino después de la rotación. La run 463 pasó toda la CI. No se alteró producción para satisfacer el test.

## Plan de S05 — cerrado

- [x] A1 inventariar cobertura existente y convertir sólo huecos reales en tests.
- [x] A2 secuencia server/client de seis cambios cardinales verificando momentum, airborne continuity y ownership.
- [x] A3 holdout de impacto tardío combinado con giro real, arbitrary-normal y salida de ownership.
- [x] A4 invalidación/cancel de landing + input committed sin queue diferida ni snap-back. Beta.6 sustituye el hold parcial indefinido por recuperación acotada al HOLD previo.
- [x] A5 lifecycle combinado Elytra/agua/respawn/teleport con RESET/ownership/epoch.
- [x] A6 campaña cliente multigiro, jitter de cero, invalidación y comparación Falling/Gravity Fall/Elytra.
- [x] A7 mounts/pets/remote tracking cruzando boundaries de ownership.
- [x] A8 lane reproducible Fresh Animations Player Extension + EMF/ETF del entorno 26.2.
- [x] A9 revisión de artefactos + manifiesto `ataque → requisito → nivel → resultado → evidencia`.
- [x] A10 revisión completa de producción, tests, mixins y lifecycle; la pasada final no exigió cambios de producción.
- [x] A11 CI completa sin cambios de producción sobre la cabeza exacta de evidencia: run 464 verde.

## Holdout reservado — resultado

La campaña reservada combinaba caída sostenida, cambios físicos múltiples sin rotar momentum, cruce por cero, aparición/invalidez de landing, input durante commitment y salida final hacia Elytra. Sus observables quedaron repartidos deliberadamente entre holdouts server/client para poder localizar fallos sin convertir un único test en una novela rusa: autoridad/momentum, zero hold, landing cancel/no queue y Elytra ownership pasan de forma independiente y conjunta en la matriz acumulada. No queda queue diferida, snap-back ni ownership visual stale.

## Deuda fuera de scope

- La integración concreta de superficies de Scale Brews sigue perteneciendo al adaptador/consumer, no a esta API.
- El comfort tuning humano de 12/6 y 180/240 ms puede seguir refinándose tras playtesting, pero no se altera sin evidencia.
- Multiplayer con latencia real y packs externos no fijados sigue siendo QA de integración, no una garantía universal.

## Regla de cierre

Cumplida: tests aplicables verdes, snapshots validados y revisados, holdouts superados, deuda externa explícita, revisión final sin cambios de producción y ejecución CI completa posterior sobre el HEAD de evidencia.


## Nota posterior beta.6

GF-S05 queda como evidencia histórica de la arquitectura que introdujo cancelación sin snap. La política vigente desde beta.6 refina ese contrato: el frame parcial sigue siendo el punto inicial exacto de la cancelación, pero ya no queda congelado; `RECOVER_HOLD` vuelve al frame de vuelo retenido en 4 ticks / 200 ms. El snapshot canónico pasa a `s05-cancelled-landing-recovers-hold`.
