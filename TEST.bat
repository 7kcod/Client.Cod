@echo off
title pvp.cod test instance
cd /d "%~dp0"
echo.
echo  pvp.cod test instance
echo  ----------------------
echo  First launch downloads Minecraft 1.21.11 + Fabric (a few minutes).
echo  pack.cod is built in.
echo.
if not exist run\resourcepacks mkdir run\resourcepacks
call gradlew.bat runClient
pause
