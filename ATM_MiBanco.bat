@echo off
setlocal EnableDelayedExpansion
title Automatizacion App Movil Mi Banco - GreenSQA
cd /d "%~dp0"

rem ================== Configuracion ==================
set "ANDROID_SDK=%LOCALAPPDATA%\Android\Sdk"
if defined ANDROID_HOME set "ANDROID_SDK=%ANDROID_HOME%"
rem UiAutomator2 (Appium) requiere ANDROID_HOME y JAVA_HOME en el proceso del servidor
if not defined ANDROID_HOME set "ANDROID_HOME=%ANDROID_SDK%"
if not defined JAVA_HOME if exist "%ProgramFiles%\Java\jdk-17" set "JAVA_HOME=%ProgramFiles%\Java\jdk-17"
set "ADB=%ANDROID_SDK%\platform-tools\adb.exe"
set "EMULATOR=%ANDROID_SDK%\emulator\emulator.exe"
rem Ultimo emulador usado en este equipo (cada maquina tiene los suyos; no se versiona)
set "ARCHIVO_AVD=config\emulador_actual.txt"
set "APPIUM_URL=http://127.0.0.1:4723"
set "APP_PAQUETE=co.com.mibanco.app"
set "CONTROL=control\Control_Ejecucion.xlsx"
set "ARCHIVO_DISPOSITIVO=config\dispositivo_actual.txt"
set "ALLURE_DIR=herramientas\allure-2.29.0"
set "MAVEN_OPTS=-Xmx2g -Dfile.encoding=UTF-8"
rem Si el PATH trae %MAVEN_HOME%\bin sin expandir, cmd no encuentra mvn: se agrega explicitamente
where mvn >nul 2>&1 || if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "PATH=%MAVEN_HOME%\bin;%PATH%"

rem Modo directo: ATM_MiBanco.bat <opcion> ejecuta esa opcion una vez y termina
if not "%~1"=="" (
    call :leer_dispositivo
    if "%~1"=="1" call :abrir_control
    if "%~1"=="2" call :iniciar_ambiente
    if "%~1"=="3" call :solo_appium
    if "%~1"=="4" call :ejecutar
    if "%~1"=="5" call :consultar_reporte
    if "%~1"=="6" call :prerrequisitos
    exit /b
)

:menu
cls
call :leer_dispositivo
echo ==================================================================
echo     AUTOMATIZACION APP MOVIL MI BANCO              GreenSQA
echo ==================================================================
echo   Dispositivo seleccionado: !DISPOSITIVO!
echo ------------------------------------------------------------------
echo   1. Abrir archivo de control
echo   2. Inicializar ambiente (Emulador + Appium)
echo   3. Inicializar solo Appium y reconocer dispositivo conectado
echo   4. Ejecutar pruebas
echo   5. Consultar reporte
echo   6. Verificar prerrequisitos
echo   0. Salir
echo ------------------------------------------------------------------
set "OPCION="
set /p "OPCION=Seleccione una opcion: "
if "%OPCION%"=="1" call :abrir_control & goto menu
if "%OPCION%"=="2" call :iniciar_ambiente & goto menu
if "%OPCION%"=="3" call :solo_appium & goto menu
if "%OPCION%"=="4" call :ejecutar & goto menu
if "%OPCION%"=="5" call :consultar_reporte & goto menu
if "%OPCION%"=="6" call :prerrequisitos & goto menu
if "%OPCION%"=="0" exit /b 0
goto menu

rem ================== 1. Archivo de control ==================
:abrir_control
if not exist "%CONTROL%" (
    echo No se encontro %CONTROL%
    pause
    exit /b
)
start "" "%CONTROL%"
exit /b

