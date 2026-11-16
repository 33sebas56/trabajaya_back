# TrabajaYA | Backend

Aquí presento el backend de TrabajaYA, mi proyecto para conectar a personas que
necesitan resolver un trabajo con trabajadores independientes. Lo que quería hacer
no era otro directorio de tecnicos: el cliente pública la necesidad y propone un
pago; si nadie acepta, puede aumentar la oferta sin superar su presupuesto.

Me interesaba que esa idea tuviera reglas reales detrás de la pantalla. Por eso el
precio queda acordado para cada trabajador cuando acepta, los cupos no se pueden
ocupar dos veces y la dirección completa no se muestra a cualquiera.

Esta entrega corresponde **solo al backend**. El frontend va en otro repositorio.
Estoy llevando el proyecto de forma individual y organizo esta consolidacion con
SDD, conservando el trabajo que ya existia. No presento estos archivos como si fueran
el historial original de desarrollo.

## Que hace

El sistema tiene registro e inicio de sesión para clientes y trabajadores, perfiles
con habilidades y disponibilidad, publicaciones con fotografias, filtros por zona,
precios manuales o automáticos y aceptacion de cupos. Después del acuerdo permite
conversar, avanzar por los estados del trabajo, confirmar su finalizacion y calificar
al otro participante. También tiene reportes, notificaciones y administracion.

El chat es persistente y se consulta por HTTP; no es WebSocket. Los eventos son
internos al mismo backend, no un sistema de microservicios ni una cola externa.

**No incluye pagos reales**, retención de dinero, devoluciones, verificación de
identidad ni seguimiento GPS continuo. Prefiero dejar ese alcance claro antes que
mostrar como terminado algo que todavía no hace el sistema.

## Como lo organicé

Uso Java 21 y Spring Boot 3.5.16. Mantengo Spring Security, validación de entradas,
JDBC para SQL, Flyway para las migraciones y Actuator para consultar la salud.

```text
src/main/java/com/trabajaya/
  users/           Acceso, sesiones, perfiles y limite de intentos
  jobs/            Publicaciones, precios, cupos y estados
  messaging/       Conversaciones por asignacion
  reputation/      Calificaciones
  notifications/   Avisos del sistema
  moderation/      Reportes y funciones de administrador
  storage/         Imagenes locales o Supabase Storage
  events/          Eventos internos, auditoria y avisos
  config/          Seguridad y configuracion del despliegue
  shared/          SQL, errores y utilidades pequenas
```

Lo mantuve como **monolito modular**. Los controladores reciben solicitudes; los
servicios aplican permisos, reglas y transacciones. Las consultas usan parametros,
no concatenacion de datos del usuario. La autenticación usa tokens de sesión opacos
revocables: no estoy usando JWT ni Supabase Auth.

PostgreSQL en Aiven guarda los datos del negocio. Supabase Storage guarda las
imágenes en un bucket privado; no lo uso como otra base de datos de usuarios.

## Lo que cambie en esta revisión

Separe servicios que antes estaban mezclados en controladores, mejore el manejo de
errores y agregue un identificador por solicitud. También revise las contraseñas,
la paginacion y el acceso al historial de trabajadores que se retiran.

En las imágenes hay validación del contenido, límites de dimensiones y conversion
a PNG. Si falla el registro SQL después de subir un objeto, el backend intenta
eliminarlo del almacenamiento. Eso reduce archivos huerfanos, pero no equivale a
una transacción distribuida garantizada.

Las **42 rutas HTTP de la aplicación** se conservaron. La migración V1 sigue intacta
y la V2 agrega índices. No hace falta vaciar una base existente. Los detalles estan
en [compatibilidad](docs/COMPATIBILIDAD.md).

## SDD y archivos de la entrega

Aquí deje las reglas, el alcance, el plan y las tareas junto al código. Son archivos
Markdown navegables en GitHub, no documentos aparte que puedan quedar desactualizados.

