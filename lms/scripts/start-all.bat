@echo off
echo Starting RabbitMQ...
start "RabbitMQ" cmd /k "%~dp0start-rabbitmq.bat"

echo Waiting 30 seconds for RabbitMQ to start...
timeout /t 30 /nobreak >nul

echo Starting Identity Service...
start "Identity" cmd /k "%~dp0start-identity.bat"

echo Starting Course Service...
start "Course" cmd /k "%~dp0start-course.bat"

echo Starting Payment Service...
start "Payment" cmd /k "%~dp0start-payment.bat"

echo Starting Notification Service...
start "Notification" cmd /k "%~dp0start-notification.bat"

echo Starting Certificate Service...
start "Certificate" cmd /k "%~dp0start-certificate.bat"

echo Starting Progress Service...
start "Progress" cmd /k "%~dp0start-progress.bat"

echo Starting Enrollment Service...
start "Enrollment" cmd /k "%~dp0start-enrollment.bat"

echo Starting API Gateway...
start "Gateway" cmd /k "%~dp0start-gateway.bat"

echo.
echo All started.
echo   Front door (use this one): http://localhost:8080
echo   RabbitMQ UI:               http://localhost:15672  (guest / guest)
