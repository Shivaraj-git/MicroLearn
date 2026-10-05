@echo off
title Identity Service (8081)
cd /d "%~dp0..\identity-service"
call mvn spring-boot:run
