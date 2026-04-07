@echo off
cd /d "%~dp0"
javac *.java
if errorlevel 1 pause & exit /b 1
java MainFrame
pause