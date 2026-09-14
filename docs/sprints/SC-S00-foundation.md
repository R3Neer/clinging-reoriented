# SC-S00 — Investigación y arquitectura

Estado: **EN CURSO**.

Objetivo: entender el estado real de Minecraft/Fabric 26.2 y del mod antes de escribir gameplay. Cierra sólo con una arquitectura concreta de item, proyectil, captura, targeting, dispenser, Target Block y render.

Ataques previos: APIs cambiadas en 26.2; confundir `ShulkerBullet` natural con projectile relanzado; perder duplicación vanilla; depender de textura final; escanear bloques globalmente; no distinguir daño melee/flecha/escudo; target entity-id stale.

Evidencia requerida: archivos/clases reales auditados, decisiones de ownership y lifecycle, plan de test por cada frontera.