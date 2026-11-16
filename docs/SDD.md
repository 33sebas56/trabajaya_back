# SDD y GitHub Spec Kit en un proyecto que ya existe

La rúbrica evalua constitución, especificación con Non-Goals, plan, tareas, código,
pruebas y evidencia Git. El README permite navegar todo eso, pero no reemplaza los
artefactos diferenciados ni demuestra que se ejecuto Spec Kit.

## Lo que contiene este paquete

- `.specify/memory/constitution.md`: reglas reales de TrabajaYA.
- `specs/001-backend-sdd/spec.md`: consolidacion del backend, historias y aceptacion.
- `plan.md`: decisiones de implementacion.
- `tasks.md`: tareas identificadas y estados honestos.
- `research.md`, `data-model.md` y `contracts/`: contexto y contratos.

Los archivos se elaboraron sobre un backend existente. No se afirma que el código
original se genero con Spec Kit ni se reconstruye una cronologia falsa.

## Uso real posterior con el agente elegido

La instalacion del CLI requiere Python/uv y acceso a GitHub. Es una herramienta del
proceso de desarrollo, no una dependencia que deba instalarse en Render.

Antes de inicializar, conservar un commit de base y trabajar en otra rama. La
opción --force puede reemplazar archivos gestionados: revisar el diff, conservar
la constitución y la especificación propias, y no aceptar cambios a ciegas.

Ejemplo para PowerShell y una integración Copilot ya disponible en el entorno:

```powershell
uv tool install specify-cli --from git+https://github.com/github/spec-kit.git
specify init --help
specify init --here --force --integration copilot --script ps
```

La clave de integración debe coincidir con el agente elegido; no contratar un
servicio solo para copiar este ejemplo. Las opciones pueden cambiar: revisar el
help de la versión instalada. No ejecutar el comando en una copia sin respaldo.

Con la integración compatible, el flujo disponible en la documentación actual es:

```text
/speckit-constitution
/speckit-specify
/speckit-plan
/speckit-tasks
/speckit-analyze
/speckit-implement
```

No es necesario pedirle al agente que recree todo el marketplace. Se puede usar un
cambio acotado ya documentado, como una mejora de almacenamiento, revisar los
artefactos y continuar la siguiente tarea pendiente. Registrar la versión real del
CLI, los comandos usados y los archivos resultantes en un commit del repositorio.

**Estado actual: archivos preparados; CLI/agente no ejecutados en esta entrega.**
No hay capturas, logs o historial artificial que afirmen lo contrario.

## Modalidad individual

La rúbrica habla de equipo y aportes de cada integrante. Trabajar individualmente
no concede automáticamente una exencion: la aceptacion de esa modalidad se debe
confirmar con el docente. Se puede mantener un flujo real de ramas y pull requests
propios, pero una autorevision no se presenta como revisión de otra persona.

## Referencia

Guia oficial para proyectos existentes:
https://github.com/github/spec-kit/blob/main/docs/how-to/existing-projects.md

Repositorio y comandos actuales:
https://github.com/github/spec-kit
