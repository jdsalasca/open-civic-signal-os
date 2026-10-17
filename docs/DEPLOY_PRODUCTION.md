# Despliegue en producción

Guía para desplegar Open Civic Signal OS en un host detrás de un túnel de Cloudflare, publicado en
`https://open-comunity.eridu.top`. El despliegue usa `infra/docker-compose.prod.yml` y
`scripts/deploy-prod.sh` (gestionados por el equipo de infraestructura) y las variables de entorno
definidas en `infra/.env.prod.example`.

> Nota: si `infra/docker-compose.prod.yml` o `scripts/deploy-prod.sh` aún no existen en tu checkout,
> espera a que el otro trabajo de infraestructura los publique; esta guía les da por sentados con esos
> nombres exactos.

## Requisitos previos

- Docker + Docker Compose v2 en el host.
- `cloudflared` instalado y autenticado.
- Acceso al DNS del dominio `open-comunity.eridu.top` para enrutar el túnel.
- Un archivo `infra/.env.prod` creado a partir de `infra/.env.prod.example` (nunca se commitea).

## Variables de entorno

Copia y rellena el ejemplo:

```bash
cp infra/.env.prod.example infra/.env.prod
```

Variables esperadas (los nombres exactos son los que aparecen en `infra/.env.prod.example`):

| Variable | Uso |
|---|---|
| `POSTGRES_DB` | Nombre de la base de datos PostgreSQL. |
| `POSTGRES_USER` | Usuario de la base de datos. |
| `POSTGRES_PASSWORD` | Contraseña de la base de datos (obligatoria, sin default). |
| `JWT_SECRET` | Clave de firma de tokens JWT. Generar con `node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"`. |
| `CORS_ALLOWED_ORIGINS` | Orígenes del navegador permitidos. Debe incluir `https://open-comunity.eridu.top`. |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP opcional para correo real. |
| `INTEGRATION_CREDENTIAL_KEY` | Clave para cifrar tokens de integraciones (recomendada si se usan bots). |
| `API_IMAGE`, `WEB_IMAGE` | Imágenes a desplegar (por tag/SHA, no `latest` en producción). |

`CORS_ALLOWED_ORIGINS` debe ser restrictivo: solo el origen público y, si aplica, el host local de administración.

## Despliegue inicial

```bash
# 1. Preparar variables
cp infra/.env.prod.example infra/.env.prod
# editar infra/.env.prod con valores reales

# 2. Desplegar
bash scripts/deploy-prod.sh
```

`scripts/deploy-prod.sh` se encarga de tomar `infra/.env.prod`, levantar `infra/docker-compose.prod.yml`,
esperar a que los health checks estén verdes y dejar la aplicación disponible en el host.

## Health checks

- API: `curl -fsS http://127.0.0.1:8081/actuator/health` (debe responder `{"status":"UP"}`).
- Web: `curl -fsS http://127.0.0.1:5175/` (debe servir la aplicación).
- Estado de contenedores: `docker compose -f infra/docker-compose.prod.yml ps`.
- Endpoint proxied (a través de nginx): `curl -fsS http://127.0.0.1:5175/api/health`.

Si el puerto del API difiere en tu `docker-compose.prod.yml`, ajusta el comando al puerto publicado.

## Backups

Base de datos (dump lógico):

```bash
mkdir -p backups
docker compose -f infra/docker-compose.prod.yml exec -T civic-db \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  > "backups/signalos-$(date +%Y%m%d-%H%M%S).sql"
```

Volumen de datos (copia física, con el stack detenido o en ventana de bajo uso):

```bash
docker run --rm \
  -v civic-db-data:/data \
  -v "$(pwd)/backups:/backup" \
  alpine tar czf "/backup/civic-db-data-$(date +%Y%m%d).tgz" -C /data .
```

Programa ambos comandos en `cron` y verifica periódicamente que los archivos no estén vacíos.

## Restauración

```bash
# 1. Detener API y web para evitar escrituras
docker compose -f infra/docker-compose.prod.yml stop civic-api civic-web

# 2. Restaurar el dump
docker compose -f infra/docker-compose.prod.yml exec -T civic-db \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < backups/signalos-YYYYMMDD-HHMMSS.sql

# 3. Levantar de nuevo
docker compose -f infra/docker-compose.prod.yml up -d

# 4. Verificar
curl -fsS http://127.0.0.1:8081/actuator/health
curl -fsS http://127.0.0.1:5175/
```

Flyway vuelve a validar el esquema al arrancar el API; si una migración falla, revisa los logs del contenedor
`civic-api` antes de continuar.

## Rollback

1. Identifica el tag/SHA anterior de las imágenes (`API_IMAGE`, `WEB_IMAGE`) y actualízalos en `infra/.env.prod`.
2. Re-ejecuta `bash scripts/deploy-prod.sh`.
3. Si el despliegue incluyó migraciones de base de datos incompatibles, restaura primero el backup previo
   (ver Restauración) y luego despliega la imagen anterior.
4. No cambies el ingress del túnel salvo que el host o el puerto cambien.

Preferir rollback de imagen sobre `git checkout` del código: las migraciones ya aplicadas no se revierten
automáticamente.

## Túnel de Cloudflare

Snippet de ingress para `~/.cloudflared/config.yml` (o el archivo de configuración del túnel):

```yaml
tunnel: <TUNNEL_ID>
credentials-file: /etc/cloudflared/<TUNNEL_ID>.json
ingress:
  - hostname: open-comunity.eridu.top
    service: http://127.0.0.1:5175
  - service: http_status:404
```

Comandos típicos:

```bash
cloudflared tunnel create open-civic-signal-os
cloudflared tunnel route dns open-civic-signal-os open-comunity.eridu.top
cloudflared tunnel run open-civic-signal-os
```

Para mantenerlo permanente, instálalo como servicio del sistema (`cloudflared service install`) y verifica con
`cloudflared tunnel info open-civic-signal-os`.

## Seguridad operativa

- No commitear `infra/.env.prod` ni archivos de credenciales del túnel.
- Rotar `JWT_SECRET` y `POSTGRES_PASSWORD` ante cualquier sospecha de fuga.
- Mantener `CORS_ALLOWED_ORIGINS` mínimo.
- Revisar `docs/SECURITY_BASELINE.md` y `docs/OBSERVABILITY.md`.