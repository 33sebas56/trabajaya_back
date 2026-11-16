# Decisiones de esta consolidacion

Estas decisiones describen lo que tiene el código recibido y los ajustes de esta
entrega. No son actas fechadas retrospectivamente del desarrollo original.

## D01. Monolito modular con eventos internos

Mantengo un despliegue Spring Boot y separo responsabilidades por negocio. Para un
proyecto individual evita operar varios servicios y colas. La consecuencia es que
los módulos comparten proceso y base; los eventos son síncronos y no durables.

## D02. SQL para negocio y Storage para archivos

Usuarios, acuerdos, mensajes y reputación necesitan relaciones y transacciones.
Las imágenes van a un bucket privado, con referencias SQL. Supabase Storage no se
presenta como una base NoSQL necesaria para el sistema. Un fallo entre ambos
proveedores se compensa cuando es posible; no hay atomicidad distribuida.

## D03. Conservar el contrato y la migración

No cambio los nombres de las 42 rutas ni el formato principal consumido por el
frontend. V1 conserva su checksum. Los índices se agregan en V2. Se evita perder
los datos y se reduce el trabajo posterior del frontend; aún debe verificarse el
flujo conectado a esta versión.

## D04. Sesion opaca revocable

Mantengo las sesiones existentes en SQL en lugar de cambiar a JWT. Así se pueden
revocar al cerrar sesión o suspender una cuenta. Tiene el costo de una consulta de
autenticación y no ofrece escalado sin base. La contraseña respeta el límite de
bytes de BCrypt; no se almacena en texto plano.

## D05. Historial no equivale a permiso permanente

Un trabajador retirado necesita consultar lo que ocurrio, pero no conservar acceso
a la dirección. Se separan hasParticipation e isAssigned. El historial aparece en
mis trabajos y los datos privados se filtran aparte.

## D06. Errores utiles sin detalles internos

Las respuestas conservan message y los campos de validación, agregando estado,
código y requestId donde corresponde. Se preservan 404/405/415 para fallos HTTP.
Errores de proveedor se traducen a respuestas controladas sin copiar sus cuerpos
ni credenciales. El filtro de seguridad también responde JSON.

## D07. SDD sobre un proyecto existente

La especificación cubre una consolidacion acotada. Constitution, spec, plan y tasks
se versionan junto al código. La estructura es compatible con Spec Kit, pero el
uso real del CLI/agente y la evidencia Git deben generarse en el repositorio del
estudiante; no se sustituyen con documentos que afirmen ejecuciones inexistentes.

## D08. Despliegue gratuito y alcance academico

Render, Aiven y Supabase pueden usarse sin contratar planes de pago dentro de sus
límites. No prometo disponibilidad permanente, temporizadores puntuales durante
suspensión ni correo SMTP en puertos bloqueados. La documentación deja separados
los servicios preparados de los que ya hayan sido conectados y verificados.
