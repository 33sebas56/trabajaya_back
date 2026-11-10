@echo off
cd /d "%~dp0"
echo TrabajaYA Backend - Java JDK 21 - http://localhost:8080
echo Perfil local: H2 persistente e imagenes locales. Ctrl+C para detener.
set SPRING_PROFILES_ACTIVE=local
call mvnw.cmd spring-boot:run
if errorlevel 1 pause
