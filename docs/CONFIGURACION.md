# Configuracion local, PostgreSQL y Supabase

## Variables y secretos

Nunca pongas claves de base de datos o Supabase en el frontend ni en un commit. Puedes usar variables de entorno, o copiar `config/local.properties.example` a `config/local.properties` y completar sus propiedades. Ese archivo esta ignorado por Git y no contiene valores reales en esta entrega.

El archivo `.env` del backend **solo lo lee Docker Compose**. Spring usa variables del proceso o `config/local.properties`; no carga `.env` automaticamente. Ejecuta siempre desde la raiz del backend.

## PostgreSQL existente (sin Docker)

En pgAdmin o psql, con un administrador:

```sql
CREATE ROLE trabajaya LOGIN PASSWORD 'REEMPLAZA_POR_UNA_CLAVE_LOCAL';
CREATE DATABASE trabajaya OWNER trabajaya;
```

No reutilices el rol superusuario de PostgreSQL para la aplicacion. Configura DB_URL `jdbc:postgresql://localhost:5432/trabajaya`, DB_USERNAME `trabajaya`, DB_PASSWORD y perfil `postgres`. Flyway crea las tablas al arrancar y necesita permiso DDL sobre el esquema para esta modalidad local.

## PostgreSQL con Docker

No necesitas Docker si ya tienes PostgreSQL.

```powershell
Copy-Item .env.example .env
notepad .env
# Reemplaza DB_PASSWORD en ese archivo antes de iniciar.
docker compose up -d
# Usa aqui la misma clave que pusiste en .env.
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5433/trabajaya"
$env:DB_USERNAME="trabajaya"
$env:DB_PASSWORD="LA_MISMA_CLAVE"
$env:DEMO_SEED="true"
.\mvnw.cmd spring-boot:run
```

`docker compose down` detiene la base y conserva su volumen. No ejecutes `down -v` sobre datos que quieras guardar. Cambiar DB_PASSWORD despues de crear el volumen no cambia la clave del usuario existente de PostgreSQL.

## Imagenes con Supabase Storage

1. Crea tu proyecto Supabase y entra en Storage.
2. Crea un bucket **privado** llamado `trabajaya-images`.
3. Permite `image/png` y un limite de objeto de al menos 8 MB. Se reciben JPEG/PNG de hasta 5 MB; el backend vuelve a codificarlos como PNG, con limite procesado de 8 MB.
4. Copia la URL del proyecto y una clave privada de servidor `sb_secret_...` (recomendada); el adaptador tambien admite `service_role` heredada. No uses la clave publica anon/publishable para este adaptador.
5. Configura estas variables **solo en la terminal del backend**:

```powershell
$env:STORAGE_PROVIDER="supabase"
$env:SUPABASE_URL="https://TU_PROYECTO.supabase.co"
$env:SUPABASE_SERVICE_KEY="TU_CLAVE_PRIVADA_DE_SERVIDOR"
$env:SUPABASE_BUCKET="trabajaya-images"
.\mvnw.cmd spring-boot:run
```

Puede combinarse con perfil `local` o `postgres`. El bucket debe existir; la aplicacion no crea proyectos ni buckets. No se envian claves al frontend. Se utiliza la API REST de Storage desde Java, sin SDK en el navegador.

Flujo: navegador -> backend valida sesion/archivo -> Storage privado -> SQL guarda ID, propietario y ruta -> navegador consulta `/api/media/{id}` -> backend comprueba si puede leerlo -> descarga y entrega los bytes.

El adaptador distingue claves secretas modernas (header `apikey`) y JWT `service_role` (tambien Bearer). La integracion remota debe comprobarse con tu proyecto real: esta entrega no dispone de sus credenciales. Respuestas 401/403 suelen indicar clave/bucket/permisos; 404 puede indicar URL o bucket incorrectos. Consulta la consola del backend, sin publicar secretos.

### Cambiar de disco a Supabase

Cada imagen registra el proveedor usado. Cambiar la configuracion **no traslada** archivos existentes. La aplicacion rechaza leerlos con otro proveedor para evitar respuestas confusas. Para una demostracion, usa una base de pruebas nueva o vuelve a subir las imagenes despues del cambio. Para conservar datos reales, implementa una migracion de objetos y actualiza sus referencias solo despues de verificar cada copia. No se incluye un borrado masivo automatico.

Los archivos cargados y no asociados (por abandonar un formulario) permanecen en su almacenamiento. La limpieza por antiguedad y la reconciliacion de archivos huerfanos son mantenimiento pendiente. SQL y Storage no comparten una transaccion distribuida.

## Recuperacion por correo

En `local`, el enlace se imprime en la consola y vence a los 20 minutos. En un perfil sin consola, configura:

```powershell
$env:MAIL_ENABLED="true"
$env:SMTP_HOST="smtp.tu-proveedor.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="TU_USUARIO"
$env:SMTP_PASSWORD="TU_CLAVE_SMTP"
$env:MAIL_FROM="no-reply@tu-dominio.com"
$env:FRONTEND_URL="http://localhost:5173"
```

Se usa STARTTLS. Tu proveedor debe autorizar remitente y credenciales. La recuperacion no permite enumerar cuentas y revoca todas las sesiones anteriores. No se ha verificado SMTP externo en este entorno.

## Entornos separados

| Propiedad | Local rapido | PostgreSQL |
|---|---|---|
| Perfil | local (predeterminado) | postgres |
| Base SQL | H2 en `data/` | PostgreSQL por DB_URL |
| Datos ficticios | Si, primera ejecucion | No, salvo DEMO_SEED=true |
| Fotos | Disco por defecto | Disco o Supabase |
| Recuperacion | Consola local | SMTP configurado |

La direccion del backend para el proxy se configura en `.env` del frontend con `API_TARGET`. No pongas ninguna credencial en ese archivo del frontend.

## Referencias oficiales consultadas

- https://docs.spring.io/spring-boot/3.5/system-requirements.html
- https://supabase.com/docs/guides/storage
- https://supabase.com/docs/guides/api/api-keys
- https://supabase.com/docs/guides/storage/buckets/fundamentals
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/reference/self-hosting-storage/upload-a-new-object
