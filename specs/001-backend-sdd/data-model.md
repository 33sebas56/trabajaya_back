# Modelo de datos conservado

La fuente autoritativa es
[V1__schema.sql](../../src/main/resources/db/migration/V1__schema.sql).
La V2 agrega índices sin cambiar columnas ni eliminar datos.

| Grupo | Relacion principal |
|---|---|
| Identidad | users, auth_sessions, password_resets y user_skills |
| Catalogo | categories y habilidades seleccionadas por trabajador |
| Publicaciones | jobs, price_history y job_events |
| Participacion | assignments, con precio acordado por trabajador |
| Comunicacion | messages asociados a una asignación |
| Reputacion | reviews con autor, destinatario y asignación |
| Archivos | media con propietario, clave de objeto y asociacion permitida |
| Moderacion | reports y su resolución |
| Observabilidad | notifications y domain_events |

No existe tabla de pagos implementados. Los bytes de las imágenes no se guardan en
PostgreSQL. Las sesiones son revocables y las contraseñas no se almacenan en texto
plano. Revisar el SQL antes de afirmar nombres o restricciones no listados aquí.
