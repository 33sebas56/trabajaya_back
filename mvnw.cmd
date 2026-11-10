@echo off
setlocal
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 goto bundled
call mvn %*
exit /b %errorlevel%
:bundled
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\maven.ps1" %*
exit /b %errorlevel%
