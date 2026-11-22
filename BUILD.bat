@echo off
title pvp.cod build
cd /d "%~dp0"
set "LOG=%~dp0build-log.txt"
echo pvp.cod build log > "%LOG%"
set "JHOME="
for /f "delims=" %%J in ('where javac 2^>nul') do if not defined JHOME for %%A in ("%%J\..\..") do set "JHOME=%%~fA"
if defined JHOME set "JAVA_HOME=%JHOME%"
echo JAVA_HOME=%JAVA_HOME% >> "%LOG%"
java -version >> "%LOG%" 2>&1
echo Building pvp.cod (with pack.cod inside)... this can take a few minutes the first time.
del /q "build\libs\*.jar" >nul 2>&1
call gradlew.bat build --no-daemon --console=plain >> "%LOG%" 2>&1
echo EXIT=%ERRORLEVEL% >> "%LOG%"
for %%F in ("build\libs\pvp-cod-*.jar") do (
  echo %%~nxF | find /i "sources" >nul || (
    del /q "..\mods\pvp-cod-*.jar" >> "%LOG%" 2>&1
    rem pack.cod is inside pvp.cod now, so the separate pack.cod jar has to go (two copies crash the game).
    del /q "..\mods\pack-cod-*.jar" >> "%LOG%" 2>&1
    del /q "..\mods\pack-cod-*.jar.disabled" >> "%LOG%" 2>&1
    copy /y "%%F" "..\mods\%%~nxF" >> "%LOG%"
    echo COPIED %%~nxF >> "%LOG%"
  )
)
echo DONE >> "%LOG%"
type "%LOG%" | find "EXIT="
type "%LOG%" | find "COPIED"
echo.
echo Done. Details are in build-log.txt
timeout /t 5 >nul
