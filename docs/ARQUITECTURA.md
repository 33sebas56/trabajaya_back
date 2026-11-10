# Arquitectura y decisiones

## Monolito modular con eventos internos

```text
Frontend HTML/CSS/JS (5173)
           |
     Proxy /api local
           |
Spring Boot REST (8080)
  Security -> principal autenticado -> autorizacion de negocio
           |
  users -- jobs -- messaging -- reputation -- moderation
           |               |
  transacciones SQL      storage.ObjectStorage
           |               +-- LocalObjectStorage
  H2 / PostgreSQL          +-- SupabaseObjectStorage
           |
  DomainEvent -> EventRecorder -> domain_events + notifications
```

El empaquetado es por capacidad de negocio, no una carpeta global de todos los controladores. Es un solo proceso desplegable, no microservicios. Se usan JDBC y NamedParameterJdbcTemplate con valores parametrizados. Los identificadores SQL no provienen de texto del usuario.

`DomainEvent` comunica publicacion, aumento, aceptacion, avance, mensaje, reputacion y moderacion. `EventRecorder` es un listener sincronico de Spring; se ejecuta dentro de la transaccion que publica el evento. Un fallo al registrar la notificacion impide confirmar esa transaccion. No se promete entrega distribuida: no hay broker ni cola durable independiente ni outbox asincrono. La tabla de eventos es un registro operativo, no event sourcing.

## Datos

`users`, `user_skills`, `categories`: identidades y habilidades.
`auth_sessions`, `password_resets`: hashes de tokens y vencimientos.
`jobs`, `price_history`: publicaciones y cambios de oferta.
`assignments`: un cupo por trabajador; el precio aceptado es inmutable.
`job_events`: historial funcional del trabajo.
`messages`: conversacion por asignacion.
`media`: metadatos y referencias; los bytes estan en disco o Storage.
`reviews`: una opinion de cada participante hacia la contraparte.
`reports`: casos con estado y respuesta administrativa.
`domain_events`, `notifications`: eventos de negocio y avisos.

Las tablas se crean con Flyway. Las relaciones tienen claves foraneas. Se conserva el historial: no se borran cuentas o trabajos desde la interfaz, se suspenden/cancelan.

## Estados y cupos

Asignacion: `ACEPTADO -> EN_CAMINO -> EN_LUGAR -> INICIADO -> TERMINADO -> CONFIRMADO`.

El trabajador avanza hasta TERMINADO. Solo el cliente confirma. No se inicia la ejecucion hasta completar los cupos requeridos. El trabajador puede liberar un cupo antes de iniciar, siempre que nadie haya comenzado. El cliente puede cancelar antes del inicio; despues debe reportar el problema.

Trabajo: PUBLICADO (cupos pendientes), ASIGNADO (cupos completos), EN_CURSO, POR_CONFIRMAR y COMPLETADO. CANCELADO y VENCIDO cierran publicaciones. Un trabajo vence automaticamente al pasar su fecha si no tuvo participantes. Si quedaron cupos parciales, se conserva el acuerdo y el cliente puede cancelarlo; no se incorporan mas personas despues de la fecha. No se recalcula silenciosamente el alcance para reducir cupos.

La aceptacion, el aumento y los avances bloquean la fila del trabajo (`SELECT ... FOR UPDATE`) en una transaccion. Dos solicitudes por el ultimo cupo se serializan. El cliente no puede editar descripcion, participantes ni ubicacion despues de una aceptacion. El trabajador envia `expectedPrice`: si cambio desde que lo vio, recibe 409 y debe revisar antes de aceptar.

## Precio dinamico

Importes enteros COP por persona. El cliente configura inicial, tope, incremento e intervalo. Cada cinco segundos el servidor busca publicaciones cuyo aumento corresponde; el minimo intervalo configurable es un minuto. Cada ejecucion aplica como maximo un incremento y programa el siguiente desde ese momento; no aplica de golpe todos los intervalos durante los que estuvo apagado.

El aumento se limita al presupuesto maximo y solo afecta cupos pendientes. Ejemplo: primer trabajador acepta 60.000; la oferta sube a 80.000; el segundo acepta 80.000. El primer acuerdo sigue en 60.000. El presupuesto maximo del cliente no se entrega al trabajador en la API.

## Ubicacion y privacidad

La exploracion devuelve zona/ciudad y, cuando ambas partes proporcionan coordenadas, una distancia aproximada. Las coordenadas y direccion exactas se entregan solamente al cliente, administrador o trabajadores con asignacion vigente. No se manda la direccion para esconderla solamente con CSS.

Con coordenadas se aplica distancia geodesica aproximada en SQL antes de paginar; sin ellas se compara ciudad. No es ruta por calles, tiempo de viaje ni seguimiento continuo. El enlace a mapas se genera al consultar la direccion autorizada.

Las fotografias de una publicacion son visibles para quienes pueden ver esa publicacion; pueden revelar informacion por su contenido, por lo que se advierte al cliente que no adjunte datos sensibles. El backend recodifica imagenes para descartar metadatos; esto no garantiza detectar contenido sensible ni malware.

## Seguridad

Contrasenas BCrypt; tokens aleatorios de sesion almacenados como SHA-256 en SQL; expiracion y revocacion. Son tokens opacos, no JWT. SessionStorage en el frontend separa sesiones por pestaña. La sesion nunca depende de un rol enviado por el navegador. Registro publico solo permite CLIENTE/TRABAJADOR.

Se usa Spring Security para exigir autenticacion y rol administrativo. Cada operacion comprueba pertenencia al trabajo o conversacion. El frontend escapa texto del usuario antes de insertar HTML. El servidor frontend restringe archivos servidos y define CSP. CSRF esta deshabilitado porque la API usa Bearer explicito, no cookies de autenticacion. Para exponer en Internet hacen falta HTTPS, revision de seguridad, politicas definitivas y un analisis del modelo de sesion.

Hay limites locales para login/registro/recuperacion, no un sistema antiabuso distribuido. No existe verificacion real de identidad ni se muestra como si existiera. Las habilidades son autodeclaradas.

## Limites de esta entrega

No pagos (HU22-HU24), escrow, devoluciones, validacion documental, push, WebSocket, GPS continuo, certificados tecnicos ni sistema de emergencias. Los reportes aceptan texto asociado al trabajo; no tienen adjuntos propios (las fotos estan en publicaciones y chat). No hay indicadores de puntualidad inventados. No se implemento un algoritmo ML de precio: el cliente decide y el servidor aplica reglas deterministas.

No se ha ejecutado una auditoria profesional, prueba de carga, integracion PostgreSQL remota ni integracion con una cuenta Supabase real. Esta version es para desarrollo y exhibicion local, no un servicio comercial listo para operar.
