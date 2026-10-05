@echo off
title Payment Service (8083)
cd /d "%~dp0..\payment-service"
call mvn spring-boot:run