| Archivo | Que explica |
|---|---|
| [Constitución](.specify/memory/constitution.md) | Principios y restricciones reales |
| [Especificación](specs/001-backend-sdd/spec.md) | Historias, criterios de aceptacion y Non-Goals |
| [Plan](specs/001-backend-sdd/plan.md) | Enfoque técnico de la consolidacion |
| [Tareas](specs/001-backend-sdd/tasks.md) | Cambios incluidos y acciones pendientes |
| [Decisiones](docs/DECISIONES.md) | Motivos y consecuencias de las decisiones |
| [Trazabilidad](docs/TRAZABILIDAD.md) | Relacion entre rúbrica, código y pruebas |
| [Uso de Spec Kit](docs/SDD.md) | Como continuar el proceso sin inventar evidencia |

La estructura esta preparada para trabajar con Spec Kit. **No incluyo una ejecución
del CLI ni conversaciones del agente que no se hayan realizado**. Esa evidencia y
los commits/pull requests reales se completan en mi repositorio. La aceptacion de
la modalidad individual corresponde al docente.

## Despliegue gratuito

La ruta principal es Render Free + Aiven PostgreSQL + Supabase Storage. Dejo el
`Dockerfile`, el perfil `render` y un `render.yaml` opcional. No necesito instalar
Docker en mi computador para que Render construya la imagen desde GitHub.

1. Subir este código a una rama del repositorio, conservando mi carpeta `.git`.
2. Crear un Web Service de tipo Docker, plan Free, con la carpeta del `pom.xml` como raiz.
3. Configurar las variables de `.env.example` y el certificado de Aiven en Render.
4. Activar `SPRING_PROFILES_ACTIVE=postgres,render` y comprobar la URL pública.

La guia completa esta en [despliegue](docs/DESPLIEGUE.md). **Las credenciales se
configuran después**: el ZIP no contiene contraseñas reales ni conexiones activas
a las cuentas de Aiven o Supabase. `.env.example` es una plantilla; Spring no carga
ese archivo automáticamente.

El endpoint `/actuator/health` verifica la aplicación y su acceso a la base. El
endpoint `/api/health` informa el proveedor de almacenamiento, pero no comprueba
por si solo una subida a Supabase. La raiz `/` no contiene el frontend.

## Pruebas automatizadas

Para revisar el backend uso GitHub Actions. La ejecución
[ingenieria_web #1](https://github.com/33sebas56/trabajaya_back/actions/runs/43790892364), correspondiente al commit
`ea16f66`, terminó con estado **Success**.

| Etapa | Resultado publicado |
|---|---|
| Compilación y suite con H2 | Aprobado |
| Suite con PostgreSQL 18 temporal | Aprobado |
| Construcción de la imagen Docker | Aprobado |

Aquí muestro el [registro de la ejecución](docs/EVIDENCIAS_PRUEBAS.md), con
los enlaces verificables y los nombres de los reportes generados. Los resultados
individuales se conservan en los artefactos de Actions; la verificación del
servicio publicado se registra por separado.

El [workflow](.github/workflows/ci.yml) se ejecuta al subir cambios a `main`, en
pull requests o mediante **Actions > Run workflow**. No necesita credenciales
de Aiven ni Supabase: usa bases y almacenamiento de pruebas dentro del runner.

Para ejecutarlo fuera de GitHub se necesita JDK 21 y Maven 3.9.x:

```bash
mvn -B -ntp verify
mvn spring-boot:run
```

También están `mvnw` y `mvnw.cmd`: son lanzadores del proyecto que descargan Maven
si no está instalado. El perfil local opcional usa H2 y no crea cuentas demo por defecto.

## Demostración del proyecto

Aquí muestro el recorrido que representa la idea de TrabajaYA: un cliente publica
una necesidad, aumenta la oferta, un trabajador acepta, ambos se comunican,
registran el avance y cierran el trabajo con una calificación.

La [guía de demostración](docs/EXHIBICION.md) y las
[solicitudes HTTP](requests/trabajaya.http) permiten recorrer las funciones del
backend. La organización de las ramas y los cambios está en
[contribución](CONTRIBUTING.md).

## Alcance del entorno académico

El despliegue está preparado para Render Free, Aiven y Supabase. El correo de
recuperación se mantiene desactivado en esta configuración. El programador de
precios depende de que el proceso esté activo; Render Free puede suspenderlo por
inactividad. Los detalles técnicos están en la
[guía de despliegue](docs/DESPLIEGUE.md).
