# Recorrido para mostrar el backend

Este archivo es un guion, no un registro de una presentacion ya realizada. Usar una
base de TrabajaYA, usuarios de demostración creados expresamente y datos no sensibles.
Las solicitudes estan en [trabajaya.http](../requests/trabajaya.http).

## Introduccion

"Aquí presento TrabajaYA. Lo que quería era que una persona publicara una necesidad
con un precio y que un trabajador pudiera aceptarla. Si no encuentra a alguien,
puede mejorar la oferta, pero sin cambiar lo ya acordado con otra persona."

## Flujo público después del despliegue

1. Mostrar `/actuator/health` y `/api/categories`. Explicar que la página principal
   pertenece al frontend, no a la API.
2. Crear cliente y trabajador con correos de demostración distintos. Al trabajador
   asignarle una habilidad/categoría existente y la misma ciudad del trabajo.
3. Publicar un trabajo futuro por 60.000 COP, máximo 100.000 y un cupo.
4. Consultarlo como trabajador: mostrar que no se entrega dirección exacta.
5. Aumentar 10.000 como cliente; aceptarlo como trabajador con expectedPrice 70000.
6. Volver al detalle para mostrar precio acordado y dirección autorizada.
7. Enviar un mensaje y avanzar EN_CAMINO, EN_LUGAR, INICIADO, TERMINADO.
8. Confirmar como cliente y calificar. Consultar el historial.
9. Intentar una acción no permitida, como aceptar otro cupo inexistente, y mostrar
   el error controlado. No exponer un token completo al proyectar la pantalla.

La subida autenticada de una imagen a `/api/media` se muestra cuando Supabase este
configurado; el health informativo no reemplaza esa evidencia.

## Código que explica el flujo

- `jobs/JobService.java`: transacciones y permisos.
- `jobs/JobRules.java`: reglas puras verificables.
- `messaging/ChatService.java`: participantes y mensajes persistentes.
- `storage/MediaService.java`: permisos y compensación de objetos.
- `.specify/` y `specs/`: reglas, alcance, plan y tareas.
- `.github/workflows/ci.yml`: verificación automatizada preparada.

Cerrar explicando las limitaciones: pagos reales fuera de alcance, correo no
conectado, suspensión del plan gratuito y evidencia Git/Spec Kit individual.
