# Compatibilidad con el proyecto recibido

## Se conserva

- Java 21, Spring Boot 3.5.16 y SQL por JDBC.
- Las 42 rutas HTTP declaradas por los controladores.
- Solicitudes camelCase, respuestas del negocio snake_case y bearer tokens.
- Importes COP por persona, enums y reglas principales de precios/estados.
- La migración V1 byte por byte; la V2 solo agrega índices.
- Adaptadores local y Supabase, sin obligar a modificar el frontend.

El inventario verificable esta en
[api-baseline.json](../specs/001-backend-sdd/contracts/api-baseline.json).
El comprobador compara método/ruta y el hash V1: no pretende verificar cada campo
ni reemplaza pruebas de contrato de solicitudes y respuestas.

## Cambios intencionales

| Cambio | Impacto |
|---|---|
| DEMO_SEED false por defecto | No se crean cuentas de ejemplo accidentalmente |
| Datos locales fuera del ZIP | No se trasladan usuarios ni secretos de otra instalacion |
| Perfil render estricto | Exige postgres, TLS verify-full, Supabase y no demo |
| Errores con code/status/requestId | Campos adicionales; message se conserva |
| Validaciones adicionales | Rechaza entradas que antes podian pasar incompletas |
| Retirados en mine=true | Conserva historial, sin entregar dirección privada |
| Guardado de avatar con bloqueo | Evita doble uso del mismo objeto |
| V2 con índices | Flyway la ejecuta una vez; no borrar el historial |
| Lanzador Maven opcional | Docker/CI usan Maven instalado, sin depender del computador |

## Actualizar sin perder el repositorio

Crear una rama en el repositorio actual y copiar los archivos de esta entrega encima
del código, sin reemplazar `.git`, `.env`, configuraciones privadas ni datos locales.
Revisar el diff antes del commit. El ZIP no es una copia del historial Git: ese
historial debe conservarse en el repositorio del usuario.

No importar la base H2 que venia en el archivo original a Aiven como parte de esta
actualizacion. No ejecutar clean/repair de Flyway para forzar coincidencias. Si hay
una V2 ajena a este paquete ya aplicada, revisar la numeracion antes de desplegar.

El frontend no forma parte de esta revisión. No se ha realizado una prueba de su
navegador contra este backend modificado: la compatibilidad conservada es la base
para esa comprobacion posterior, no una garantia de extremo a extremo.
