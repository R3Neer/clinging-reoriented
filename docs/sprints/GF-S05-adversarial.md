# GF-S05 — Campaña adversarial visual y hardening

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN**.

## Tesis

Al cerrar S05 no aparecerá una mecánica nueva: las invariantes de S00–S04 seguirán siendo ciertas bajo secuencias combinadas, lifecycle hostil y compatibilidad real. La física continuará siendo server-authoritative y conservará momentum; la cámara seguirá perteneciendo al jugador; el cuerpo contará la trayectoria sin secuestrar la vista; el landing commitment seguirá siendo fail-closed; impacto, agua, Elytra, mounts/pets y respawn no podrán abrir exploits ni dejar ownership visual stale.

Criterio demostrable: la cabeza exacta de la rama debe superar la matriz adversarial completa, producir snapshots semánticos coherentes con asserts numéricos y completar una revisión de código seguida de una pasada completa sin cambios.

## Scope

S05 cruza requisitos ya implementados, en especial FR-GF-001..003, 010..013, 020..035, 040..063, 070..083 y NFR-GF-002..008. No cambia los timings 12/6 ni 180/240 ms, no añade steering, FOV, partículas, sonidos, keyframes ni integración concreta con Scale Brews.

## Baseline leído

Baseline de entrada: commit `e2b394710e50fa58253cbd69d55c927996832936`, run CI 427.

- servidor: 73/73 GameTests;
- client GameTests: verde con campaña S04 de snapshots;
- First Person 2.7.2 + Not Enough Animations: verde;
- Scale Brews optional server/client lanes: verdes;
- root corporal S04 validado visualmente después de mover el pivot desde pies al centro lógico del avatar;
- snapshots S04 revisados: sustained DOWN, curva EAST, zero hold, reverse WEST, landing begin/mid/final;
- el impacto arbitrary-normal necesita el fallback post-`Entity.move` porque el redirect gravity-relative de Gravity Changer no cubre toda colisión bloqueante cuando `onGround` no representa la normal real;
- el harness de impacto necesita jugador post-login realista porque el mock conectado permanece temporalmente invulnerable hasta `hasClientLoaded()`.

## Cobertura adversarial que ya existe

No se duplicará sin motivo:

- `HardeningAdversarialTests`: retirement local imposible/retry, revocación por foreign mob write, relocation tree atómica con passenger obstruction, causalidad one-shot de moving surface;
- `GravityPolishGameTests`: momentum/fall history en jugador, mob y mount; heading vertical; epoch visual a través de respawn;
- `ImpactGameTests` + kernel: high/low/tangential/multiaxis y ownership de daño;
- `LandingStateGameTests` + `LandingSurfaceApiGameTests`: predicción/commit/provider/fail-closed;
- `GravityFallStateGameTests`: START/LAND/RESUME/RESET y lifecycle server;
- `GravityFallClientGameTest`: blend, sustained, 90°, zero crossing, 180°, BODY_LANDING, RESUME y camera isolation;
- `Water*`, `AnimalGravityTests`, `FirstPersonChecks` y lanes CI cubren sus dominios individuales.

S05 debe atacar **interacciones entre esos dominios**, no simplemente volver a ejecutarlos con otro nombre.

## Matriz de ataques

| Ataque | Requisitos | Nivel principal | Observable obligatorio |
|---|---|---|---|
| secuencia `EAST→UP→NORTH→DOWN→WEST→UP` | 001, 002, 010–013, 040, 054–057 | server + client | momentum mundial continuo, camera forward continuo, body sólo responde a velocity |
| high speed + giro tardío + impacto arbitrary-normal | 070–079 | server | daño no borrado ni duplicado; tangencial sigue inocuo |
| predicción válida → bloque destruido → input durante cancel | 025–027, 033 | server + client | commitment se invalida, input bloqueado no se encola, no snap-back |
| velocity cardinal → zero jitter → inversión | 002, 056–057 | unit + client | hold estable, sin NaN/flip/twist espurio |
| Gravity Fall → Elytra | 062, 077 | server + client | root/landing se cancelan; Elytra recupera ownership sin daño doble |
| Gravity Fall → agua/natación | 063 | server + client | no sustained/pose falsa; ownership vuelve limpio |
| mount + passenger + pet durante cambios encadenados | 080–081 | server | jerarquía, momentum, heading y ownership sin regresión |
| death/respawn/teleport/dimension | 027, 078, 082 | server + client | no estado stale; epoch monotónico; UUID domina a entity id |
| remote tracking entra/sale en Gravity Fall | 082 | client/multiplayer fixture | snapshot discreto reconstruye el mismo body sin paquete por tick |
| First Person con root activo + landing | 083 | compat client | body puede rotar; camera forward permanece independiente |
| Fresh Animations/EMF target | 052–053, NFR-004 | compat client + snapshot | limbs siguen bajo FA; root macro permanece; cámara intacta |
| comparación Elytra / Falling / Gravity Fall | diseño + NFR-004/008 | snapshot + QA | lenguajes visuales distinguibles sin espectáculo añadido |

