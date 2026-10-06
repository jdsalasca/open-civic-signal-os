# Plan de rondas

Estado real del repositorio y proximas rondas. El tablero operativo vive en
`docs/kanban.md` (lo refresca un bot) y el historico en `docs/metrics-history.md`. Este archivo
es el plan de trabajo con criterios verificables.

## Estado

- **Tests:** 446 en `apps/api-java`, todos en verde con `mvn clean test` (5 saltados: la paridad con
  PostgreSQL, que necesita Docker).
- **Esquema:** las 49 migraciones corren en H2 (la suite) y en PostgreSQL 15, y `npm run
  schema:pg:verify` compara ambos: 62 tablas y 582 columnas, con nombres, tipos, nulabilidad y
  largo de cadena identicos. Rondas 57 a 60.
- **CI:** `postgres-schema-parity.yml` **ya se ejecuto en GitHub** (PR #122): verde en 2m23s, y el
  `push` a develop tambien. Su primer run fallo ylvaron un bug real. Ronda 61.
- **CI roto (preexistente):** `docker-images.yml` falla en develop desde al menos 09:29 de hoy; el
  smoke test del contenedor API no levanta en el 18080. Ronda 62.
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
9. **Un test que afirma una garantia que el sistema no hace es un fallo intermitente esperando su
   turno.** Las actas de asamblea se ordenaban solo por `created_at`; H2 no guarda fracciones de segundo,
   asi que dos actas caidas en el mismo segundo empatan y la base puede devolver cualquiera primero. El
   test afirmaba "April primero" y pasaba en casi todas las corridas: no era un test, era una moneda al
   aire. La estabilidad tampoco se puede verificar de caja negra - H2 es consistente consigo mismo para
   un plan y unos datos dados, asi que dos consultas identicas devuelven el mismo orden por suerte, con
   desempate o sin el. Ese test se borro en vez de subirse. Y al escribir el fix correcto (una posicion
   monotona como la de V50) aparecio algo mas grande: **`ddl-auto: create-drop` borra lo que Flyway
   construyo**, asi que en la suite ninguna columna `seq` se rellena y el fix de la ronda 48 nunca se ha
   ejercitado. El orden que importa para un resident que audita una respuesta municipal esta verificado
   en produccion y sin verificar en las pruebas.
10. **Una columna que la base asigna no se lee desde la entidad ya gestionada.** Con
   `insertable = false` el valor lo pone la base, pero el objeto que queda en el contexto de
   persistencia sigue con `null`: un test que lea esa entidad informa del estado en memoria de Hibernate
   y declara rota una base de datos que funciona. `flush()` + `clear()` antes de releer. Y el
   `coalesce(e.seq, 0)` que escribi en V50 para "sobrevivir a una base sin secuencia" estaba
   escondiendo justo que en la suite no habia secuencia: una defensa contra un caso que nunca ocurre
   tapando el que si ocurre.
   Y `create-drop` no es un detalle de tests: significa que **las 49 migraciones llegan a produccion sin
   que la suite las haya ejecutado nunca**. Una columna que solo existe en un `.sql` no existe para las
   pruebas. Ya corregido en la ronda 56.
11. **Hibernate vacia inserts antes que updates y deletes.** "Libera la fila actual y luego inserta la
   nueva" choca contra el indice unico si ambas cosas van en el mismo flush: la nueva entra con el slot
   que la vieja todavia tiene. Dos bugs de produccion estaban vivos por esto - publicar de nuevo el
   backlog y reemplazar la agenda de una asamblea - y ambos tests que lo afirmaban llevaba rondas
   pasando, porque el esquema de los tests no tenia los indices. Cuando falte una restriccion, la
   pregunta no es solo "que dato falta" sino "que invariante nunca se pudo comprobar".
12. **Un `target/` stale tambien miente sobre la configuracion, no solo sobre el codigo.** Sin `clean`,
   Maven puede usar un `target/test-classes/application-test.yml` viejo: dos tests "fallaban" por una
   config que yo creia activa, y el diagnostico mostraba 7 FKs donde deberian ser 139. Es el mismo
   peligro de `target/` trackeado que el plan ya registraba, con otra forma: no un `.class` viejo que
   hace pasar un test contra codigo antiguo, sino un recurso viejo que hace correr un test contra
   configuracion antigua. Toda evidencia de migraciones viene de `mvn -o clean test`.
13. **Un fallo que no dice nada del dominio es un fallo peligroso.** Al verificar el esquema contra
   PostgreSQL, el script reporto `FAILED` dos veces sin una linea de Maven, por `spawnSync mvn ENOENT`
   (Maven no esta en el PATH de un proceso Node en Windows aunque PowerShell lo encuentre) y
   `spawnSync mvn.cmd EINVAL` (no se puede ejecutar un `.cmd` con `execFileSync`). Ninguno de los dos
   tinha que ver con esquemas, y los dos se leen como "las schemas no coinciden". Un error que no nombra
   la capa donde fallo invita a "arreglarlo" aflojando una asercion: hay que imprimir lo que la
   herramienta dijo de verdad antes de tocar la prueba.
14. **No reinicies el estado de trabajo de otra persona para hacer tu medicion.** El primer intento
   apunto al `civic-db` de la infra y fallo por autenticacion: el volumen se inicializo con una contrasena
   antigua. Se podia recrear el volumen o dejarlo; recreate destruye datos locales que no son mios y,
   peor, hace que la verificacion dependa del estado de una base en la que alguien trabaja. Traerse su
   propio PostgreSQL, en su puerto, con una contrasena desechable. **Pendiente para el humano:** el
   volumen `civic-db` tiene una contrasena que no coincide con `infra/.env`, asi que `docker compose up`
   no autentica hasta que alguien lo recree o corrija el archivo.
15. **Un workflow no se prueba en la maquina donde lo escribiste.** El script de la ronda 57 llegaba a
   PostgreSQL por `host.docker.internal`, que es una comodidad de Docker Desktop y **no resuelve en un
   runner Linux**: habria pasado en Windows y fallado en el primer PR, con un mensaje que se lee como
   "los esquemas no coinciden" - la unica conclusion que ese script no debe equivocar. Red de Docker y
   nombre de contenedor en su lugar, y `127.0.0.1` en vez de `localhost` porque el puerto publicado es
   IPv4. Hallado leyendo el script contra el runner, no ejecutandolo: **el workflow sigue sin haberse
   corrido en GitHub**, y eso queda escrito.
16. **Antes de integrar una rama ajena, comprobar si ya esta superseded.** Las dos ramas `codex/*`
   seguian sin mergear y `git branch --no-merged develop` las listaba como trabajo pendiente. Las dos
   eran enteramente redundantes: el fix ya estaba en develop. Integrarlas a ciegas habria sido ruido, y
   borrarlas por "parecen superadas" es como desaparece el trabajo de otra persona. Se documentan y se
   dejan; `git branch -d` las habria rechazado igual por no estar mergeadas.
17. **Una mutacion que no se aplica es peor que no hacerla.** Al quitar una regla de normalizacion para
   comprobar que el test de tipos falla, el reemplazo de varias lineas no coincidio: el archivo tiene
   CRLF y el patron tenia LF. La corrida "mutada" paso, y ese verde no probaba nada - era el codigo sin
   mutar. Un falso negativo en una mutacion teaches a confiar en un test que no compara. Antes de creer
   cualquier resultado, grep del archivo para confirmar que el cambio esta, y rehacerlo con la
   herramienta de edicion. Regla general: **reemplazos multilinea hechos con script sobre archivos que
   ya existen, se verifican con grep o se rehacen con la herramienta de edicion.**
   Y yo la violiameSAMO round: un `.Replace` con script en `docs/plan.md` se comio los backticks de
   ``varchar(64)`` y ``act``, dejandolos como `archar(64)` y `ct`, en la misma ronda que escribe la
   regla. Backticks en un string de PowerShell son caracteres de escape: el patron de la regla tiene
   que cumplirse en laPráctica, no solo estar escrita.
18. **Un valor "obvio" en la normalizacion es una suposicion hasta que se mide.** Escribi
   `Integer.MAX_VALUE` como la marca de "sin limite" de H2 porque es lo que parece; el test fallo en las
   63 columnas con `1000000000 vs unbounded`. H2 usa su propio marcador de mil millones. Y un
   `(Integer) rows.getObject(5)` funcionaba con H2 y lanzaba con PostgreSQL, que devuelve `Long` para
   `character_maximum_length` - un fallo que solo aparece en un motor y se lee como problema de esquema
   cuando es un cast.
19. **Una mutacion tautologica no demuestra nada, pero una real si.** Reemplazar el valor comparado por
   `"bounded"` en ambos lados paso: es la tautologia de dejar de comparar. La mutacion que si demuestra
   es `alter table users alter column username type varchar(64)` sobre PostgreSQL ya migrado, que es lo
   que haria una migracion descuidada y lo que el test caza nombrando la columna.
20. **Ejecutar el gate es una ronda de trabajo, no un detalle.** Tres rondas documentaron que el
   workflow de paridad nunca se habia ejecutado en GitHub; abrir el PR #122 lo ejecuto y encontro en 20
   segundos lo que tres rondas de inspeccion no vieron: `mvn -o` hardcodeado, que funciona con un
   `~/.m2` tibio y hace imposible resolver hasta el parent POM en un runner limpio. **"Verificado en mi
   maquina" nunca es "verificado en CI", y la diferencia no es academica: es un flag.**
   El mismo run confirmo lo contrario: el refactor de red Docker de la ronda 58 funciono en un Linux
   real, que era justo lo que no se podia comprobar desde Windows.
21. **Un gate de CI que nunca se dispara es peor que no tenerlo**, porque aparenta cobertura. El workflow
   escuchaba pushes a `main` cuando todo el trabajo de este repo pasa por `develop`: iba a quedar en
   verde sin ejecutarse nunca. Por eso el PR #122 no fue vacio - llevo la correccion que lo hace real.
22. **Un mismo error puede tener dos causas apiladas, y tapar la primera esconde la segunda.** El CI de
   Docker reportaba `Failed to connect to localhost port 18080` en cinco corridas seguidas. Arregle que
   la imagen no arranca sin base de datos - y entonces aparecio el mismo error en **otro** contenedor:
   nginx resuelve `proxy_pass` al arrancar, asi que sin un host `civic-api` no degrada la ruta, no
   arranca. Un error repetido cinco veces no es un error, son dos errores con el mismo sintoma.
23. **Un shell de desarrollo puede esconder lo que un runner limpio no.** El API no arranca sin
   `JWT_SECRET` porque no tiene default a proposito; mi maquina si lo tenia exportado (88 chars), asi que
   la reproduccion local paso en parte por suerte. Es la segunda vez en esa ronda que una maquina que
   funciona escondia un runner roto. Antes de creer una reproduccion local: **mirar las variables de
   entorno que el contenedor no tendra.**
24. **Validar un workflow por sus claves no dice si sus pasos estan bien ordenados.** Al reordenar los
   pasos por numero de linea perdi `Build Web image` completo: el YAML parseaba, todas las claves que
   miraba estaban, y CI fallo con `pull access denied` en un segundo. Lo que lo cazó fue **leer la lista
   de nombres de los pasos**, no un chequeo de claves. Y despues de tres iteraciones de descubrir flags
   perdidos de uno en uno por ciclo de CI, el ciclo que funciono fue verificar **todos** los flags de
   ambos contenedores de una pasada antes de pushear.

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
| 49 | Una asamblea cerrada reporta el tiempo que duro | 430 tests; falla por mutacion | Hecho este commit |
| 50 | Una sala no descarga su historia entera al abrirse | 432 tests; falla por mutacion | Hecho `387095f` |
| 51 | Un historial de sala truncado se ve truncado, y se pagina a pedido | 6 Playwright (2 mutaciones); 8 capturas; 432 tests | Hecho este commit |
| 52 | Contar los mensajes de una sala ya no los lee | 433 tests; el test mide I/O y falla por mutacion | Hecho este commit |
| 53 | Las pantallas de sala dejan de consultar una vez por fila | 436 tests; 58->8 queries y 61->5 entity loads; 2 mutaciones | Hecho este commit |
| 54 | La bandeja de menciones deja de consultar 3 veces por fila | 437 tests; 30->15 queries; y el orden de evidencia de asamblea, que hacia fallar el preflight | Hecho este commit |
| 55 | La secuencia del timeline de auditoria ya se puebla y se testea | 438 tests; falla por mutacion sin el `columnDefinition` | Hecho este commit |
| 56 | Las 49 migraciones se ejecutan en la suite | 441 tests; 2 bugs de produccion (supersede, agenda) y 10 tests que escribian datos imposibles | Hecho este commit |
| 57 | Las migraciones corren tambien contra PostgreSQL y se comparan con H2 | 49 migraciones en PG15; 62 tablas y 582 columnas identicas; falla con PG sin migrar | Hecho este commit |
| 58 | schema:pg:verify corre en cada PR, y el script ya funciona en Linux | Workflow con paths acotados; red Docker en vez de host.docker.internal | Hecho este commit |
| 59 | La paridad compara tambien los TIPOS de columna | 582 columnas; 162 difieren en 2 pares de deletreo; falla nombrando 99 columnas | Hecho este commit |
| 60 | La paridad compara nulabilidad y largo de columna | 441/141 nulos en ambos; 136 varchar con 24 largos iguales; detecta un varchar(64) real | Hecho este commit |
| 61 | El workflow de paridad ya corrio en GitHub | PR #122: 1o run fallo por `-o`, 2o verde en 2m23s, y `push` a develop en verde | Hecho este commit |
| 62 | `docker-images.yml` en verde: la imagen API arranca con BD y nginx resuelve su upstream | 23 pasos en verde e imagenes publicadas; 2 defectos (sin BD, upstream sin resolver) | Hecho este commit |
| 63 | El backlog publico habla lenguaje claro y su sello de frescura no depende del navegador | 14 Playwright; sin `IN_PROGRESS` ni `1/4/2026, 10:00:00 a. m.` ni `313.00`; igual bajo locale `de-DE` | Hecho este commit |
| 64 | `SignalDetail` deja de mostrar el enum crudo y su bitacora no depende del locale | Primitivas compartidas `formatStamp` y `useSignalStatusLabel`; 6 Playwright; sin `IN_PROGRESS` ni `1.4.2026, 10:00:00` | Hecho este commit |
| 65 | Ninguna vista renderiza un timestamp por el locale del navegador | 25 sitios en 16 archivos; guardia `no-locale-timestamps` con mutacion verificada; 26/10 fallos igual que en baseline | Hecho este commit |
| 66 | El detalle de caso describe mal su propia pantalla | Eyebrow `Priority Rank` y subtitulo `Intelligence Context` repetian tarjetas; el topbar decia `Home` porque `activeSection` comparaba rutas exactas | Hecho este commit |
| 67 | Tres specs muertas del dashboard vuelven a correr | `signals/aging` y `help-center` sin mockear causaban `logout()`; 3 tests en verde y 18 con las specs del detalle | Hecho este commit |
| 68 | `dashboard-aging` vuelve a correr y el helper de sesion queda completo | Le faltaban `auth/me` y `help-center`; 2 tests en verde y 20 con todo lo de las rondas 64-68 | Hecho este commit |
| 69 | Convertir las specs de comunidad y digest que siguen fuera | `weekly-digest`, `merge-review`, `field-data-mode` y `community-rooms-history` necesitan **membresia activa** en `communities/my`, no solo rutas | Siguiente |
| 69 | 17 specs de comunidad y digest vuelven a correr | `field-data-mode` 4, `weekly-digest` 7, `merge-review` 6; **37 tests en serial**; el patron se repite: falta una ruta y el sintoma apunta a la vista | Hecho este commit |
| 70 | La suite Playwright se ejecuta en CI contra un build de produccion | Nuevo `.github/workflows/playwright.yml`; 76 tests en verde en modo CI; la suite entera da 42/42 y queda en cuarentena explicita | Hecho este commit |
| 71 | Un solo puerto, declarado una vez | `ports.ts` exporta `WEB_PORT`; `vite.config.js` **versionado** eclipsaba al `.ts` y hacia inútiles todas las ediciones previas del config | Hecho este commit |
| 72 | La suite no puede volver a correr contra otra app | `globalSetup` compara el `<title>` con `Open Civic Signal OS`; 2 specs mas al gate (13 de 47), **80 tests** en verde | Hecho este commit |
| 73 | Seis specs navegaban a `127.0.0.1:5173`, que es la app de otro proyecto | Rutas relativas: ahora respetan `baseURL`; el contexto de error paso de una universidad a la app civic. **Fallan aun** por aserciones de contenido | Hecho este commit |
| 74 | `community-trust-metrics` en verde y dos locators ambiguos corregidos | Gate de 14 specs, **82 tests** en verde; `community-proposals` usaba `getByText` que matcheaba 2 y 4 elementos | Hecho este commit |
| 75 | La ambiguedad de locator era una clase, no un incidente | 5 `getByText().click()` acotados; `community-decisions` y `community-governance` en verde. Gate de 16 specs, **86 tests** | Hecho este commit |
| 76 | `mockAppBootstrap` degradaba el rol de membresia a MEMBER | Regresion propia de las rondas 73-75: pisaba el COORDINATOR que el spec sembraba y dejaba el boton deshabilitado. Rol ahora parametrizado | Hecho este commit |
| 77 | `community-projects` afirmaba sobre la tarea equivocada | Crear un board lo selecciona, asi que la tarea sembrada salia de pantalla y el test era impasable. Ahora mueve y comenta la tarea que creo; gate sube a 17 specs | Hecho este commit |
| 78 | Las specs de las rondas 73-75 pueden afirmar menos de lo que pretenden | Dos rounds seguidos(~2 specs) exhilarating una asercion o un permiso, no la app. Falta una forma de distinguir "afirmo otra cosa" de "no afirmo nada" | Siguiente |
| 79 | `community-proposals`expects un control de moderacion que nunca renderiza | Misma regresion de la ronda 76 sin aplicar aqui: `canModerate` exige COORDINATOR y el helper pisaba el rol a MEMBER. Verde en aislamiento | Hecho este commit |
| 80 | `community-trust-metrics` era flaky en mobile-chrome | `getByText` sin scope sobre una opcion de PrimeReact podia resolver a un nodo desmontado bajo carga. Ahora `.p-dropdown-item`, y `community-projects` si esta en el gate (18 specs, 2 corridas iguales) | Hecho este commit |
| 81 | Dos specs etiquetadas "decision de producto" sin verificarlo | Ninguna lo era: testid obsoleto en open-data (`scope-` vs `token-scope-`), y en announcements el testid si es de la vista que visita | Hecho este commit |
| 82 | `community-official-announcements`: 3 fallos en 12 lineas | URL absoluta a otro puerto, badge traducido afirmado en ingles, y `getByText('Pinned')` sobre un flag que no renderiza texto. Verde: 1 passed | Hecho este commit |
| 83 | El flake de mobile-chrome no era de ninguna spec | `browserContext.newPage` agotaba 30s: `workers: CI ? 1 : undefined` abria contexts en paralelo en local, y CI ya iba con 1. Ahora `workers: 1` siempre; gate con `--retries=0` | Hecho este commit |
| 84 | `community-official-announcements` entra al gate | El blocker del flake ya estaba resuelto en la ronda 83. Gate: 20 specs, 94 passed con `retries=0` | Hecho este commit |
| 85 | Las specs con backend no estan bloqueadas "sin backend" | El backend ya corria en 8080/8081 y el preview proxea `/api` a 8081. El bloqueo real es que no hay admin sembrado: login expira con `admin`/`admin12345`, credenciales que el CHANGELOG documenta | Hecho este commit |
| 86 | Auditoria visual del backlog publico | Sin defecto: el espacio de "Formula version v1" si existe, era la fuente. Quinta vez que un defecto aparente se evapora al verificar | Hecho este commit |
| 87 | Las specs con backend quedan fuera del gate por dos razones | (a) No hay admin sembrado: decidir perfil, entorno y politica de password es decision de backend. (b) El workflow de playwright no levanta backend, asi que necesitarian un segundo job con la pila completa. Gate re-verificado en la ronda 87: 20 specs, 94 passed, etries=0 | Siguiente |


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
- **Correccion de la ronda 67: los tests Playwright no necesitan backend sembrado.** Las rondas 65 y
  66 registraron que 26 specs fallaban porque "hacen login real contra el backend y esta base de
  datos no tiene esa cuenta". **Eso era incorrecto para un subconjunto.** Ningun workflow corre esta
  suite (`frontend-ux-evidence-gate.yml` solo valida el cuerpo del PR), asi que nadie lo noto. La causa
  real es que a esos specs les faltan dos rutas que el dashboard dispara y nadie recuerda mockear,
  `signals/aging` y `help-center`: la peticion llega al backend real, responde 401, el refresh responde
  403, y `axios.ts` llama `logout()`, que borra la sesion sembrada. El sintoma -aterrizar en `/login`-
  apunta a la vista y no al hueco. Un subconjunto mas pequeño si usa login real y si necesita backend.
- **Un fixture invalido puede entrar en bucle de crash.** Si `signals/aging` responde sin
  `atRiskSignals`, el dashboard revienta en `aging.atRiskSignals.length`, React desmonta el arbol, el
  boundary reintenta y vuelve a reventar: ~12 peticiones por segundo en vez de una pantalla en blanco.
  Los dos arrays requeridos estan ahora en el helper compartido. Sigue siendo cierto que un crash de
  render deberia verse como error y no como consumo de red.
- **Federacion sin autenticacion mutua ni respuesta sobre residencia de datos.** Documentado en
  `docs/FEDERATION.md` y en el ADR; requiere decision de gobernanza antes de un despliegue real
  transfronterizo.
- **Scores historicos solo desde la ronda 48.** El ledger existe; las senales anteriores a el no tienen
  entrada y su score pasado es incunable. El informe lo declara en `reproducibilityLimits`.
- **La garantia de orden del timeline de auditoria ya esta verificada (ronda 55).** Antes: `ddl-auto:
  create-drop` reemplazaba el esquema de Flyway y `seq` quedaba NULL en toda fila, asi que V50 nunca se
  habia ejercitado. Corregido con `columnDefinition` en la entidad. **Lo que sigue sin ejercitarse son
  las 49 migraciones en si mismas** - ver la ronda 56.
- **`%TEMP%/get-shit-done/` sin trackear en el arbol de trabajo.** Instalacion del framework de
  agentes con la variable sin expandir; no pertenece al repositorio y no se commitea.
