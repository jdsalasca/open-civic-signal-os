# Checklist de lanzamiento

Lista de verificación previa al lanzamiento público de Open Civic Signal OS en
`https://open-comunity.eridu.top`.

## Infraestructura y secretos

- [ ] `infra/.env.prod` creado desde `infra/.env.prod.example`, sin secretos en git.
- [ ] `POSTGRES_PASSWORD` y `JWT_SECRET` generados con entropía suficiente.
- [ ] `CORS_ALLOWED_ORIGINS` incluye solo orígenes necesarios (`https://open-comunity.eridu.top`).
- [ ] `INTEGRATION_CREDENTIAL_KEY` configurado si se usan integraciones de mensajería.
- [ ] Imágenes fijadas por tag/SHA (no `latest`) en producción.
- [ ] `SPRING_PROFILES_ACTIVE=prod` en producción (sin datos de prueba).
- [ ] Backups automatizados y restauración probada.
- [ ] Health checks verdes: `/actuator/health` y web en `127.0.0.1:5175`.
- [ ] Túnel de Cloudflare activo y DNS resolviendo a `https://open-comunity.eridu.top`.

## Cuentas y seguridad

- [ ] Rotar o deshabilitar las credenciales sembradas (`admin` / `admin12345`, `servant` / `servant123`,
  `citizen` / `citizen123`) si el perfil `dev` o `test` se ejecutó en este entorno. El sembrador
  (`OpenCivicSignalOsApplication.seedUsers`) solo corre en esos perfiles.
- [ ] Confirmar que no queda ninguna cuenta con contraseña por defecto en producción.
- [ ] Definir el primer administrador fuera del repositorio.
- [ ] Revisar `docs/SECURITY_BASELINE.md`.

## Datos y privacidad

- [ ] Crear páginas o placeholders públicos de privacidad y términos (actualmente no existen en la app)
  o enlazar documentos externos.
- [ ] Verificar anonimización antes de publicar dashboards.
- [ ] Confirmar captura mínima de PII en la ingesta.
- [ ] Revisar `docs/architecture/ADR-20260321-public-data-anonymization-gate.md`.

## Observabilidad y monitoreo

- [ ] Logs estructurados sin PII.
- [ ] Alertas de tasa de error y datos obsoletos.
- [ ] Dashboard de salud del servicio.
- [ ] Plan de respuesta a incidentes (`docs/TRUST_INCIDENT_TRIAGE.md`).

## Producto

- [ ] Verificar flujos clave (registro, reporte, priorización) en móvil y escritorio.
- [ ] Documentar limitaciones conocidas en `README.md`.
- [ ] Publicar resumen "qué cambió para las comunidades".
- [ ] Revisar `docs/RELEASE_POLICY.md`.