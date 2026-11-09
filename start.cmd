@echo off
rem Zapusk prilozheniya s frontendom: http://localhost:8080
rem   start.cmd           - demo-rezhim, vstroennaya baza H2 (PostgreSQL ne nuzhen)
rem   start.cmd postgres  - rabota s PostgreSQL iz application.yml
setlocal
cd /d "%~dp0"

rem Esli Java net v PATH i ne zadan JAVA_HOME - ishem JDK, skachannyj IntelliJ IDEA (papka .jdks)
if defined JAVA_HOME goto run
where java >nul 2>nul
if not errorlevel 1 goto run
for /d %%D in ("%USERPROFILE%\.jdks\*") do if exist "%%D\bin\java.exe" set "JAVA_HOME=%%D"
if not defined JAVA_HOME (
  echo Java ne najdena. Ustanovite JDK 21+ ili ukazhite JAVA_HOME.
  exit /b 1
)

:run
if defined JAVA_HOME echo JAVA_HOME=%JAVA_HOME%
set "PROFILE=demo"
if /i "%~1"=="postgres" set "PROFILE=default"

echo Posle zapuska otkrojte v brauzere: http://localhost:8080
call "%~dp0mvnw.cmd" spring-boot:run -Dspring-boot.run.profiles=%PROFILE%
