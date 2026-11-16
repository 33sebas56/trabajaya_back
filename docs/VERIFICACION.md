# Verificación de esta entrega

Esta tabla registra lo que se pudo comprobar al preparar el paquete. No es una
certificación de producción ni el resultado de un despliegue en las cuentas del usuario.

| Comprobación | Resultado | Alcance real |
|---|---|---|
| Compilar seis clases Java sin dependencias y ejecutar DomainSmoke | **66 comprobaciones aprobadas** | Precios, estados, contraseñas, rutas de objetos, paginación, eventos y limitador |
| Analizar sintaxis de src con el parser de JDK 21 | **58 archivos aceptados** | Solo sintaxis; no resuelve librerías ni tipos externos |
| Inventario de métodos/rutas | **42 rutas conservadas** | No garantiza por sí solo todos los formatos de respuesta |
| SHA-256 de V1 frente al ZIP recibido | **Idéntico** | No se reescribió una migración existente |
| XML de pom y YAML de CI/Render/Compose/issue | **Parseo aprobado** | No valida las cuentas ni ejecuta cada plataforma |
| Integridad del repositorio y enlaces Markdown | Ver log adjunto | Comprueba artefactos, enlaces, rutas y datos que no deben versionarse |
| Maven / compilación completa Spring | **Bloqueado** | No se pudo resolver repo.maven.apache.org al descargar Maven |
| 31 métodos JUnit en seis clases | **Incluidos, no ejecutados aquí** | Los ejecutará el workflow cuando exista una ejecución real |
| PostgreSQL, Flyway y Docker en CI | **Preparados, pendientes** | El workflow no se ejecutó desde este entorno |
| Render + Aiven + Supabase reales | **No configurados ni probados** | No se recibieron credenciales; no se accedió a las cuentas |
| Integración con el frontend | **Pendiente** | Se conserva el contrato; frontend fuera de este paquete |
| Spec Kit CLI, commits y PR del estudiante | **Pendientes** | No se generó evidencia ficticia |

## Resultados reproducibles sin dependencias

Con JDK 21, los siguientes comandos reproducen verificaciones limitadas:

```bash
sh tools/run-domain-checks.sh
mkdir -p target/syntax-check
javac -d target/syntax-check tools/JavaSyntax.java
java -cp target/syntax-check JavaSyntax src
python3 tools/check_repository.py
```

No son requisitos de pruebas locales para el usuario: se documentan para que las
comprobaciones tengan un alcance claro y puedan repetirse en un entorno adecuado.
Los resultados capturados están en [evidence](../evidence/verification.json).

## Qué ocurrió con Maven

Se intentó `./mvnw -B -ntp test`, pero el lanzador no pudo descargar Maven:

```text
curl: (6) Could not resolve host: repo.maven.apache.org
```

Por tanto **no existe una compilación completa ni un reporte Surefire aprobado en
esta entrega**. No se debe interpretar la aceptación de sintaxis como que Spring,
SQL, Mockito o la inyección de dependencias ya fueron verificados.

## Suite preparada para GitHub Actions

| Clase | Métodos @Test | Propósito |
|---|---:|---|
| FlowIntegrationTest | 11 | API completa, permisos, privacidad, cupos, perfiles y moderación |
| BackendRulesTest | 6 | Contraseñas, límites, paginación, eventos y claves |
| JobRulesTest | 5 | Precios, secuencia de estados, completitud y distancia |
| ImageSanitizerTest | 3 | Formato real, conversión y límites de imágenes |
| MediaCompensationTest | 3 | Rollback/commit mediante dobles y callbacks de transacción |
| CloudConfigurationGuardTest | 3 | Rechazo de configuraciones inseguras en Render |

El workflow ejecuta la suite con H2 y con un PostgreSQL 18 temporal. Luego construye
la imagen Docker si ambos trabajos pasan. Publica los reportes como artefactos de
Actions incluso cuando falle una prueba. La contraseña de la base temporal de CI
es desechable, no una credencial de Aiven.

Para completar la evidencia, conservar el enlace real del workflow y su commit,
revisar los resultados y agregar después una verificación pública de salud,
registro y subida autorizada a Supabase. No marcar tareas externas como terminadas
solo porque exista el código para realizarlas.

## Límites conocidos

El correo no se entrega mientras no haya proveedor; el scheduler no corre cuando
Render Free duerme. La compensación de Storage puede fallar o no ejecutarse ante un
corte abrupto del proceso. El limitador es de una instancia y el inventario HTTP es
solo de métodos/rutas. No se hicieron pruebas de carga, auditoría externa de seguridad
ni una prueba de compatibilidad visual del frontend.
