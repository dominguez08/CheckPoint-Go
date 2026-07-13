# Script para limpiar cache de NetBeans y reconstruir CheckPointGo
# Ejecutar en PowerShell (como Administrador recomendado)
# Uso: .\limpiar_y_reconstruir.ps1

$ErrorActionPreference = "Stop"

Write-Host "`n===================================`n" -ForegroundColor Cyan
Write-Host "   LIMPIEZA Y RECONSTRUCCION`n" -ForegroundColor Cyan
Write-Host "   CheckPointGo`n" -ForegroundColor Cyan
Write-Host "===================================`n" -ForegroundColor Cyan

# Verificar que estamos en el directorio correcto
if (-not (Test-Path "pom.xml")) {
    Write-Host "ERROR: pom.xml no encontrado" -ForegroundColor Red
    Write-Host "Asegurate de estar en: C:\Users\User\Documents\NetBeansProjects\CheckPointGo" -ForegroundColor Red
    Read-Host "Presiona Enter para salir"
    exit 1
}

# Paso 1: Limpiar target
Write-Host "[1/5] Limpiando target..." -ForegroundColor Yellow
if (Test-Path "target") {
    Remove-Item -Path "target" -Recurse -Force -ErrorAction SilentlyContinue
}
Write-Host "[OK] Target limpiado`n" -ForegroundColor Green

# Paso 2: Limpiar cache de NetBeans
Write-Host "[2/5] Limpiando cache de NetBeans..." -ForegroundColor Yellow
$netBeansVersions = @("21", "20", "19", "18")
foreach ($version in $netBeansVersions) {
    $cachePath = "$env:APPDATA\NetBeans\$version\var\cache"
    if (Test-Path $cachePath) {
        Remove-Item -Path $cachePath -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "[OK] Cache NetBeans $version limpiado"
    }
}
Write-Host ""

# Paso 3: Maven clean
Write-Host "[3/5] Ejecutando mvn clean..." -ForegroundColor Yellow
& mvn clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: mvn clean falló" -ForegroundColor Red
    Read-Host "Presiona Enter para salir"
    exit 1
}
Write-Host "[OK] Maven clean completado`n" -ForegroundColor Green

# Paso 4: Maven install
Write-Host "[4/5] Ejecutando mvn install..." -ForegroundColor Yellow
& mvn clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: mvn install falló" -ForegroundColor Red
    Read-Host "Presiona Enter para salir"
    exit 1
}
Write-Host "[OK] Maven install completado`n" -ForegroundColor Green

# Paso 5: Mensaje final
Write-Host "===================================`n" -ForegroundColor Cyan
Write-Host "   COMPLETADO EXITOSAMENTE!`n" -ForegroundColor Green
Write-Host "===================================`n" -ForegroundColor Cyan
Write-Host "Abre NetBeans y:" -ForegroundColor Yellow
Write-Host "1. File > Open Project" -ForegroundColor White
Write-Host "2. Selecciona: C:\Users\User\Documents\NetBeansProjects\CheckPointGo" -ForegroundColor White
Write-Host "3. Right-click en proyecto > Clean and Build" -ForegroundColor White
Write-Host "4. F6 para ejecutar`n" -ForegroundColor White

Read-Host "Presiona Enter para salir"
