# Verificación del backend de TrabajaYA

Aquí registro las comprobaciones del backend y separo cada resultado por su alcance. El sistema ejecuta código Java real, consultas SQL, migraciones y operaciones de almacenamiento; los resultados no son una representación de lo que podría suceder.

## Pruebas automatizadas revisadas

La ejecución inicial corresponde al commit `ea16f6652cdefbade654d4fbbf95a60417b2309f`. Los archivos `test-results-h2.zip` y `test-results-postgres.zip` contienen los XML de Surefire:

| Entorno | Casos ejecutados | Fallos | Errores | Omitidos |
|---|---:|---:|---:|---:|
| H2 | 31 | 0 | 0 | 0 |
| PostgreSQL | 31 | 0 | 0 | 0 |

Son 31 casos distintos ejecutados en dos entornos, 62 ejecuciones en total. Incluyen flujo de contratación, privacidad de direcciones, sesiones, concurrencia del último cupo, precios, estados, imágenes y calificación. El detalle está en [EVIDENCIAS_PRUEBAS.md](EVIDENCIAS_PRUEBAS.md).

La ejecución de GitHub correspondiente también construyó la imagen Docker. Construir una imagen no es lo mismo que iniciar esa imagen con las variables de Render.

## Correcciones verificadas durante el despliegue

Se identificaron y corrigieron problemas de configuración: formato JDBC, certificado CA de Aiven, URL base de Supabase y disponibilidad de PostgreSQL. El log completo mostró `UnknownHostException`; en el panel de Aiven el servicio estaba apagado. Después de encenderlo, el autor confirmó que el backend inició en Render. Esa es una comprobación manual comunicada por el autor, no un reporte automático adjunto de todas las operaciones de la nube.

El validador de nube se adelantó al inicio del DataSource. También se ampliaron las pruebas de configuración y el workflow para arrancar el contenedor. **Los 31 casos de la tabla pertenecen a la ejecución inicial**: para atribuir un resultado a las pruebas ampliadas se utiliza el artefacto y el commit de esa ejecución, no el conteo anterior.

## Cómo interpreto la evidencia

- `/actuator/health` revisa la aplicación y su conexión a la base.
- `/api/categories` realiza una consulta SQL.
- `/api/health` informa el proveedor configurado, pero no acredita por sí solo una subida a Supabase.
- La subida y lectura de una imagen, el registro y la contratación se comprueban con operaciones autenticadas.

Los dobles usados por algunas pruebas permiten reproducir errores de forma controlada. No significan que la aplicación publicada simule el almacenamiento o los trabajos.

## Alcance y trazabilidad

Los artefactos de [SDD](SDD.md) describen reglas, especificación, plan y tareas del proyecto. Su existencia no acredita una ejecución del CLI de Spec Kit ni revisiones de un equipo. El historial de Git y las ejecuciones de Actions son la fuente para esas evidencias.

El proyecto no implementa pagos reales, verificacion documental de identidad ni GPS continuo. El correo depende de habilitar un proveedor. Los servicios gratuitos pueden suspenderse y el programador de precios no corre mientras Spring Boot está detenido. No se ha presentado una certificación de producción ni una auditoría externa.

Este registro reemplaza el informe de preparación que indicaba que Maven no pudo descargarse en un entorno anterior. Aquel bloqueo no describe las pruebas de GitHub que posteriormente sí se ejecutaron.
