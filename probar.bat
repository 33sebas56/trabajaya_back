@echo off
cd /d "%~dp0"
call mvnw.cmd test
if errorlevel 1 pause
