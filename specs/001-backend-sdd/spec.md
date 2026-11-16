# Especificación: consolidacion del backend TrabajaYA

## Contexto y punto de partida
Ya existe un backend de TrabajaYA. Esta especificación describe su consolidacion
para dos entregas academicas; no sostiene que el sistema original se construyo a
partir de estos documentos. El código recibido conserva las funciones de registro,
publicación, precio dinámico, asignaciones, mensajes, reputación y moderacion.

## Objetivo
Entregar un backend organizado, con contratos compatibles, reglas verificables y
configuración de despliegue para Render Free, PostgreSQL/Aiven y Supabase Storage.
La documentación debe ser navegable desde GitHub y explicar decisiones y límites.

## Historias y criterios de aceptacion

### US1 - Identidad y acceso (P1; backlog HU-01 a HU-04)
Como usuario quiero registrarme e iniciar sesión con el rol correcto.
- AC1.1: solo CLIENTE o TRABAJADOR pueden registrarse; ADMIN no es autoseleccionable.
- AC1.2: claves invalidas, correo duplicado y datos incompletos se rechazan sin SQL
  ni secretos en la respuesta; las contraseñas se almacenan con BCrypt.
- AC1.3: cierre y restablecimiento revocan las sesiones correspondientes.
- AC1.4: el límite de solicitudes devuelve 429 sin crecer indefinidamente en memoria.
- AC1.5: no se muestran datos de contacto ni coordenadas propias en perfiles ajenos.

### US2 - Publicaciones y contratacion (P1; HU-05 a HU-14)
Como cliente quiero publicar trabajos y que se acepten con un precio acordado.
- AC2.1: fecha futura, cantidades validas, coordenadas emparejadas y máximo >= inicial.
- AC2.2: solo el propietario edita antes de la primera aceptacion.
- AC2.3: un aumento no supera el máximo y no modifica precios ya acordados.
- AC2.4: dos aceptaciones concurrentes del último cupo producen una sola asignación.
- AC2.5: el precio esperado obsoleto devuelve 409; no se acepta silenciosamente otro.
- AC2.6: solo participantes autorizados reciben la dirección exacta; un trabajador
  que se retira conserva su historial pero pierde acceso a esa dirección.
- AC2.7: las oportunidades aplican habilidades/zona y paginacion sin desbordamientos.

### US3 - Ejecución y confianza (P1; HU-15 a HU-21)
Como participante quiero coordinar, cerrar el trabajo y dejar una valoracion.
- AC3.1: solo participantes de una asignación acceden a su chat; al cancelar o
  confirmar no se envian mensajes nuevos, pero se conserva el historial.
- AC3.2: EN_CAMINO -> EN_LUGAR -> INICIADO -> TERMINADO sigue ACEPTADO, sin saltos;
  solo el cliente confirma y solo se inicia con todos los cupos completos.
- AC3.3: una calificación por autor/asignación, de 1 a 5 y tras confirmar.
- AC3.4: reportes, notificaciones y administracion comprueban identidad y rol.
- AC3.5: cambios relevantes producen auditoría y notificaciones en la misma transacción.

### US4 - Archivos privados y errores claros (P1; HU-06/HU-15)
Como usuario quiero adjuntar imágenes sin publicar datos privados ni archivos ajenos.
- AC4.1: aceptar PNG/JPEG de hasta 5 MiB, limitar dimensiones y recodificar como PNG.
- AC4.2: rechazar archivos falsos y asociaciones de imágenes de otros usuarios.
- AC4.3: no asignar simultaneamente la misma imagen como avatar y adjunto.
- AC4.4: un fallo de Storage devuelve un error controlado sin exponer claves.
- AC4.5: si la transacción SQL de la subida falla, intentar retirar el objeto externo;
  un fallo de compensación se registra sin incluir secretos (no hay atomicidad distribuida).

### US5 - Entrega y operación (P1)
Como desarrollador individual quiero explicar y desplegar el backend sin rehacerlo.
- AC5.1: Dockerfile multi-etapa Java 21, perfil postgres+render, PORT y salud pública.
- AC5.2: credenciales solo por entorno; ejemplos sin valores reales.
- AC5.3: arranque en Render rechaza datos demo y almacenamiento local accidental.
- AC5.4: README, constitución, plan, tareas y trazabilidad coinciden con el código.
- AC5.5: CI preparado para pruebas H2 y PostgreSQL, con resultados descargables.
- AC5.6: se conserva V1 y se agregan cambios de índices en una V2 independiente.

## Non-Goals
- No pagos, comisiones, retenciones, devoluciones ni disputas financieras (HU-22 a 24).
- No GPS continuo, mapas/rutas externos ni certificacion de competencias.
- No nuevo frontend en esta entrega; se conservan sus contratos existentes.
- No correo garantizado sin proveedor, ni SMTP en puertos bloqueados por Render Free.
- No WebSocket: chat y notificaciones se consultan por HTTP con `after` para mensajes.
- No Kafka/RabbitMQ, microservicios, colas durables ni disponibilidad permanente gratis.
- No pruebas contra las cuentas reales del usuario sin credenciales configuradas.
- No inventar evidencia histórica de trabajo en grupo o ejecución de Spec Kit.

## Exito observable
El flujo registro -> publicar -> aumentar -> aceptar -> conversar -> terminar ->
confirmar -> calificar conserva permisos y formatos. La salud verifica la base de
datos; el health informativo de Storage no sustituye una subida real autorizada.
