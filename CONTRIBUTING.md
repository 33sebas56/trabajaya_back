# Como organizo los cambios

Este proyecto se lleva de forma individual. Las ramas, commits y pull requests son
para revisar y explicar el trabajo real, no para simular un equipo.

## Flujo sugerido

1. Conservar el repositorio original, incluida su carpeta `.git`.
2. Crear una rama antes de copiar o modificar el código.
3. Revisar el diff y relacionarlo con T001-T022 o una nueva tarea.
4. Hacer commits descriptivos y pequenos cuando el cambio lo permita.
5. Abrir un pull request con alcance, pruebas y pendientes.
6. Revisar el resultado de Actions antes de integrar en main.

Ejemplo de inicio, no historial ya realizado:

```powershell
git switch -c feature/backend-sdd
git status
# Copiar y revisar los cambios antes del commit.
git diff --stat
```

No borrar main ni reemplazar el repositorio completo por una carpeta sin historia.
No dividir artificialmente un cambio ya entregado para fingir colaboración.

## Reglas de cambio

- Mantener el contrato HTTP o documentar una versión nueva.
- No modificar una migración aplicada; agregar otra numerada.
- No subir tokens, `.env`, bases locales, certificados ni archivos de usuarios.
- Actualizar spec/plan/tasks cuando cambie el alcance comprometido.
- Incluir el caso exitoso y un caso de error en pruebas para una regla nueva.
- No marcar despliegues/pruebas externas como terminados sin resultado real.

El código se revisa con JDK 21. El CI usa H2 y PostgreSQL temporales y no necesita
acceder a Aiven ni a Supabase. Los reportes del workflow son evidencia de la revisión
que efectivamente se ejecuto, no de todas las instalaciones futuras.