rem ================== 2. Emulador + Appium ==================
:iniciar_ambiente
call :emulador_activo
if defined SERIAL (
    echo Ya hay un emulador en ejecucion: !SERIAL!
) else (
    call :elegir_avd
    if not defined AVD exit /b
    echo Iniciando emulador !AVD! ...
    rem Arranque en frio: un snapshot guardado tras una caida deja el emulador inestable
    start "Emulador Android" "%EMULATOR%" -avd !AVD! -no-snapshot-load -no-boot-anim -no-audio
    call :esperar_emulador
    if not defined SERIAL (
        echo El emulador no aparecio a tiempo. Revise la ventana del emulador.
        pause
        exit /b
    )
)
call :esperar_arranque !SERIAL!
rem En el emulador de pruebas se desactiva Autofill para evitar el dialogo "Guardar clave"
"%ADB%" -s !SERIAL! shell settings put secure autofill_service null >nul 2>&1
rem Sin animaciones y pantalla siempre encendida: menos carga y la app no queda detras del bloqueo
"%ADB%" -s !SERIAL! shell settings put global window_animation_scale 0 >nul 2>&1
"%ADB%" -s !SERIAL! shell settings put global transition_animation_scale 0 >nul 2>&1
"%ADB%" -s !SERIAL! shell settings put global animator_duration_scale 0 >nul 2>&1
"%ADB%" -s !SERIAL! shell svc power stayon true >nul 2>&1
call :guardar_dispositivo !SERIAL!
call :verificar_app !SERIAL!
call :iniciar_appium
pause
exit /b

:elegir_avd
set "AVD="
set "AVD_ULTIMO="
set "AVD_DEFECTO="
if exist "%ARCHIVO_AVD%" set /p AVD_ULTIMO=<"%ARCHIVO_AVD%"
set /a N=0
echo.
echo Emuladores disponibles en este equipo:
for /f "usebackq delims=" %%a in (`"%EMULATOR%" -list-avds`) do (
    rem Solo nombres de AVD, descarta lineas INFO/WARNING del emulador
    echo %%a| findstr /r /c:"^[A-Za-z0-9_.-]*$" >nul && (
        set /a N+=1
        set "AVD_!N!=%%a"
        if "%%a"=="!AVD_ULTIMO!" (
            set "AVD_DEFECTO=%%a"
            echo   !N!. %%a  [ultimo usado]
        ) else (
            echo   !N!. %%a
        )
    )
)
if !N!==0 (
    echo No hay emuladores creados. Cree uno desde Android Studio - Device Manager.
    pause
    exit /b
)
if !N!==1 (
    set "AVD=!AVD_1!"
    echo Unico emulador: !AVD!
    goto elegir_avd_fin
)
set "SEL="
if defined AVD_DEFECTO (
    set /p "SEL=Numero del emulador (Enter = !AVD_DEFECTO!): "
    if "!SEL!"=="" set "AVD=!AVD_DEFECTO!"
) else (
    set /p "SEL=Numero del emulador: "
)
if not defined AVD if defined SEL set "AVD=!AVD_%SEL%!"
if not defined AVD (
    echo Opcion no valida.
    exit /b
)
:elegir_avd_fin
(echo !AVD!)>"%ARCHIVO_AVD%"
exit /b

:emulador_activo
set "SERIAL="
for /f "usebackq tokens=1,2" %%a in (`"%ADB%" devices`) do (
    echo %%a | findstr /b "emulator-" >nul && if "%%b"=="device" set "SERIAL=%%a"
)
exit /b

:esperar_emulador
set /a INTENTOS=0
:esperar_emulador_ciclo
call :emulador_activo
if defined SERIAL exit /b
set /a INTENTOS+=1
if !INTENTOS! gtr 60 exit /b
timeout /t 3 /nobreak >nul
goto esperar_emulador_ciclo

:esperar_arranque
echo Esperando que Android termine de arrancar en %1 ...
set /a INTENTOS=0
:esperar_arranque_ciclo
set "BOOT="
for /f "usebackq delims=" %%b in (`"%ADB%" -s %1 shell getprop sys.boot_completed 2^>nul`) do set "BOOT=%%b"
if "!BOOT!"=="1" (
    echo Dispositivo listo.
    exit /b
)
set /a INTENTOS+=1
if !INTENTOS! gtr 100 (
    echo Android no termino de arrancar a tiempo.
    exit /b
)
timeout /t 3 /nobreak >nul
goto esperar_arranque_ciclo

rem ================== 3. Solo Appium + dispositivo ==================
:solo_appium
call :iniciar_appium
call :elegir_dispositivo
if defined SERIAL call :verificar_app !SERIAL!
pause
exit /b

