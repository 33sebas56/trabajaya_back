# Arquitectura del backend

## Modelo

Monolito modular en un proceso Spring Boot. No se promete independencia de
despliegue entre paquetes ni se usan Kafka/RabbitMQ. La separación de controladores
y servicios delimita responsabilidades; el acceso SQL compartido sigue en `Db` y
no todos los módulos tienen repositorios exclusivos.

```text
Frontend / cliente HTTP
       | JSON + Bearer
       v
SecurityFilterChain -> controladores -> servicios transaccionales
                                       |              |
                                       v              v
                                  PostgreSQL       eventos internos
                                     Aiven        auditoria + avisos
                                       |
                                metadatos de media
                                       |
                     MediaService -> ObjectStorage -> Supabase privado
```

## Módulos

| Modulo | Responsabilidad |
|---|---|
| users | BCrypt, tokens opacos, restablecimiento, perfiles, habilidades y disponibilidad |
| jobs | Validaciones, SQL de oportunidades, acuerdos, límites de precio y estados |
| messaging | Mensajes y marca de lectura por asignación |
| reputation | Valoracion única por autor/asignación confirmada |
| moderation | Reportes, suspensión de cuentas/publicaciones y categorías |
| notifications | Consulta y lectura de avisos propios |
| events | Eventos síncronos y registro de auditoría |
| storage | Sanitizacion, permisos, metadatos y adaptadores de objetos |
| shared/config | Errores, trazas por requestId, consultas parametrizadas y entorno |

## Identidad

El token opaco se entrega al iniciar sesión. Su huella se guarda en `auth_sessions`;
la contraseña se guarda con BCrypt. Las sesiones se pueden revocar. La API no usa
cookies de autenticación y por eso no depende de CSRF basado en cookies. CORS no
sustituye las comprobaciones de rol o propietario.

El limitador de autenticación usa memoria acotada y ventanas temporales. No confia
en X-Forwarded-For enviado por cualquiera. Detras de un proxy varias personas pueden
compartir el límite por origen; no es un control distribuido ni una solucion completa
contra abuso. Las entradas sensibles nunca se escriben completas en logs.

## Contratacion y concurrencia

La fila del trabajo se bloquea al aceptar y al modificar precios. El backend
comprueba cupos, elegibilidad, estado y expectedPrice antes de guardar la asignación.
El precio acordado pertenece a la asignación y no cambia con aumentos posteriores.
Los importes son enteros COP por persona; no hay calculos de cobro real.

Los estados tienen transiciones validas: ACEPTADO, EN_CAMINO, EN_LUGAR, INICIADO,
TERMINADO y CONFIRMADO. Retirarse antes de empezar libera el cupo. El cliente confirma
cada participación; el agregado del trabajo depende de todas ellas.

## Privacidad e historial

El cliente propietario y los participantes autorizados reciben los datos privados.
El presupuesto máximo no se pública a candidatos. Un trabajador retirado conserva
su propia participación y puede reportar, pero ya no recibe dirección exacta ni
coordenadas privadas. Los perfiles ajenos no entregan correo ni telefono.

## Eventos

Los eventos se publican dentro del proceso mediante Spring y se atienden de forma
sincrona. La auditoría y los avisos de esos eventos usan la transacción de negocio.
Si un listener sincrono lanza una excepcion, la operación puede revertirse. Esto no
es un outbox ni un bus durable; una caida antes del commit no garantiza una entrega
externa. La lista de destinatarios del evento es inmutable.

## Imágenes y límite transaccional

Se comprueba la firma/formato decodificable, hasta 5 MiB de entrada y 4096 por lado,
con un máximo de 16 millones de pixeles. Se recodifica PNG y se limita el resultado
a 8 MiB. El objeto usa una clave generada, no el nombre de archivo enviado.

La subida a Storage no participa de la transacción PostgreSQL. Se registra una
compensación para retirar el objeto si falla la transacción que guarda sus metadatos.
La compensación puede fallar; se registra el problema sin secretos. Una terminacion
abrupta entre pasos todavía puede dejar un objeto huerfano. No se incluye un proceso
automático de reconciliacion en esta entrega.

La asociacion de un avatar bloquea la fila de media y verifica propietario/uso. Un
archivo no se reutiliza simultaneamente como avatar y adjunto de trabajo/chat. Leer
la ruta de media requiere autorización aunque se conozca su UUID.

## Operacion

Flyway aplica V1 intacta y V2 de índices. El perfil de Render usa TLS verify-full,
3 conexiones SQL por defecto, almacenamiento externo y ningun seed. El identificador
X-Request-ID ayuda a buscar un fallo sin mostrar una traza interna al cliente.

El despliegue gratuito no garantiza ejecución continua del scheduler. La aplicación
no incluye verificación documental, pagos, SLA, auditoría externa inmutable ni alta
disponibilidad. Son límites explicitos, no funciones ocultas.
