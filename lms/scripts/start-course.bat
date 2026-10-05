@echo off
title Course Service (8082)
cd /d "%~dp0..\course-service"
call mvn spring-boot:run
