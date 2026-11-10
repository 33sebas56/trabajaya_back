# API REST | TrabajaYA

Base local: `http://localhost:8080/api`. JSON UTF-8. Los cuerpos de solicitud usan camelCase; las respuestas de SQL usan snake_case. Importes enteros COP por persona. Fechas de solicitudes ISO 8601 con zona, por ejemplo `2030-05-20T09:00:00-05:00` (elige una fecha futura).

Excepto las rutas publicas, incluye `Authorization: Bearer <token>`. El token se recibe al iniciar sesion/registrarse y es revocable. El frontend ya implementa este contrato.

## Publicas

| Metodo | Ruta | Entrada / salida |
|---|---|---|
| GET | /health | Estado de la aplicacion, storage, demo |
| GET | /categories | Array de categorias activas |
| POST | /auth/register | 201, token + perfil |
| POST | /auth/login | email, password -> token + perfil |
| POST | /auth/forgot-password | email -> mensaje generico |
| POST | /auth/reset-password | token, password -> confirmacion |

Registro:
```json
{"name":"Persona de prueba","email":"persona@example.com","password":"Clave2026!","role":"TRABAJADOR","phone":"3000000000","city":"Pasto","skills":[1,5],"experience":3,"radiusKm":20,"acceptedTerms":true}
```

Para cliente: role CLIENTE, skills vacio. Nunca ADMIN. El correo debe ser unico.

## Perfil y sesion

| Metodo | Ruta | Uso |
|---|---|---|
| POST | /auth/logout | Revocar token actual |
| GET | /me | Mi perfil completo |
| PUT | /me | Actualizar perfil; no cambia email ni rol |
| GET | /users/{id} | Perfil publico autenticado, sin correo/telefono/coordenadas |
| GET | /dashboard | Contadores de la cuenta actual |

PUT /me:
```json
{"name":"Persona de prueba","phone":"3000000000","city":"Pasto","bio":"Trabajo con instalaciones y reparaciones.","experience":3,"skills":[1,5],"radiusKm":20,"available":true,"latitude":null,"longitude":null,"avatarId":null}
```

## Trabajos

GET `/jobs?mine=false&category=1&minimum=50000&city=Pasto&date=2030-05-20&page=0&size=12` devuelve `{items,total,page,size}`. Fecha y filtros son opcionales. `mine=true` lista propios/acordados. El servidor aplica habilidades y zona del trabajador; no confia en los filtros del cliente.

GET `/jobs/{id}` devuelve detalle, oferta, fotos, historial y asignaciones visibles segun permisos. POST `/jobs` (cliente) devuelve 201:
```json
{"title":"Instalar dos lamparas","description":"Instalar dos lamparas con materiales disponibles. Se requiere herramienta propia.","categoryId":1,"city":"Pasto","zone":"Centro","address":"DIRECCION PRIVADA DE PRUEBA","latitude":null,"longitude":null,"scheduledAt":"2030-05-20T09:00:00-05:00","durationHours":2,"workersNeeded":1,"initialPrice":60000,"maxPrice":100000,"incrementAmount":10000,"intervalMinutes":10,"autoIncrease":false,"mediaIds":[]}
```

PUT `/jobs/{id}` permite los campos descriptivos/ubicacion/fecha/cupos/mediaIds del cuerpo anterior, pero no la estrategia de precio; solo antes de una aceptacion.

| Metodo | Ruta | Cuerpo |
|---|---|---|
| POST | /jobs/{id}/increase | {"amount":10000} |
| POST | /jobs/{id}/accept | {"expectedPrice":70000} |
| POST | /jobs/{id}/cancel | {"reason":"Ya no necesito realizar el trabajo."} |
| POST | /assignments/{id}/status | {"status":"EN_CAMINO"} |
| POST | /assignments/{id}/review | {"score":5,"comment":"Cumplio lo acordado."} |

Estados de participacion: ACEPTADO, EN_CAMINO, EN_LUGAR, INICIADO, TERMINADO, CONFIRMADO. CANCELADO permite liberar cupo solo antes de comenzar. Confirmacion exclusiva del cliente. Calificacion unica por autor/participacion confirmada.

## Archivos y conversaciones

POST `/media`: multipart/form-data con campo **file**, PNG/JPEG <=5MB. Devuelve 201 `{id,url,contentType,size}`. Adjunta ese id a mediaIds, avatarId o mediaId. GET `/media/{id}` devuelve bytes PNG con autorizacion; no es una URL publica para copiar sin token.

| Metodo | Ruta | Cuerpo / salida |
|---|---|---|
| GET | /assignments/{id}/messages?after=0 | Array hasta 100 mensajes, orden ascendente |
| POST | /assignments/{id}/messages | {"body":"Voy en camino.","mediaId":null} |
| POST | /assignments/{id}/messages/read | Marca mensajes recibidos |
| GET | /assignments/{id}/messages/read-status | {"last_read_id":123} de mensajes propios leidos |

Solo cliente y trabajador de esa participacion acceden. Una conversacion por asignacion, no un chat publico de todos los candidatos. Si se confirma/cancela, queda solo lectura. Los datos son persistentes; el frontend consulta novedades cada cinco segundos.

## Avisos y reportes

GET `/notifications` -> `{items,unread}`; POST `/notifications/{id}/read`; POST `/notifications/read-all`.

GET `/reports` -> reportes del autor. POST `/reports`:
```json
{"jobId":1,"reason":"INCUMPLIMIENTO","detail":"Descripcion de al menos diez caracteres."}
```

Motivos: INCUMPLIMIENTO, SEGURIDAD, FRAUDE, OTRO. No adjuntos propios ni reembolsos.

## Administracion

Todas requieren ADMIN.

| Metodo | Ruta | Uso |
|---|---|---|
| GET | /admin/summary | Contadores |
| GET | /admin/users?page=0 | Array de 100 usuarios como maximo |
| PATCH | /admin/users/{id} | {"active":false,"reason":"Motivo de la medida"} |
| GET | /admin/jobs?page=0 | Array de publicaciones |
| POST | /admin/jobs/{id}/hide | {"active":false,"reason":"Motivo de moderacion"} |
| GET | /admin/reports | Hasta 100 casos recientes |
| PATCH | /admin/reports/{id} | {"status":"RESUELTO","resolution":"Respuesta al caso"} |
| GET | /admin/categories | Categorias activas e inactivas |
| POST | /admin/categories | {"name":"Nuevo oficio","active":true} |
| PUT | /admin/categories/{id} | Mismo cuerpo anterior |
| GET | /admin/audit?page=0 | Eventos con responsable y fecha |

La suspension de usuario invalida sesiones. La suspension de publicacion detiene nuevos acuerdos y avances; el historial permanece. La reactivacion de publicaciones requiere revision administrativa adicional: el panel actual no la expone.

## Errores

400 validacion/formato; 401 sesion requerida o vencida; 403 sin permiso; 404 no visible/no encontrado; 409 conflicto de cupo, precio, estado o duplicado; 413 imagen grande; 429 demasiados intentos; 500 fallo no previsto con referencia de soporte.

La respuesta de validacion incluye `{message,fields}`. No se entregan stack traces, contrasenas ni claves de proveedores al navegador.
