# Desplegar solamente el backend

Esta guia prepara un despliegue academico en planes gratuitos. No requiere arrancar
otra base en el computador ni desplegar todavía el frontend. Tener estos archivos
no significa que el servicio ya este publicado: faltan las cuentas y sus variables.

## 1. Repositorio

Conservar el repositorio actual y trabajar en una rama. Copiar el contenido de esta
carpeta junto al `pom.xml`, sin borrar `.git`. No subir bases H2, `target`, `.env`,
credenciales ni configuraciones privadas. El Dockerfile debe quedar en la misma
carpeta que `pom.xml` y `src`.

## 2. Aiven PostgreSQL

Usar la base exclusiva de TrabajaYA. Puede llamarse `defaultdb`: el nombre del
servicio no es necesariamente el nombre de la base. Copiar host, puerto, usuario
y contraseña desde Aiven. Descargar el CA del servicio; no cambiar SSL a un modo
sin verificación para solucionar errores de conexión.

La URL JDBC no es la Service URI `postgres://...`:

```text
jdbc:postgresql://HOST:PUERTO/NOMBRE_BASE?sslmode=verify-full&sslrootcert=/etc/secrets/aiven-ca.pem
```

El usuario debe tener permisos para crear las tablas e índices de Flyway. V1 crea
el esquema y V2 agrega índices. **No ejecutar estos SQL manualmente, no borrar
`flyway_schema_history` y no modificar V1 en una base ya migrada.** No hay operaciones
de limpieza de tablas en estas migraciones.

## 3. Supabase Storage

En el proyecto de Supabase, crear el bucket **privado** `trabajaya-images`. El nombre
del proyecto no crea automáticamente el bucket. Permitir `image/png` y un límite de
10 MiB por objeto. El backend recibe JPEG/PNG de hasta 5 MiB, verifica dimensiones
y convierte a PNG; el resultado permitido es de hasta 8 MiB.

Copiar la URL HTTPS del proyecto y una clave **secret** de servidor `sb_secret_...`.
El adaptador también admite la antigua clave JWT `service_role`, pero nunca se debe
usar una clave `anon`/publishable para esta conexión. Las claves modernas se envian
en `apikey`; las antiguas, ademas, en Authorization Bearer. La clave queda solo en
Spring Boot, nunca en el frontend ni en el repositorio.

No habilitar acceso público para solucionar permisos. Las solicitudes de usuarios
pasan por la autorización de la API antes de acceder al objeto privado.

## 4. Web Service en Render

Seleccionar el repositorio y crear **Web Service > Docker > Free**.

| Campo | Valor |
|---|---|
| Root Directory | Vacio si `pom.xml` esta en la raiz; si no, su subcarpeta |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context | `.` |
| Docker Command | Vacio; usa ENTRYPOINT |
| Health Check Path | `/actuator/health` |
| Region | Virginia es una opción; preferir cercania a los servicios |

El contenedor escucha en `0.0.0.0` y toma `PORT` de Render. No crear una base ni un
disco de pago en Render. La compilación Docker omite pruebas: la verificación va
en GitHub Actions. Es preferible integrar el PR cuando el CI este aprobado.

`render.yaml` es una alternativa al formulario, no un paso adicional obligatorio.
El certificado CA se agrega manualmente también cuando se usa el Blueprint.

## 5. Variables para completar después

En Environment, sin comillas adicionales:

| Variable | Valor o finalidad |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `postgres,render` |
| `DB_URL` | JDBC con verify-full y ruta al CA |
| `DB_USERNAME` | Usuario de Aiven |
| `DB_PASSWORD` | Contrasena real de Aiven |
| `DB_POOL_SIZE` | `3` para comenzar |
| `STORAGE_PROVIDER` | `supabase` |
| `SUPABASE_URL` | URL del proyecto, sin `/storage/v1` |
| `SUPABASE_SERVICE_KEY` | Clave secreta del servidor |
| `SUPABASE_BUCKET` | `trabajaya-images` |
| `DEMO_SEED` | `false` |
| `MAIL_ENABLED` | `false` |
| `ADMIN_EMAIL` | Correo del administrador |
| `ADMIN_PASSWORD` | Al menos 12 caracteres, letras y numeros, hasta 72 bytes UTF-8 |

El administrador inicial es opcional, pero las dos variables deben configurarse
juntas. Si ese correo ya existe, **no se cambia su contraseña al redeplegar**. Nunca
puede crearse un administrador desde el registro público.

El perfil render rechaza PostgreSQL sin verify-full, almacenamiento local y datos
demo. No desactivar esas comprobaciones para ocultar una variable faltante.

Cuando se publique el frontend se agregaran `FRONTEND_URL` y `CORS_ORIGINS` con su
origen HTTPS real, sin rutas adicionales. El correo permanece apagado; cambiar
MAIL_ENABLED por si solo no integra un proveedor.

### Certificado

En Render > Environment > Secret Files, crear `aiven-ca.pem` y pegar el PEM completo,
con BEGIN CERTIFICATE y END CERTIFICATE. Se monta en `/etc/secrets/aiven-ca.pem`.
No incluirlo dentro de la imagen ni de la plantilla de variables.

## 6. Arranque público

Hacer Deploy y revisar logs de Maven, Spring y Flyway. En la URL asignada:

```text
https://TU_BACKEND.onrender.com/actuator/health
https://TU_BACKEND.onrender.com/api/categories
https://TU_BACKEND.onrender.com/api/health
```

Se espera salud UP y categorías persistidas. El health informativo con storage
supabase confirma configuración, **no credenciales ni una subida real**. Esa última
verificación se hace con un usuario autenticado y `POST /api/media`.

La API no muestra una página principal en `/`. Una respuesta de seguridad en esa
ruta no es evidencia de que el servicio falle. No enviar claves ni tokens al pedir
ayuda con logs.

## 7. Límites del plan gratuito

Render Free suspende servicios después de inactividad. El programador de precios
solo corre con el proceso activo; al volver no reconstruye todos los intervalos
perdidos. No se usan trucos de trafico artificial para impedir la suspensión.

El disco es efimero: las imágenes deben ir a Supabase y los registros de negocio a
Aiven. Render Free bloquea puertos SMTP habituales; por ahora no hay entrega de
correos de recuperación. Una futura integración por API HTTPS requiere código.

Revisar que Aiven sea realmente Free y no solo un servicio pagado con créditos de
prueba. Los límites y politicas de cada proveedor pueden cambiar; confirmar el plan
antes de guardar una configuración que tenga costo.

## Fuentes de configuración

- Render Docker: https://render.com/docs/docker
- Render servicios web: https://render.com/docs/web-services
- Render archivos secretos: https://render.com/docs/docker-secrets
- Render Free: https://render.com/docs/free
- JDBC PostgreSQL SSL: https://jdbc.postgresql.org/documentation/ssl/
- Aiven TLS: https://aiven.io/docs/platform/concepts/tls-ssl-certificates
- Supabase claves: https://supabase.com/docs/guides/getting-started/api-keys
- Supabase acceso Storage: https://supabase.com/docs/guides/storage/security/access-control
