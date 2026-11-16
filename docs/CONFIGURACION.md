# Configuración

La configuración principal de esta entrega es la de
[despliegue en Render con Aiven y Supabase](DESPLIEGUE.md).

`.env.example` es una plantilla de variables, no un archivo que Spring cargue
automáticamente. Los valores reales se ponen en Environment de Render. No deben
quedar en Git ni en el frontend.

El perfil local sigue disponible como alternativa de desarrollo: JDK 21,
`mvn spring-boot:run`, H2 en `data/` y archivos en `uploads/`. No se crean usuarios de
ejemplo salvo que se habilite expresamente DEMO_SEED. Este camino es opcional y no
es un requisito previo para el despliegue pedido.

El ejemplo `config/local.properties.example` no tiene credenciales. Su copia
`local.properties` se importa exclusivamente en el perfil local, no en Render.
