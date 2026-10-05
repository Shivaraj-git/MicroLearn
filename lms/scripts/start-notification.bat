@echo off
title Notification Service (8086)
cd /d "%~dp0..\notification-service"
call mvn spring-boot:run
