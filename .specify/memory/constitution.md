# Constitución de TrabajaYA

Versión: 1.0.0. Estado: propuesta de reglas para esta entrega individual.

## 1. Una necesidad concreta
La aplicación conecta clientes con trabajadores independientes. La oferta la pública
el cliente y puede aumentarla hasta su límite. El precio aceptado pertenece a cada
asignación y no se altera al subir el precio de los cupos restantes.

## 2. Alcance y compatibilidad
Conservar Java 21, Spring Boot, JDBC y las rutas `/api` existentes. Las migraciones
aplicadas no se modifican. Las mejoras deben mantener las claves JSON que utiliza
el frontend. No introducir pagos, verificaciones de identidad ni servicios pagos
como si ya estuvieran implementados.

## 3. Arquitectura verificable
Monolito modular por negocio. Los controladores reciben y validan HTTP; los servicios
coordinan reglas y transacciones; los repositorios y el adaptador Db ejecutan SQL
parametrizado. Los eventos internos son síncronos y su auditoría/notificaciones se
confirman junto con la transacción: esto no es una cola distribuida.

## 4. Seguridad desde el servidor
Autenticar sesiones Bearer revocables. Guardar hashes de claves y tokens, nunca los
valores originales. Comprobar permisos antes de operar. La dirección precisa y el
presupuesto máximo no forman parte de la respuesta pública de una oportunidad.
Validar imágenes por contenido, no solo por extension. No versionar secretos,
bases locales, archivos de usuarios ni copias del directorio .git.

## 5. Datos y transacciones
Bloquear el trabajo para aceptar cupos, aumentar ofertas y cambiar estados. No usar
un contador del frontend como autoridad. PostgreSQL es la referencia de despliegue;
H2 sirve solamente como entorno de pruebas. Cambios de esquema nuevos se agregan
con Flyway y con una estrategia de reversa documentada.

## 6. Calidad y evidencia
Todo cambio de comportamiento debe tener un criterio verificable y una prueba.
Distinguir pruebas ejecutadas, pendientes y servicios simulados. Nunca marcar una
conexión a Aiven/Supabase como verificada sin haberla usado. Mantener documentados
los límites de Render Free y la ausencia de correo cuando MAIL_ENABLED=false.

## 7. Trabajo individual y Git
Realizo esta entrega de forma individual. Trabajo en ramas, relaciono cambios con
tareas y reviso un pull request antes de integrarlo. No invento colaboradores,
revisiones, commits ni fechas para simular trabajo grupal. La aceptacion de esta
modalidad debe acordarse con el docente.

## Gobierno
Cambiar este documento exige explicar el motivo, revisar especificación/plan/tareas
y registrar la decision en docs/DECISIONES.md. El ZIP no prueba por si solo un
historial de uso de Spec Kit ni reemplaza los commits y PR reales del repositorio.
