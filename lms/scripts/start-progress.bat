@echo off
title Progress Service (8085)
cd /d "%~dp0..\progress-service"
call mvn spring-boot:run
