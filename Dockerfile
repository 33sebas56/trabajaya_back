FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p /app && chown 1000:1000 /app
COPY --from=build --chown=1000:1000 /workspace/target/trabajaya-backend.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Duser.timezone=America/Bogota"
# Render secret files are readable by group 1000. Never COPY secrets into the image.
USER 1000:1000
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