:elegir_dispositivo
set "SERIAL="
set /a N=0
echo.
echo Dispositivos conectados:
for /f "usebackq skip=1 tokens=1,2" %%a in (`"%ADB%" devices`) do (
    if "%%b"=="device" (
        set /a N+=1
        set "DEV_!N!=%%a"
        set "MODELO="
        for /f "usebackq delims=" %%m in (`"%ADB%" -s %%a shell getprop ro.product.model`) do set "MODELO=%%m"
        echo   !N!. %%a  !MODELO!
    )
    if "%%b"=="unauthorized" echo   - %%a  NO AUTORIZADO: acepte la depuracion USB en el telefono
)
if !N!==0 (
    echo No hay dispositivos. Conecte el telefono por USB con la depuracion USB activada.
    exit /b
)
set "SEL=1"
if !N! gtr 1 set /p "SEL=Numero del dispositivo: "
set "SERIAL=!DEV_%SEL%!"
if not defined SERIAL (
    echo Opcion no valida.
    exit /b
)
call :guardar_dispositivo !SERIAL!
echo Dispositivo seleccionado: !SERIAL!
exit /b

:verificar_app
"%ADB%" -s %1 shell pm list packages %APP_PAQUETE% | findstr /c:"%APP_PAQUETE%" >nul
if not errorlevel 1 (
    echo App Mi Banco instalada en %1.
    exit /b
)
echo La app Mi Banco NO esta instalada en %1.
set "APK="
for %%f in (*.apk) do set "APK=%%f"
rem La version vigente se deja en app\; se toma la mas reciente
for /f "delims=" %%f in ('dir /b /o-d "app\*.apk" 2^>nul') do if not defined APK set "APK=app\%%f"
if not defined APK (
    echo No hay un APK en la carpeta del proyecto ni en app\ para instalar.
    exit /b
)
set "RESP="
set /p "RESP=Desea instalar !APK! ? (S/N): "
if /i "!RESP!"=="S" "%ADB%" -s %1 install -r "!APK!"
exit /b

:iniciar_appium
call :appium_activo
if "!APPIUM_OK!"=="1" (
    echo Appium ya esta en ejecucion en %APPIUM_URL%
    exit /b
)
echo Iniciando Appium ...
start "Servidor Appium" cmd /k appium --address 127.0.0.1 --port 4723 --allow-insecure "*:get_server_logs" --log-timestamp --local-timezone
set /a INTENTOS=0
:iniciar_appium_ciclo
timeout /t 2 /nobreak >nul
call :appium_activo
if "!APPIUM_OK!"=="1" (
    echo Appium listo en %APPIUM_URL%
    exit /b
)
set /a INTENTOS+=1
if !INTENTOS! gtr 30 (
    echo Appium no respondio. Revise la ventana "Servidor Appium".
    exit /b
)
goto iniciar_appium_ciclo

:appium_activo
set "APPIUM_OK=0"
for /f %%c in ('curl -s -o nul -w "%%{http_code}" %APPIUM_URL%/status 2^>nul') do if "%%c"=="200" set "APPIUM_OK=1"
exit /b

rem ================== 4. Ejecutar ==================
:ejecutar
call :leer_dispositivo
if "!DISPOSITIVO!"=="(ninguno)" (
    echo Primero inicialice el ambiente ^(opcion 2^) o reconozca un dispositivo ^(opcion 3^).
    pause
    exit /b
)
"%ADB%" -s !DISPOSITIVO! get-state >nul 2>&1
if errorlevel 1 (
    echo El dispositivo !DISPOSITIVO! no esta conectado. Use la opcion 2 o 3.
    pause
    exit /b
)
call :appium_activo
if not "!APPIUM_OK!"=="1" call :iniciar_appium
if not "!APPIUM_OK!"=="1" (
    pause
    exit /b
)
call :memoria_libre
echo.
if !MEM_LIBRE_MB! lss 2048 (
    echo *** ATENCION: el equipo solo tiene !MEM_LIBRE_MB! MB de RAM libre. ***
    echo Con menos de 2 GB libres adb pierde la conexion con el emulador y las pruebas fallan al azar.
    echo Cierre navegadores, Teams, editores y demas programas pesados antes de continuar.
    echo.
)
echo Recuerde: guarde y cierre el archivo de control y el listado de clientes antes de continuar.
pause
echo Ejecutando pruebas en !DISPOSITIVO! ...
call mvn -q -B -Ddevice.udid=!DISPOSITIVO! clean test-compile exec:java
if errorlevel 2 (
    echo.
    echo *** EJECUCION DETENIDA POR INDISPONIBILIDAD DEL SISTEMA ***
    echo Las filas no ejecutadas quedaron PENDIENTE. Vuelva a ejecutar en otra oportunidad.
)
echo.
call :ultima_ejecucion
if defined ULTIMA (
    set "RESP="
    set /p "RESP=Desea abrir el reporte de esta ejecucion? (S/N): "
    if /i "!RESP!"=="S" call :abrir_reporte "!ULTIMA!"
)
pause
exit /b

