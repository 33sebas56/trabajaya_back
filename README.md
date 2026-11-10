# TrabajaYA | Backend Spring Boot

Plataforma de trabajos independientes: el cliente publica una necesidad y propone un precio por persona. Los trabajadores compatibles pueden aceptar un cupo; el cliente puede aumentar la oferta para los cupos pendientes. Cada participacion conserva el precio aceptado.

**Java 21 + Spring Boot 3.5.16 + Spring Security + Spring JDBC + Flyway.** Arquitectura de monolito modular con eventos internos, API REST y almacenamiento de imagenes intercambiable.

> Estado de verificacion: se reviso la sintaxis de todos los archivos Java y se ejecutaron las reglas de dominio independientes de Spring. No fue posible descargar Maven/dependencias ni levantar Spring en el entorno de entrega. Las pruebas de integracion estan escritas, pero pendientes de ejecutar en tu equipo. No se incluye un JAR supuestamente compilado. Consulta `docs/VERIFICACION.md`.

## 1. Arranque rapido en Windows

Instala o utiliza tu **JDK 21**. Verifica en PowerShell:

```powershell
java -version
javac -version
```

Extrae completamente el ZIP. Abre una terminal dentro de `trabajaya-backend` y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

Tambien puedes abrir `iniciar-local.bat`. El puerto es **8080**. La primera ejecucion necesita Internet para descargar Maven y las dependencias. El lanzador utiliza tu Maven si ya existe; de lo contrario descarga Maven 3.9.11 y verifica su SHA-512. Es un lanzador incluido en el proyecto, no el Maven Wrapper oficial.

El perfil predeterminado `local` utiliza una base SQL H2 persistente en `data/` y guarda las imagenes en `uploads/`. No necesitas instalar PostgreSQL ni disponer de Supabase para recorrer el sistema. Los datos no se pierden al reiniciar.

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/actuator/health
```

El primer endpoint identifica la aplicacion y el proveedor de archivos. Actuator verifica tambien la conexion SQL. Deten el proceso con Ctrl+C.

**En IntelliJ:** abre `pom.xml` como proyecto Maven, selecciona JDK 21, espera la sincronizacion y ejecuta `com.trabajaya.TrabajaYaApplication`. Usa la carpeta del backend como directorio de trabajo. Las migraciones se aplican automaticamente.

## 2. Iniciar el frontend

Extrae el ZIP del frontend en otra carpeta. Abre una segunda terminal y ejecuta `node server.mjs` en esa carpeta. Entra a **http://localhost:5173**. El frontend envia `/api` al backend de 8080 mediante su proxy local.

No abras `index.html` mediante doble clic ni Live Server: ese modo no incluye el proxy de API.

### Cuentas ficticias del perfil local

| Rol | Correo | Clave |
|---|---|---|
| Cliente | cliente@trabajaya.demo | Demo2026! |
| Trabajador | trabajador@trabajaya.demo | Demo2026! |
| Segunda trabajadora | ana@trabajaya.demo | Demo2026! |
| Administrador | admin@trabajaya.demo | Demo2026! |

Solo se crean al iniciar una base local vacia. Tambien puedes registrarte normalmente. Los ejemplos estan en Pasto y usan direcciones de demostracion, no domicilios reales. No publiques este modo en Internet.

Para demostrar conversaciones, abre dos ventanas o perfiles del navegador. Cada pestaña conserva una sesion separada; una pestaña duplicada puede copiar la sesion inicial, por lo que conviene cerrar sesion en ella e ingresar con la otra cuenta.

## 3. PostgreSQL

Consulta **`docs/CONFIGURACION.md`** para utilizar tu instalacion existente (puerto habitual 5432), o el contenedor incluido (puerto local 5433).

Configuracion minima en PowerShell para una base ya creada:

```powershell
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5432/trabajaya"
$env:DB_USERNAME="trabajaya"
$env:DB_PASSWORD="TU_CLAVE_LOCAL"
$env:ADMIN_EMAIL="admin@tu-proyecto.local"
$env:ADMIN_PASSWORD="CAMBIA_EstaClave_2026"
.\mvnw.cmd spring-boot:run
```

El perfil `postgres` no crea las cuentas ficticias por defecto. Puedes habilitarlas expresamente en tu base de pruebas con `$env:DEMO_SEED="true"`. El administrador inicial se crea con las variables anteriores; no hay registro publico de administradores.

No edites una migracion ya aplicada. Los cambios posteriores deben ir en `V2__...sql`, `V3__...sql`, etc. No se utiliza `ddl-auto=create`.

## 4. Supabase Storage

La integracion esta escrita, pero **no esta conectada a una cuenta real**: debes proporcionar tu URL, una clave de servidor y un bucket privado. Sigue `docs/CONFIGURACION.md`.

- PostgreSQL: usuarios, trabajos, mensajes, reputacion y referencias de archivos.
- Supabase Storage: los archivos de imagen.
- El navegador no recibe la clave de Supabase. Solicita las imagenes al backend, que comprueba permisos.

**Storage no es una base NoSQL.** La necesidad de guardar imagenes no obliga a incorporar una segunda base no relacional. Si una rubrica exige expresamente una base NoSQL, esta arquitectura por si sola no satisface ese requisito.

## 5. Funciones incluidas

Registro separado cliente/trabajador, login/logout, recuperacion de clave (consola local o SMTP), perfiles, habilidades, disponibilidad y radio; publicaciones con fotos y ubicacion privada; edicion/cancelacion antes de comenzar; precio manual/automatico con historial; coincidencias por habilidad y proximidad/ciudad; cupos concurrentes y precio fijo por aceptacion; seguimiento por trabajador; chat persistente con imagenes y lectura; notificaciones; confirmacion de cierre; calificacion bilateral; reportes; administracion de usuarios, publicaciones y categorias; registro de eventos y moderacion.

El chat y los avisos se actualizan mediante consultas periodicas (polling), no WebSocket. No requieren Redis, Kafka, RabbitMQ ni un servidor adicional de mensajeria.

**Fuera de esta version:** pasarela/retencion/reembolso de dinero (HU22-HU24, fase futura), verificacion documental de identidad, certificacion profesional, seguimiento GPS continuo, notificaciones push y analisis antivirus. No se representan como funciones reales.

## 6. Estructura

```text
src/main/java/com/trabajaya/
  config/        Seguridad, CORS, salud y datos locales
  users/         Cuentas, sesiones, recuperacion y perfiles
  jobs/          Trabajos, precios, cupos y estados
  messaging/     Conversaciones privadas por participacion
  reputation/    Calificaciones bilaterales
  moderation/    Reportes y panel administrativo
  notifications/ Avisos persistentes y lectura
  storage/       Puerto ObjectStorage + adaptadores local/Supabase
  events/        Eventos de dominio y registro transaccional
  shared/        SQL, errores, identidad y geodistancia
