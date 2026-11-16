# Hallazgos y decisiones previas

La rúbrica recibida pide seis aspectos: artefactos SDD, aplicación funcional, calidad,
trabajo colaborativo, pruebas y documentación/demo. No pide nuevas funciones comerciales.
La modalidad individual es una adaptacion solicitada por el autor, no una exencion
que el documento conceda automáticamente.

El ZIP contiene código Java, un backend modular y una migración existente. También
contiene .git, archivos del IDE y una base H2: no se redistribuyen estos ultimos.
No hace falta introducir una base NoSQL para fotos: se usa PostgreSQL para registros
y Supabase Storage para objetos. La aplicación ya implementa Bearer revocable con
SQL; cambiarlo por JWT no es necesario para este alcance.

La guia oficial de Spec Kit para proyectos existentes recomienda partir del código
actual y definir el siguiente cambio acotado, no inventar una especificación histórica.
Esta entrega aporta los artefactos; una ejecución del CLI/agente requiere evidencia
real posterior y no se da por realizada solamente por tener carpetas .specify.

Fuentes tecnicas consultadas:
- https://github.com/github/spec-kit/blob/main/docs/how-to/existing-projects.md
- https://docs.spring.io/spring-boot/3.5/system-requirements.html
- https://render.com/docs/docker
- https://render.com/docs/free
- https://render.com/docs/docker-secrets
- https://supabase.com/docs/guides/getting-started/api-keys

Fuente de evaluación: Criterios_evaluacion_proyecto_final_SDD.pdf, páginas 2 a 8,
aportado por el usuario. No se adjunta una copia al repositorio.