:ultima_ejecucion
set "ULTIMA="
if not exist ejecuciones exit /b
for /f "delims=" %%d in ('dir /b /ad /o-n ejecuciones 2^>nul') do (
    set "ULTIMA=%%d"
    exit /b
)
exit /b

rem ================== 5. Reporte ==================
:consultar_reporte
if not exist ejecuciones (
    echo Aun no hay ejecuciones.
    pause
    exit /b
)
set /a N=0
echo.
echo Ultimas ejecuciones:
for /f "delims=" %%d in ('dir /b /ad /o-n ejecuciones 2^>nul') do (
    if !N! lss 10 (
        set /a N+=1
        set "EJE_!N!=%%d"
        echo   !N!. %%d
    )
)
if !N!==0 (
    echo Aun no hay ejecuciones.
    pause
    exit /b
)
set "SEL="
set /p "SEL=Numero de la ejecucion (Enter = la mas reciente): "
if "!SEL!"=="" set "SEL=1"
set "EJE=!EJE_%SEL%!"
if not defined EJE (
    echo Opcion no valida.
    pause
    exit /b
)
echo   1. Ver reporte Allure
echo   2. Abrir carpeta de la ejecucion (evidencias y control de resultados)
set "SEL="
set /p "SEL=Opcion: "
if "!SEL!"=="1" call :abrir_reporte "!EJE!"
if "!SEL!"=="2" start "" explorer "ejecuciones\!EJE!"
exit /b

:abrir_reporte
if exist "ejecuciones\%~1\reporte_portable\index.html" (
    start "" "ejecuciones\%~1\reporte_portable\index.html"
) else (
    echo La ejecucion %~1 no tiene reporte generado.
    pause
)
exit /b

rem ================== 6. Prerrequisitos ==================
:prerrequisitos
echo.
call :verificar_comando java "Java (JDK 17+)"
call :verificar_comando mvn "Maven"
call :verificar_comando node "Node.js"
call :verificar_comando appium "Appium"
call :verificar_comando curl "curl"
if exist "%ADB%" (echo   [OK]    Android SDK - adb) else (echo   [FALTA] Android SDK - adb en %ADB%)
if exist "%EMULATOR%" (echo   [OK]    Android SDK - emulador) else (echo   [FALTA] Android SDK - emulador)
call appium driver list --installed 2>&1 | findstr /i "uiautomator2" >nul
if errorlevel 1 (echo   [FALTA] Driver uiautomator2 de Appium: appium driver install uiautomator2) else (echo   [OK]    Driver uiautomator2 de Appium)
if exist "%ALLURE_DIR%\bin\allure.bat" (
    echo   [OK]    Allure
) else (
    echo   [...]   Descargando Allure y dependencias del proyecto ...
    call mvn -q -B initialize dependency:resolve
    if exist "%ALLURE_DIR%\bin\allure.bat" (echo   [OK]    Allure) else (echo   [FALTA] Allure: revise la conexion a internet)
)
if exist "%CONTROL%" (echo   [OK]    Archivo de control) else (echo   [FALTA] %CONTROL%)
if exist "datos\pool_clientes.xlsx" (echo   [OK]    Listado de clientes) else (echo   [FALTA] datos\pool_clientes.xlsx)
pause
exit /b

:verificar_comando
where %1 >nul 2>&1 && echo   [OK]    %~2 || echo   [FALTA] %~2
exit /b

rem ================== Utilidades ==================
:leer_dispositivo
set "DISPOSITIVO=(ninguno)"
if exist "%ARCHIVO_DISPOSITIVO%" set /p DISPOSITIVO=<"%ARCHIVO_DISPOSITIVO%"
exit /b

:guardar_dispositivo
(echo %1)>"%ARCHIVO_DISPOSITIVO%"
exit /b

:memoria_libre
set "MEM_LIBRE_MB=99999"
for /f %%m in ('powershell -NoProfile -Command "[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)" 2^>nul') do set "MEM_LIBRE_MB=%%m"
exit /b
