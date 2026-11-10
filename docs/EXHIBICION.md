# Recorrido para exhibicion local

Antes de presentar, ejecuta `./mvnw.cmd test` y confirma el arranque de ambos procesos. Consulta VERIFICACION.md para distinguir lo comprobado en la entrega de lo que debes validar localmente.

1. Inicia backend y frontend. Ingresa como cliente y abre otra ventana con un trabajador.
2. Publica una tarea en Pasto con categoria compatible, fecha futura y un cupo. Usa precio 60.000, maximo 100.000 e incremento 10.000. Adjunta una foto sin datos personales.
3. Abre la oportunidad como trabajador: todavia no debe verse direccion ni presupuesto maximo.
4. Como cliente, aumenta a 70.000. El trabajador actualiza y acepta el precio mostrado.
5. Comprueba que la direccion ahora si aparece, que se ocupa el cupo y que queda fijado el precio acordado.
6. Envia un mensaje desde cada cuenta. Debe aparecer en la otra en la siguiente actualizacion (aproximadamente 5 segundos), sin cambiar artificialmente de rol.
7. Avanza: en camino, en el lugar, en ejecucion y terminado. El cliente confirma y califica; el trabajador puede devolver la calificacion.
8. Recarga ambas paginas. Los datos deben seguir existiendo. Deten/reinicia el backend para mostrar persistencia local.
9. Para mostrar moderacion, crea un reporte desde un trabajo y entra como admin. Cambia a EN_REVISION o RESUELTO y comprueba la respuesta desde el autor.

## Demostraciones opcionales

**Aumento automatico:** crea otra tarea con intervalo de 1 minuto y automatico activo. Deja backend encendido. El proximo aumento se aplica en la siguiente pasada del planificador, sin exceder el tope. No utilices cifras inventadas de trabajadores notificados.

**Dos cupos con precios distintos:** publica con dos personas. Carlos acepta a 60.000; aumenta a 80.000; Ana acepta. Cada participacion debe conservar su valor. Ambos deben terminar y el cliente confirmar cada uno para completar el trabajo global.

**Concurrencia:** `FlowIntegrationTest.twoWorkersCannotTakeLastSlot` prueba solicitudes concurrentes; no basta con cambiar visualmente un contador.

**Supabase:** con configuracion real, sube una imagen y verifica que existe en el bucket privado. Reinicia el backend y consultala desde un participante autorizado. Nunca muestres una clave service_role en pantalla durante la exposicion.

## Codigo para mostrar

- `jobs/JobService.java`: transaccion de aceptacion, bloqueo del trabajo y precio esperado.
- `jobs/JobRules.java`: reglas puras de precio y estados.
- `events/EventRecorder.java`: eventos internos -> avisos/auditoria dentro de la transaccion.
- `storage/ObjectStorage.java`: cambio de proveedor sin cambiar los endpoints.
- `frontend/src/jobs.js`: fetch reales y comportamiento por permisos.

Explicacion sugerida: "La interfaz no decide por si sola quien puede aceptar. El servidor verifica permisos, cupos y el precio dentro de una transaccion. La base de datos conserva el acuerdo y los eventos generan el historial y las notificaciones."

Los pagos reales, la certificacion de identidad y el seguimiento GPS continuo no forman parte de esta demostracion. No los presentes como implementados.
