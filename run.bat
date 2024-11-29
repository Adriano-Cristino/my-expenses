@echo off
echo Criando diretorios necessarios...
mkdir src\main\java\com\expenses 2>nul
mkdir src\main\resources\css 2>nul
mkdir src\main\resources\fxml 2>nul
mkdir src\main\resources\images 2>nul
mkdir src\main\resources\reports 2>nul

echo Limpando e compilando...
call mvn clean javafx:run

pause
