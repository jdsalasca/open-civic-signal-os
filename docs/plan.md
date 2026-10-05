# Plan de rondas

Estado real del repositorio y proximas rondas. El tablero operativo vive en
`docs/kanban.md` (lo refresca un bot) y el historico en `docs/metrics-history.md`. Este archivo
es el plan de trabajo con criterios verificables.

## Estado

- **Tests:** 436 en `apps/api-java`, todos en verde con `mvn clean test`.
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
6. **Un teto puesto por el servidor y un array truncado es una mentira si nadie la declara.** La ronda
   50 acoto la respuesta de una sala y devolvio `hasMoreMessages` + el total real. La ronda 51 encontro
   que la pantalla renderizaba esa pagina como si fuera la sala: 50 filas al lado de 1.204 mensajes,
   sin ninguna senal de que faltan 1.154. El campo que hace detectable la truncacion existe para ser
   usado por el consumidor; si el consumidor no lo usa, la propiedad es decorativa. Y truncar en
   silencio no es "optimizar": es decidir que a la persona que coordina le alcanza con los 50 mas
   recientes. El hermano mayor: acotar la carga sin acotar la **consulta** tampoco esta hecho
   (ronda 52).
7. **Un valor correcto tambien puede salir de leerlo todo.** El conteo de mensajes de una sala era
   correcto y salia de `findAll().size()` sobre la historia entera, una vez por sala en cada apertura
   del workspace. La prueba que lo caza no puede assertar `messageCount` -el codigo roto tambien
   devolvia 60- sino medir I/O: la misma consulta con 5 y con 60 mensajes tiene que leer lo mismo. Y
   `em.clear()` antes de medir, o la cache de primer nivel responde y el test pasa por la razon
   equivocada. Un test verde que no puede fallar es peor que no tener test: enacta el defecto como
   correcto.
8. **Un N+1 detras de una cache sigue siendo N+1, y solo aparece con datos realistas.** El nombre del
   autor se resolvia dentro del `.map()` que renderiza cada mensaje; con un solo autor las busquedas
   siguientes las respondia la cache de primer nivel y el test pasaba con el defecto puesto. Con un
   autor por mensaje salio (58 vs 13). "Realista" aqui quiere decir "una conversacion con mas de una
   persona", que es justo el caso que la plataforma existe para.
   Y la metrica importa tanto como el dato: `getQueryExecutionCount` no cuenta un `findById`, asi que
   un test que lo use no puede cazar un N+1 de carga-por-id - el de membresias solo se detecto al
   cambiar a `getEntityLoadCount`. Antes de dar por buena una medicion, mutar el codigo y ver que el
   test se pone rojo; un test que no se pone rojo no esta probando el fix.

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
| 47 | Revision visual del digest semanal + build output sin trackear | 12 Playwright, 8 capturas, 424 tests | Hecho `7a33213` |
| 48 | Ledger de scores + orden determinista del timeline | 429 tests; falla por mutacion; 2 preflights seguidos | Hecho este commit |
| 49 | Una asamblea cerrada reporta el tiempo que duro | 430 tests; falla por mutacion | Hecho este commit |
| 50 | Una sala no descarga su historia entera al abrirse | 432 tests; falla por mutacion | Hecho `387095f` |
| 51 | Un historial de sala truncado se ve truncado, y se pagina a pedido | 6 Playwright (2 mutaciones); 8 capturas; 432 tests | Hecho este commit |
| 52 | Contar los mensajes de una sala ya no los lee | 433 tests; el test mide I/O y falla por mutacion | Hecho este commit |
| 53 | Las pantallas de sala dejan de consultar una vez por fila | 436 tests; 58->8 queries y 61->5 entity loads; 2 mutaciones | Hecho este commit |
| 54 | La bandeja de menciones resuelve un nombre por fila (N+1 acotado a 20) | Test diferencial sobre la bandeja | Siguiente |

## Proximas rondas candidatas

1. **Auditar el mismo patron en otras piezas del mismo concepto.** Las rondas 43 a 46 lo encontraron
   cuatro veces en domains distintos. Quedan candidatos: el par lectura/escritura del handoff
   institucional, y las vistas publicas que recalculan en vez de servir lo almacenado.
2. **Nunca mas `git checkout -- apps/api-java/target`.** `target/` estaba trackeado (103 archivos,
   incluidos `.class`), asi que el paso de limpieza habitual revertia el build a un estado de hace
   meses. Tres "fallos fantasma" de la sesion salieron de ahi, y el riesgo real era que un `.class`
   viejo hiciera pasar un test contra codigo antiguo.Sacado del indice en la ronda 47; `.gitignore` ya
   lo listaba, se commiteo antes de que existiera la regla.
3. **Una pantalla que se verifica con asserts todavia no fue vista.** La revision visual del digest
   encontro un `<input>` crudo (el unico de la app), un eyebrow que describia mal la pantalla y una
   frase duplicada que yo mismo introduje. Ningun assert los ve.
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
- **Scores historicos solo desde la ronda 48.** El ledger existe; las senales anteriores a el no tienen
  entrada y su score pasado es incunable. El informe lo declara en `reproducibilityLimits`.
- **`%TEMP%/get-shit-done/` sin trackear en el arbol de trabajo.** Instalacion del framework de
  agentes con la variable sin expandir; no pertenece al repositorio y no se commitea.