# Verificacion de la entrega

## Ejecutado en el entorno de construccion

- Analisis sintactico de 36 archivos Java con el parser del JDK 21. No equivale a compilar con Spring.
- Compilacion y ejecucion de `JobRules.java` + `tools/DomainSmoke.java`: 14 comprobaciones de reglas de precio, limite, desbordamiento, estados, cupos y cancelacion pasaron.
- `npm run check`: servidor Node y modulos frontend sin errores de sintaxis.
- `npm test`: cinco pruebas de utilidades (escape HTML, importes, iniciales, estados y fechas) pasaron.
- Navegador Chromium, cargado offline: landing escritorio/movil, publicacion, aumento, aceptacion, mensaje, secuencia de estados, confirmacion, calificacion, perfil, notificaciones, reportes y pestañas administrativas. Sin excepciones JavaScript no capturadas en ese recorrido. Se utilizaron respuestas controladas de API; NO el servidor Spring.
- Revisión visual de capturas de escritorio y de 390 px de ancho; no se detecto desbordamiento horizontal en las pantallas comprobadas.
- Servidor estatico Node iniciado localmente: HTML y modulos JS accesibles; `.env` no se sirve como archivo publico.

## NO ejecutado / pendiente

- Descarga de dependencias Maven y compilacion completa de Spring Boot: el entorno no tenia Maven ni cache de dependencias y no permitia descargarlas.
- Arranque del contexto Spring, migraciones sobre H2/PostgreSQL y pruebas `FlowIntegrationTest` con la API real.
- Concurrencia real contra PostgreSQL, pruebas de carga y auditoria de seguridad.
- Supabase Storage y SMTP con cuentas reales: no se recibieron credenciales.
- Launchers Windows/Mac en esos sistemas operativos. Se reviso su codigo, pero no se ejecutaron aqui.

No existe en esta entrega un JAR precompilado ni una certificacion de que el sistema ya se ejecuto de extremo a extremo. El frontend usa rutas reales en produccion; el doble de API solo se encuentra en los tests visuales.

## Verificar en tu equipo

1. Abre el backend con JDK 21 y ejecuta `./mvnw.cmd test` en PowerShell, o `sh ./mvnw test` en Linux/macOS.
2. Si todos los tests pasan, ejecuta `./mvnw.cmd spring-boot:run`.
3. Inicia el frontend con `node server.mjs`; abre localhost:5173.
4. Realiza el recorrido de `EXHIBICION.md` con dos cuentas reales de la base local.
5. Cambia al perfil PostgreSQL, configura tu base y repite el recorrido antes de mostrarlo con esa base.
6. Configura Storage y comprueba carga/lectura autorizada de una imagen. No expongas la clave de servidor.

Si un test falla, conserva el error completo y revisa la causa antes de presentar; no lo marques como aprobado ni uses `-DskipTests` para afirmar que fue verificado.
