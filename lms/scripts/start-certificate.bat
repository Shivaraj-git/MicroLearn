@echo off
title Certificate Service (8087)
cd /d "%~dp0..\certificate-service"
call mvn spring-boot:run
