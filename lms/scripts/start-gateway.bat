@echo off
title API Gateway (8080)
cd /d "%~dp0..\api-gateway"
call mvn spring-boot:run