src/main/resources/db/migration/
  V1__schema.sql
src/test/java/com/trabajaya/
  JobRulesTest.java
  FlowIntegrationTest.java
```

Se utiliza JDBC con consultas parametrizadas y bloqueo de filas, no entidades JPA. La separacion por negocio permite evolucionar los modulos sin distribuir prematuramente el sistema. Los controladores administrativos pequenos usan el acceso SQL compartido; no se trata de microservicios ni de aislamiento estricto con Spring Modulith.

## 7. Pruebas y empaquetado

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
java -jar target\trabajaya-backend-1.0.0.jar
```

Los tests Spring usan una base H2 en memoria y archivos en `target/test-uploads`; no modifican tu base de uso normal. Para una prueba de compatibilidad PostgreSQL, inicia ese perfil y ejecuta el recorrido de `docs/EXHIBICION.md`. H2 no sustituye probar contra PostgreSQL real.

Para ejecutar solo las reglas puras sin Maven:

```powershell
New-Item -ItemType Directory -Force target\domain-check | Out-Null
javac -d target\domain-check src\main\java\com\trabajaya\jobs\JobRules.java tools\DomainSmoke.java
java -cp target\domain-check DomainSmoke
```

## 8. Documentacion

- `docs/CONFIGURACION.md`: PostgreSQL, Supabase, SMTP y variables.
- `docs/ARQUITECTURA.md`: modulos, eventos, datos, estados y limites.
- `docs/API.md`: rutas, cuerpos, seguridad y codigos de respuesta.
- `docs/EXHIBICION.md`: recorrido local y casos para demostrar.
- `docs/VERIFICACION.md`: pruebas ejecutadas y pendientes.
- `requests/trabajaya.http`: solicitudes de ejemplo para IntelliJ/REST Client.

## Solucion de problemas

- **Java incorrecto:** configura JAVA_HOME al JDK 21 y revisa el Maven Runner de IntelliJ.
- **No descarga dependencias:** revisa Internet/proxy y Maven Central; no borres versiones aleatoriamente.
- **Puerto 8080 ocupado:** `$env:PORT="8081"` en backend y `API_TARGET=http://127.0.0.1:8081` en `.env` del frontend.
- **Base H2 bloqueada:** cierra la otra instancia que usa `data/`.
- **GPS denegado:** escribe direccion y ciudad; sin coordenadas se usa coincidencia por ciudad.
- **Sin oportunidades:** revisa ciudad, habilidades, disponibilidad, fecha futura, cupos y filtros.
- **Sin correo de recuperacion local:** busca `SOLO DESARROLLO - Recuperacion` en la consola del backend. En PostgreSQL configura SMTP.
- **Imagenes de otro proveedor:** cambiar a Supabase no migra automaticamente lo que ya existe en disco; consulta la guia.
