@echo off
title Enrollment Service (8084)
cd /d "%~dp0..\enrollment-service"
call mvn spring-boot:run
