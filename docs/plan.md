# Plan de rondas

Estado real del repositorio y proximas rondas. El tablero operativo vive en
`docs/kanban.md` (lo refresca un bot) y el historico en `docs/metrics-history.md`. Este archivo
es el plan de trabajo con criterios verificables.

## Estado

- **Tests:** 424 en `apps/api-java`, todos en verde con `mvn clean test`.
- **Calidad:** `npm run agent:preflight` en verde; parity OpenAPI 178 rutas / 147 documentadas /
  9 intencionalmente no documentadas.
- **Issues:** 2 abiertos, ambos de proceso (`#120` calidad de agentes, `#121` foco de sprint).
  Todos los issues de feature P1/P2 estan cerrados.
- **Rama:** `develop`, sin worktrees ni ramas pendientes.

## Reglas de calidad que este plan aplica

Una ronda cuenta solo si entrega valor verificable: codigo que corre, o un defecto con un test que lo
reproduce. Las rondas de refresco de documentacion no cuentan.

Cinco defectos del ultimo tramo tuvieron la misma forma, y conviene tenerla escrita porque es el
patron que mas caro sale:

1. **Un test que afirma contra un valor que el camino de produccion nunca produce no puede fallar.**
   Ocurrio tres veces: `MessagingConnectorTest` construia la entidad a mano con un token en claro que
   el service nunca daba; `DigestSchedulerIT` leia una propiedad desde un `StandardEnvironment` nuevo
   sin las fuentes de la aplicacion; `handleInternalError` descartaba la causa del error.
2. **Probar el service no es probar el endpoint.** El preview del digest se verifico a nivel de service
   y el controller llamaba a otro metodo. El test que lo caza tiene que pasar por el controller.
3. **Verificar que un test falla cuando el codigo se rompe.** Un test que pasa con y sin el fix no esta
   probando el fix. Las rondas 43 a 46 se comprobaron por mutacion.
4. **Una copia por servicio es la misma clase de defecto que una formula duplicada.** La ronda 44
   encontro ocho definiciones de "resuelto". Dos copias comparadas por un test siguen siendo dos copias:
   es la leccion de `PrioritizationFormula`, con un filtro en vez de aritmetica. Cuando un concepto de
   dominio aparece en varios servicios, su dueno es el tipo de dominio, no el servicio.
5. **Un camino de lectura que contesta desde el estado vivo lo que su hermano contesta desde el
   historial.** Tres en tres rondas (43, 44, 45). La forma de encontrarlo es siempre la misma: dos
   piezas del mismo concepto, una correcta y otra no. El test que mas rinde no comprueba el valor
   esperado sino la **coherencia interna** - que la metrica y la lista del mismo informe no se
   contradigan - porque asi no puede pasar por acertar una de las dos.

## Rondas

| Ronda | Entregable | Criterio verificable | Estado |
| --- | --- | --- | --- |
| 39 | Pantalla de boletin semanal | 12 Playwright + 28 regresion Layout/nav | Hecho `68172c1` |
| 40 | Sellado de tokens de bot (AES-GCM) | 397 tests; el token llega intacto al proveedor via API | Hecho `f058c45` |
| 41 | El digest preparado se guarda y se publica | 402 tests; publicar envia el artefacto sellado | Hecho `52126b1` |
| 42 | Namespace de ciudad y backlog federado | 412 tests; dos ciudades sin mezcla; revocar consentimiento corta el feed | Hecho `f035b5b` |
| 43 | El preview por API es el artefacto que se publica | 413 tests; falla por mutacion del controller | Hecho `3ea5408` |
| 44 | Una sola definicion de "resuelto" para todos los rankings | 420 tests; 8 copias reducidas a 1; falla por mutacion | Hecho `e150e3b` |
| 45 | Un mes cerrado se responde desde el historial, no desde el estado vivo | 423 tests; metrica y lista coinciden; falla por mutacion | Hecho `fb72451` |
| 46 | El informe declara en su payload lo que no puede reproducir | 424 tests; dos afirmaciones falsas corregidas | Hecho este commit |
| 47 | Ledger de scores: cuando un score se vuelve oficial | Decision de gobernanza mas esquema; no es una ronda de codigo | Siguiente |

## Proximas rondas candidatas

1. **Auditar el mismo patron en otras piezas del mismo concepto.** Las rondas 43 a 46 lo encontraron
   cuatro veces en domains distintos. Quedan candidatos: el par lectura/escritura del handoff
   institucional, y las vistas publicas que recalculan en vez de servir lo almacenado.
2. **Escribir siempre los archivos markdown con la herramienta de edicion, no con PowerShell.**
   `Set-Content` sin `-Encoding utf8` rompio acentos y emojis en este archivo y dejo mojibake
   committed-ready en dos filas. Se reparo reescribiendo en ASCII.
3. **Firmas entre pares.** `docs/FEDERATION.md` lo declara como trabajo de gobernanza pendiente:
   decidir el algoritmo y la distribucion de claves es una decision, no una linea de codigo.
   No se implementa sin esa decision.
4. **Ledger de scores.** Decidir cuando un score se vuelve oficial (al rescoring, al publicar, al
   aprobar) y despues registrarlo. Sin esa decision, un ledger queda sin autoridad.

## Bloqueos

- **Sin bloqueos de comandos.** Suite y preflight en verde.
- **Federacion sin autenticacion mutua ni respuesta sobre residencia de datos.** Documentado en
  `docs/FEDERATION.md` y en el ADR; requiere decision de gobernanza antes de un despliegue real
  transfronterizo.
- **Scores historicos.** El informe de transparencia declara la limitacion en su payload
  (`reproducibilityLimits`) en vez de ocultarla. El arreglo completo depende de la ronda 47.
- **`%TEMP%/get-shit-done/` sin trackear en el arbol de trabajo.** Instalacion del framework de
  agentes con la variable sin expandir; no pertenece al repositorio y no se commitea.