## Plan

- [ ] A1 inventariar cobertura exacta existente y convertir huecos reales en tests, sin duplicar observables ya demostrados.
- [ ] A2 añadir secuencia server de seis cambios cardinales verificando momentum exacto, airborne continuity y ownership.
- [ ] A3 añadir holdout de impacto tardío combinado con último giro y normal de colisión distinta de gravedad.
- [ ] A4 añadir invalidación de landing combinada con input durante commitment/cancel y demostrar que no existe queue diferida.
- [ ] A5 añadir lifecycle combinado Elytra/agua/respawn/teleport y comprobar RESET/ownership/epoch.
- [ ] A6 extender campaña client con secuencia multigiro, jitter de cero, invalidación y comparación visual Falling/Gravity Fall/Elytra.
- [ ] A7 validar mounts/pets/remote tracking en combinaciones que crucen los boundaries de ownership ya existentes.
- [ ] A8 identificar versiones exactas de Fresh Animations Player Extension + EMF/ETF del entorno objetivo y crear lane reproducible si existe build compatible con 26.2; si no existe, documentar bloqueo verificable en vez de falsear evidencia.
- [ ] A9 revisar artefactos/snapshots con matriz `ataque → requisito → nivel → resultado → evidencia`.
- [ ] A10 revisión completa de producción, tests, mixins y lifecycle; cualquier cambio reinicia revisión.
- [ ] A11 pasada CI completa sin cambios sobre la cabeza exacta.

## Modelo adversarial previo

- dos cambios legítimos dentro del mismo tick lógico/ventana de red;
- gravedad cambia pero velocity todavía conserva la dirección anterior;
- velocidad cae por debajo del epsilon y oscila alrededor de cero con componentes de ruido distintas por frame;
- landing prediction aparece el mismo tick que START o que un nuevo input;
- superficie desaparece después de publicar LAND pero antes de contacto;
- una invalidación llega cuando el SLERP corporal ya está casi terminado;
- stale packet con entity id correcto y UUID viejo después de respawn;
- tracker remoto empieza a observar entre START y LAND;
- Elytra comienza en BODY_LANDING parcial;
- agua se toca en el tick de entrada a sustained;
- mount cambia gravedad con rider y pet siguiendo a la vez;
- un impacto bloquea dos ejes y ocurre tras un giro de gravedad que deja `onGround=false`;
- First Person ve cuerpo rotado mientras cámara y selection look deben permanecer en el frame retenido;
- FA/EMF puede reordenar/modelar capas internas, pero no debe recibir ownership de la transformación macroscópica de Clinging;
- mutation mindset: quitar sequence fence, resetear airborne al girar, usar gravedad en vez de velocity para body, reactivar `fallDistance` como fuente primaria, aceptar input committed, aplicar root a la cámara.

## Holdout reservado

Después de implementar la matriz visible se revelará una secuencia compuesta no codificada previamente: caída sostenida con velocity EAST, cambios físicos `UP→NORTH→DOWN→WEST→UP` sin modificar manualmente momentum, cruce de velocidad por cero, aparición y destrucción de soporte durante BODY_LANDING, intento de input durante commitment y activación final de Elytra. Debe terminar sin queue diferida, sin snap-back, sin estado visual stale y con la cámara conservando su frame mundial salvo el landing válido que llegue a comprometerse.

## Regla de cierre

S05 sólo cierra con: tests aplicables verdes, snapshots revisados, holdout reservado superado, deuda externa explícita, revisión final sin cambios y una ejecución CI completa posterior que valide exactamente ese commit.
