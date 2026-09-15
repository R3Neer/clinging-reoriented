# Gamefeel pendiente — transición de Gravity Fall a landing

Estado: **ANÁLISIS / APARCADO para campaña posterior al planificador gravitatorio de mobs**.

Este documento conserva el fallo observado en playtest y el diagnóstico del diseño actual. No prescribe estructura de implementación.

## 1. Síntoma

El giro de aproximadamente medio segundo se siente bien cuando una reorientación/enlazamiento conduce a una pared cercana. Sin embargo, al pasar de Gravity Fall/free fall a un landing real tras una caída, la transición visual puede sentirse muy brusca o prácticamente instantánea. En ocasiones parece que el sistema descubre el aterrizaje cuando el impacto ya está ocurriendo o incluso después de haberse producido perceptualmente.

La sensación deseada es que cuerpo y cámara **anticipen un landing que ya es físicamente predecible** y completen la rotación suavemente al llegar al soporte, en lugar de reaccionar al choque.

## 2. Qué hace actualmente el diseño

- La presentación de landing comparte una ventana nominal de 10 ticks / 500 ms.
- La predicción de landing tiene exactamente ese mismo horizonte máximo: 10 ticks.
- Cada tick se proyecta el volumen actual con la velocidad prevista y se busca el primer contacto geométrico.
- Si el primer contacto encontrado no es soporte válido para la gravedad actual, la predicción se abandona para ese horizonte.
- Gravity Fall entra en fase LAND cuando aparece un candidato dentro de esa ventana.
- El cliente usa el ETA recibido como **duración total** de la interpolación hacia el frame de landing; si el ETA llega muy pequeño, la rotación es necesariamente muy corta.
- La detección/autorización de la fase se produce en la actualización lógica del servidor y después se comunica al cliente.

## 3. Diagnóstico: por qué puede entrar tarde

### 3.1 Horizonte de adquisición y ventana visual están acoplados

Usar los mismos 10 ticks para “ser capaz de descubrir el futuro landing” y para “tener 10 ticks completos para animarlo” deja cero margen para incertidumbre, actualización discreta o transporte de estado.

Para disfrutar visualmente de 500 ms de transición, el sistema debería conocer con antelación suficiente que **dentro de aproximadamente 500 ms** comenzará la ventana visual; no empezar a buscar por primera vez justo en el borde de esa misma ventana.

### 3.2 La predicción no modela toda la física real de Gravity Fall

La previsión actual aproxima gravedad y drag aéreo base, pero Gravity Fall tiene además dinámica aerodinámica propia que puede modificar la velocidad durante la caída. Si la trayectoria real cambia por postura/steering/drag mientras el predictor extrapola otro movimiento, el candidato puede aparecer, desaparecer o detectarse mucho más tarde de lo deseado.

Este problema será todavía más importante tras el rediseño aerodinámico pendiente, donde la postura corporal tendrá influencia continua sobre la trayectoria.

Principio requerido: **la predicción de landing y la física que realmente moverá al jugador deben compartir el mismo modelo conceptual de movimiento futuro**.

### 3.3 Primer contacto no-support cancela la previsión

En terreno irregular, una arista, lateral de bloque u otro contacto previo puede ser el primer impacto del barrido. Si ese contacto no es soporte válido, la predicción actual no continúa razonando sobre un landing posterior.

Eso es correcto para no llamar “landing” a un choque lateral, pero puede explicar casos donde la cámara sólo consigue comprometerse después de que la colisión haya alterado ya la trayectoria.

Hay que distinguir:

- **landing limpio predecible**;
- **contacto lateral/glancing antes del landing**;
- **impacto que invalida realmente la trayectoria prevista**.

No deben colapsarse en un simple “primer hit no es suelo → no sé nada del futuro”.

### 3.4 Detección autoritativa posterior al paso físico

La fase se decide en actualización lógica de servidor. Cuando un landing sólo se vuelve reconocible en el último instante, el paso físico que lo aproxima/produce ya ha sucedido antes de que el cliente reciba la nueva fase visual.

Esto no explica por sí solo un fallo de medio segundo si la predicción fuese temprana y estable, pero agrava mucho cualquier detección tardía.

### 3.5 La animación tiene duración ETA, sin colchón visual

