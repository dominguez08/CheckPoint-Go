@echo off
REM Script para limpiar cache de NetBeans y reconstruir CheckPointGo
REM Ejecutar como Administrador

echo.
echo ===================================
echo    LIMPIEZA Y RECONSTRUCCION
echo    CheckPointGo
echo ===================================
echo.

REM Verificar si está en el directorio correcto
if not exist "pom.xml" (
    echo ERROR: pom.xml no encontrado
    echo Asegurate de estar en: C:\Users\User\Documents\NetBeansProjects\CheckPointGo
    pause
    exit /b 1
)

echo [1/5] Limpiando target...
rmdir /s /q target 2>nul
if exist target (
    echo ERROR: No se pudo eliminar target
    pause
    exit /b 1
)
echo [OK] Target limpiado

echo.
echo [2/5] Limpiando cache de NetBeans...
REM Para NetBeans 21
if exist "%APPDATA%\NetBeans\21\var\cache" (
    rmdir /s /q "%APPDATA%\NetBeans\21\var\cache"
    echo [OK] Cache NetBeans 21 limpiado
)
REM Para NetBeans 20
if exist "%APPDATA%\NetBeans\20\var\cache" (
    rmdir /s /q "%APPDATA%\NetBeans\20\var\cache"
    echo [OK] Cache NetBeans 20 limpiado
)

echo.
echo [3/5] Ejecutando mvn clean...
call mvn clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: mvn clean falló
    pause
    exit /b 1
)
echo [OK] Maven clean completado

echo.
echo [4/5] Ejecutando mvn install...
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: mvn install falló
    pause
    exit /b 1
)
echo [OK] Maven install completado

echo.
echo ===================================
echo    COMPLETADO EXITOSAMENTE!
echo ===================================
echo.
echo Abre NetBeans y:
echo 1. File ^> Open Project
echo 2. Selecciona: C:\Users\User\Documents\NetBeansProjects\CheckPointGo
echo 3. Right-click en proyecto ^> Clean and Build
echo 4. F6 para ejecutar
echo.
pause
