# Contrato que se conserva

El inventario `api-baseline.json` se obtuvo del backend recibido, antes de los cambios.
Contiene 42 pares método/ruta y el hash SHA-256 de V1. `tools/check_repository.py`
los compara con la revisión actual. No es una definicion OpenAPI completa.

Los cuerpos, roles, estados y errores se explican en [API](../../../docs/API.md).
Las pruebas de integración comprueban casos representativos; conservar nombres de
rutas no demuestra por si solo compatibilidad de todos los campos.
