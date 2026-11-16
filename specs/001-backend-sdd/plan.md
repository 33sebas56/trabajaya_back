# Plan técnico

## Base
Java 21, Spring Boot 3.5.16, Spring Security, JDBC, Flyway y PostgreSQL. Se conserva
la versión de Spring que ya arrancaba; no se introduce una migración de versión mayor.
La API mantiene sus rutas y nombres de campos. H2 queda aislado para pruebas/desarrollo.

## Orden de implementacion
1. Inventariar el ZIP sin transportar .git, IDE, bases locales ni secretos.
2. Fijar alcance y criterios; conservar V1__schema.sql byte a byte.
3. Separar logica HTTP de servicios en perfiles, chat, reputación y moderacion.
4. Extraer reglas de contraseñas, ventanas de límite y paginacion para probarlas.
5. Normalizar errores y correlacionar solicitudes sin exponer datos internos.
6. Endurecer acceso a imágenes/asociaciones y compensación ante fallo SQL.
7. Mantener el bloqueo de cupos, precios y estados; mejorar lectura de historiales.
8. Preparar Docker, perfiles, validación de configuración, plantilla Render y CI.
9. Agregar pruebas, registro honesto de verificación y correspondencia con la rúbrica.

## Transacciones y eventos
Los servicios de escritura son límites @Transactional. EventRecorder usa un listener
sincrono: si falla el registro/notificación, se revierte la operación. No se utiliza
@Async ni se promete entrega externa exactamente una vez. Storage es un sistema
externo: la compensación del objeto ocurre tras una transacción revertida y puede fallar.

## Seguridad y compatibilidad
Bearer explicito, sin autenticación por cookie; el token crudo no se guarda en SQL.
Conservar `message`, `fields` y los campos JSON existentes; los errores pueden agregar
`status`, `code`, `requestId`. Controllers delegan a servicios sin cambiar rutas.
Límites por origen/operación y por identidad se mantienen en memoria, una instancia.
No confiar en X-Forwarded-For arbitrario para controles de autorización.

## Verificación
- Dominio puro: javac/java, sin dependencias externas.
- Contexto Spring: JUnit + MockMvc + H2; no requiere Aiven/Supabase.
- PostgreSQL: misma suite contra servicio efimero en GitHub Actions.
- Storage: dobles de prueba para fallos/compensación; conexión real pendiente.
- Render: Dockerfile preparado, no se afirma un despliegue ejecutado en esta entrega.

## Migracion y reversa
V1 no se altera. V2 solo incorpora índices: no renombra columnas ni elimina filas.
Para revertir código se puede volver al commit anterior manteniendo los índices.
No ejecutar Flyway clean/repair para ocultar errores ni borrar datos de Aiven.
El frontend sigue usando los mismos endpoints; ver docs/COMPATIBILIDAD.md.
