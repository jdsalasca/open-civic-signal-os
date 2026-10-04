# Plan de rondas

Estado real del repositorio y próximas rondas. El tablero operativo vive en
`docs/kanban.md` (lo refresca un bot) y el histórico en `docs/metrics-history.md`. Este archivo
es el plan de trabajo con criterios verificables.

## Estado

- **Tests:** 413 en `apps/api-java`, todos en verde con `mvn clean test`.
- **Calidad:** `npm run agent:preflight` en verde; parity OpenAPI 178 rutas / 147 documentadas /
  9 intencionalmente no documentadas.
- **Issues:** 2 abiertos, ambos de proceso (`#120` calidad de agentes, `#121` foco de sprint).
  Todos los issues de feature P1/P2 están cerrados.
- **Rama:** `develop`, sin worktrees ni ramas pendientes.

## Regla de calidad que este plan aplica

Una ronda cuenta solo si entrega valor verificable: código que corre, o un defecto con un test que lo
reproduce. Las rondas de refresco de documentación no cuentan.

Tres defectos del último tramo tuvieron la misma forma, y conviene tenerla escrita porque es el
patrón que más caro sale:

1. **Un test que afirma contra un valor que el camino de producción nunca produce no puede fallar.**
   Ocurrió tres veces: `MessagingConnectorTest` construía la entidad a mano con un token en claro que
   el service nuncaaba; `DigestSchedulerIT` leía una propiedad desde un `StandardEnvironment` nuevo
   sin las fuentes de la aplicación; `handleInternalError` descartaba la causa del error.
2. **Probar el service no es probar el endpoint.** El preview del digest se verificó a nivel de
   service y el controller llamaba a otro método. El test que lo cazó tiene que pasar por el
   controller.
3. **Verificar que un test falla cuando el código se rompe.** Un test que pasa con y sin el fix no
   está probando el fix. Las rondas 43 y 44 se comprobaron por mutación.
4. **Una copia por servicio es la misma clase de defecto que una fórmula duplicada.** La ronda 44
   encontró ocho definiciones de "resuelto". Dos copias comparadas por un test siguen siendo dos copias:
   es la lección de `PrioritizationFormula`, con un filtro en vez de aritmética. Cuando un concepto de
   dominio aparece en varios servicios, su dueño es el tipo de dominio, no el servicio.

## Rondas

| Ronda | Entregable | Criterio verificable | Estado |
| --- | --- | --- | --- |
| 39 | Pantalla de boletín semanal | 12 Playwright + 28 regresión Layout/nav | ✅ `68172c1` |
| 40 | Sellado de tokens de bot (AES-GCM) | 397 tests; el token llega intacto al proveedor vía API | ✅ `f058c45` |
| 41 | El digest preparado se guarda y se publica | 402 tests; publicar envía el artefacto sellado | ✅ `52126b1` |
| 42 | Namespace de ciudad y backlog federado | 412 tests; dos ciudades sin mezcla; revocar consentimiento corta el feed | ✅ `f035b5b` |
| 43 | El preview por API es el artefacto que se publica | 413 tests; falla por mutación del controller | ✅ `3ea5408` |
| 44 | Una sola definición de "resuelto" para todos los rankings | 420 tests; 8 copias → 1; falla por mutación | ✅ este commit |
| 45 | Auditar el par lectura/escritura de trust pulse y handoff institucional | Test por endpoint que compare GET y POST tras mutar el mundo | ➡️ siguiente |

## Próximas rondas candidatas

1. **Auditoría de la misma forma en otros pares lectura/escritura.** La ronda 43 encontró que un
   endpoint de lectura usaba un camino distinto al de escritura. El mismo patrón puede existir en
   `trust pulse`, `backlog publication` y `institutional handoff`: un GET que recompone lo que el
   POST selló. Es la ronda de mayor valor esperado porque el defecto ya se encontró una vez.
2. **`docs/plan.md` se mantiene al cierre de cada ronda.** Si una ronda descubre algo que contradice
   este archivo, se corrige aquí y no en el mensaje de commit.
3. **Firmas entre pares.** `docs/FEDERATION.md` lo declara como trabajo de gobernanza pendiente:
   deciding el algoritmo y la distribución de claves es una decisión, no una línea de código.
   No se implementa sin esa decisión.

## Bloqueos

- **Sin bloqueos de comandos.** Suite y preflight en verde.
- **Federación sin autenticación mutua ni respuesta sobre residencia de datos.** Documentado en
  `docs/FEDERATION.md` y en el ADR; requiere decisión de gobernanza antes de un despliegue real
  transfronterizo.
- **`%TEMP%/get-shit-done/` sin trackear en el árbol de trabajo.** Instalación del framework de
  agentes con la variable sin expandir; no pertenece al repositorio y no se commitea.