Una vez recibido LAND, el cliente no “recupera” una ventana perdida: si el ETA es 0.4 ticks, dispone de 0.4 ticks para llegar al target. Por tanto la brusquedad observada es coherente con una predicción tardía.

La interpolación en sí no parece ser el problema principal; cuando dispone de tiempo suficiente, el mismo lenguaje de rotación funciona bien.

## 4. Rediseño conceptual deseado

### 4.1 Separar adquisición de landing y presentación

Debe haber dos horizontes distintos:

1. **horizonte de adquisición/seguimiento**, mayor y destinado a descubrir con anticipación un posible soporte;
2. **ventana de presentación**, los ~10 ticks que queremos dedicar al giro visible final.

El sistema puede seguir un candidato antes de comenzar la animación. Cuando su ETA atraviese la frontera de presentación, la cámara ya conoce un landing estable en vez de descubrirlo por primera vez.

No se fija todavía una cifra definitiva para el horizonte largo; debe salir de tests de velocidad, coste y estabilidad.

### 4.2 Seguimiento de candidato con confianza e histéresis

Un landing futuro no debería existir sólo como resultado binario independiente de cada tick.

Mientras el contacto previsto permanezca coherente durante varios samples, aumenta la confianza en él. Pequeñas variaciones del punto de impacto sobre la misma superficie no deben reiniciar la decisión visual.

Si el candidato cambia de verdad, la presentación puede reajustarse/cancelarse según cuánto falte para impactar, pero sin flicker entre LAND y SUSTAIN.

### 4.3 Predicción coherente con aerodinámica real

El futuro estado de velocidad usado para landing debe incorporar las mismas causas que el movimiento real relevante:

- gravedad efectiva;
- drag;
- postura/aerodinámica aceptada;
- steering que siga estando permitido;
- efectos que alteren caída;
- otros factores de movimiento que materialmente cambien el ETA.

La predicción no tiene que adivinar inputs futuros arbitrarios. Puede proyectar la intención/postura actual con una hipótesis conservadora y actualizar cada tick, pero no debe usar un modelo físico diferente al que ya está ocurriendo.

### 4.4 El cuerpo/cámara deben converger **al** touchdown, no empezar en él

Objetivo perceptual:

- a distancia: Gravity Fall normal;
- al entrar en una ventana de landing estable: empieza gradualmente la convergencia al frame de soporte;
- en touchdown: la cámara ya está prácticamente alineada;
- después: el estado grounded continúa el mismo frame sin snap.

### 4.5 Contactos imprevistos

Si aparece un obstáculo o el primer contacto real difiere de la predicción:

- un roce lateral no se convierte artificialmente en landing;
- la presentación debe responder al nuevo estado físico sin teleport visual;
- si tras el golpe emerge inmediatamente un soporte real, la transición restante puede ser más corta porque el mundo realmente ha cambiado, pero debe evitar un cambio discontinuo de orientación cuando exista una continuación visual razonable.

## 5. Hipótesis priorizadas para investigar cuando se implemente

1. **Acoplamiento horizonte=duración visual**: defecto estructural claro.
2. **Modelo predictivo distinto de la física aerodinámica real**: probable fuente de candidatos tardíos/inestables durante Gravity Fall.
3. **Primer contacto no-support corta toda la predicción**: probable en terreno irregular y aristas.
4. **Latencia lógica/red posterior a física**: amplificador de cualquier detección tardía.
5. **Interpolación cliente**: menor sospechoso, porque el mismo slerp se ve bien cuando recibe tiempo suficiente.

## 6. Escenarios mínimos de playtest futuro

- caída vertical larga a suelo plano a velocidades baja, media y alta;
- caída lateral equivalente hacia pared con gravedad lateral;
- caída con postura aerodinámica estable;
- caída con cambios graduales de postura/steering;
- plataforma estrecha vs. superficie amplia;
- arista/lateral de bloque antes de un suelo válido;
- obstáculo añadido poco antes del touchdown;
- superficie móvil compatible;
- lag/latencia multijugador representativa;
- cámara en primera y tercera persona, con y sin First Person/Fresh Animations.

Métrica perceptual principal: **cuando el landing era predecible con suficiente antelación, el jugador no debe sentir que la cámara gira como reacción al impacto**.