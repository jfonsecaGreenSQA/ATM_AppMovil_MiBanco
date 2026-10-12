# Automatización App Móvil Mi Banco (ATM)

Automatización de pruebas funcionales de la app móvil **Mi Banco** (Android) desarrollada por **GreenSQA**.
Los casos de prueba vienen de Azure DevOps y se automatizan uno por uno con Appium, Cucumber (BDD en español) y Java.
Toda la operación (ambiente, ejecución y reportes) se hace desde el menú `ATM_MiBanco.bat`, y lo que se ejecuta lo define un archivo Excel de control.

## Estado de los casos

| Escenario | Casos de Azure | Estado |
|---|---|---|
| Registro exitoso | 38498 (CC) · 38502 (CE) | **Automatizado** (un escenario, ambos tags). CE sin ejecutar: no hay clientes CE disponibles sin la línea de prueba |
| Registro exitoso con envío de SMS | 38499 (CC) · 38503 (CE) | **Construido** (un escenario, ambos tags). 38503 ejecutado en el S23 el 09/10: registro exitoso y SMS recibido; la validación automática del SMS falló por un defecto de la espera, ya corregido. Quedan 2 clientes CE con la línea sin registrar |
| Login exitoso → Home | 38519 (CC) · 38520 (CE) | **Automatizado con CC y CE** (un escenario, ambos tags) |
| Recuperación de clave | 38529 · 38526 | Pendiente |
| Home: encabezado | 38699 (saludo y pregunta) · 38700 (+ último ingreso) | **Automatizado**, un escenario por caso. El nombre del saludo no se valida |
| Home: accesos directos | 38703 (Bre-B) · 38704 (Pedir Crédito) · 38711 (menú inferior) | **Automatizado**. Identidad visual y estado activo de Inicio: manual |
| Home: productos, banner, skeleton | 38698, 38702, 38705 a 38710 | Pendiente |
| Logs (38500, 38504, 38521) | — | Fuera de alcance por ahora |

El detalle de qué casos se agrupan en un mismo escenario está en [Casos de Prueba/Consolidacion_Casos_de_Prueba.docx](Casos%20de%20Prueba/Consolidacion_Casos_de_Prueba.docx).

## Stack

| Componente | Versión |
|---|---|
| Java | 17 o superior |
| Maven | 3.9+ |
| Appium | 3.x con driver `uiautomator2` |
| Appium java-client | 9.3.0 (Selenium 4.25.0) |
| Cucumber | 7.20.1 |
| Allure | 2.29.0 (se descarga solo en `herramientas/`) |
| Apache POI | 5.3.0 (lectura y escritura de Excel) |

La app es híbrida: Capacitor (WebView) que carga la interfaz desde Modyo (ambiente *certification*). Los `id` del HTML se exponen como `resource-id` en UiAutomator2 y así se ubican los elementos.

## Prerrequisitos

- Windows con Java 17+, Maven, Node.js, Appium (`npm i -g appium`) y su driver (`appium driver install uiautomator2`).
- Android SDK en `%LOCALAPPDATA%\Android\Sdk` (o la variable `ANDROID_HOME`) con `platform-tools` y el emulador.
- Un emulador creado en Android Studio (Device Manager) **o** un celular Android con depuración USB activa.
- **Conexión desde Colombia.** El backend del banco rechaza solicitudes con el aviso "Validación fallida... por este canal". Fuera de Colombia, el equipo (o el celular) debe salir por una VPN ubicada en Colombia.

La opción **6** del menú verifica todo lo anterior y descarga Allure y las dependencias de Maven.

### Archivos que no vienen en el repositorio

| Archivo | Dónde va | Cómo se obtiene |
|---|---|---|
| APK de la app (`*.apk`) | Raíz del proyecto | Es confidencial; solicitarlo al líder QA. El menú lo instala si el dispositivo no tiene la app |
| Sábana de cartera y `pool_clientes.xlsx` | `datos/` | Contienen datos reales de clientes; solicitarlos al líder QA. No se publican ni se comparten fuera del proyecto |
| Logos del reporte | `config/branding/` | Ver `config/branding/LEEME.txt` |

## Inicio rápido

Ejecute `ATM_MiBanco.bat` (doble clic) y use el menú:

| Opción | Qué hace |
|---|---|
| 1. Abrir archivo de control | Abre `control/Control_Ejecucion.xlsx` para elegir qué se ejecuta |
| 2. Inicializar ambiente (Emulador + Appium) | Inicia el emulador elegido, espera a que arranque, desactiva el autocompletado de claves, verifica la app e inicia Appium |
| 3. Inicializar solo Appium y reconocer dispositivo | Para un celular físico: lo detecta por USB, lo deja seleccionado, instala la app si falta e inicia Appium |
| 4. Ejecutar pruebas | Compila desde cero (`mvn clean test-compile`) y ejecuta lo marcado en el archivo de control |
| 5. Consultar reporte | Abre el reporte Allure de una ejecución anterior |
| 6. Verificar prerrequisitos | Revisa herramientas, Allure, archivo de control y listado de clientes |

Flujo normal: **2** (o **3** con celular) → **1** → **4**. Cierre el Excel de control y el listado de clientes antes de ejecutar.

También se puede llamar una opción directa: `ATM_MiBanco.bat 4`.

## Archivo de control (`control/Control_Ejecucion.xlsx`)

Cada fila de la hoja **Control** es una iteración. Las columnas se ubican por nombre de encabezado:

| Columna | Uso |
|---|---|
| Caso | Caso de la lista desplegable (solo aparecen los construidos), p. ej. `38498 - Registro exitoso con CC` |
| IdPrueba | Identificador único de la fila (p. ej. `REG-001`); nombra la carpeta de evidencias |
| Ejecutar | `SI` para ejecutar; cualquier otro valor la omite |
| TipoDocumento | `CC` (CE aún no habilitado) |
| NumDocumento | Número de 6 a 11 dígitos, o `AUTO` para tomar un cliente disponible del listado |
| Clave | Vacía = se genera una que cumple RN-R07 (4 dígitos, sin 3 iguales consecutivos) |
| OTP | Vacío = OTP de certificación configurado (`otp.defecto`) |
| Observaciones | Libre |

Las hojas **Instructivo** y **Casos de Prueba** del mismo archivo documentan su uso.

## Listado de clientes (`datos/pool_clientes.xlsx`)

Fuente única de los clientes usados en las pruebas (hoja **Clientes**). Reglas: tipo de documento CC y solo clientes con `ESTADOCLIENTE = ACTIVO` en la sábana.

| ESTADO_REGISTRO | Significado |
|---|---|
| DISPONIBLE | Se puede usar para registrarse (`AUTO` toma el primero) |
| EN_USO | Reservado por una iteración en curso |
| REGISTRADO | Registrado por la automatización; la columna CLAVE guarda su clave (sirve para login) |
| YA_REGISTRADO_PREVIO | Ya estaba registrado antes; se depura al final de cada ejecución |
| FALLIDO | La iteración falló sin confirmar el registro; no se reutiliza automáticamente |

Cada cambio de estado se guarda de inmediato porque el registro de un cliente es irrepetible.

La columna `LINEA_SMS = SI` marca los clientes cuyo celular registrado es la línea de la SIM del celular de pruebas. Solo los casos de SMS los usan (con `AUTO`) y los demás casos nunca los toman. El login (`AUTO`) toma un cliente REGISTRADO con clave y no lo consume.

### Casos de SMS

Se ejecutan en el celular físico con la SIM de la línea de prueba (opción **3** del menú). La automatización lee la bandeja de SMS con UiAutomator2 (`mobile: listSms`) por medio de la app *Appium Settings*, que necesita el permiso de SMS; si el fabricante lo bloquea, concédalo en Ajustes › Aplicaciones. Solo se tienen en cuenta los SMS que llegan después de iniciar la iteración. Los textos esperados se configuran en `sms.patron.ingreso` y `sms.patron.registro`.

### Sesión

Los escenarios `@login` y `@home` cierran sesión al terminar (Más › Cerrar sesión), en un hook que corre antes de cerrar el driver. Si la sesión queda abierta, el siguiente ingreso del mismo cliente responde "Sesión activa" hasta que venza en el servidor.

## Resultados de una ejecución

Cada ejecución crea `ejecuciones/AAAAMMDD_HHMMSS/` con:

- `reporte_portable/index.html`: reporte Allure en un solo archivo, para compartir.
- `allure-report/`: reporte Allure completo, con historial de ejecuciones anteriores.
- `Control_Resultado.xlsx`: copia del control con las columnas Resultado, Detalle, DocumentoUsado (enmascarado) e Inicio.
- `evidencias/<IdPrueba>/`: captura y árbol de UI por paso, video, logcat y log de Appium.

| Resultado | Significado |
|---|---|
| EXITOSA | Pasaron todos los pasos |
| FALLIDA | Falló una validación o un paso, o el aviso de indisponibilidad persistió tras los reintentos |
| PENDIENTE | No se ejecutó porque la ejecución se detuvo por indisponibilidad; reprogramar |
| OMITIDA | `Ejecutar` distinto de `SI` |
| NO EJECUTADA | La fila no cumple las reglas del caso (dato inválido, caso no construido, IdPrueba duplicado) |

**Indisponibilidad del sistema:** el backend devuelve de forma intermitente el aviso "Validación fallida. No es posible atender tu solicitud por este canal". Las acciones que llaman al banco (Continuar con el documento, Validar OTP, Continuar en la clave e Ingresar) aceptan el aviso y repiten la acción hasta `indisponibilidad.reintentos` (2) veces; en el login se vuelve a escribir la clave, porque el aviso la borra. Si el aviso persiste, la iteración queda **FALLIDA por indisponibilidad** con evidencia y el cliente del listado vuelve a DISPONIBLE. Al llegar a `indisponibilidad.max.iteraciones` (2) iteraciones fallidas por indisponibilidad se detiene la ejecución y el resto queda PENDIENTE.

Código de salida: `0` sin fallas, `1` con fallas, `2` detenida por indisponibilidad.

## Arquitectura

```
src/test/java/com/greensqa/mibanco/atm/
├── runners/      Orquestador: lee el control, ejecuta cada fila como iteración de Cucumber y genera el reporte
├── control/      Lectura del control, catálogo de casos (CasoPrueba), motor de reglas y escritura de resultados
├── steps/        Step definitions (Gherkin en español)
├── business/     Componentes de negocio que orquestan pantallas
├── screens/      Contratos de pantalla (interfaces) independientes de la plataforma
│   └── android/  Page Objects Android: únicos dueños de los selectores
├── data/         Listado de clientes, tipos de documento, generador de clave y enmascarado
├── hooks/        Ciclo de vida de cada iteración, evidencias por paso y detección de indisponibilidad
├── driver/       Sesión de Appium (UiAutomator2)
├── evidence/     Capturas, video, logcat y log de Appium
├── report/       Enriquecimiento y generación del reporte Allure
├── context/      Estado de la iteración en curso
└── config/       Lectura de ambiente.properties
src/test/resources/
├── features/     Escenarios .feature (un tag @IdAzure por caso), uno por proceso:
│   ├── autenticacion/autenticacion.feature   Registro, login y recuperacion de clave
│   └── home/home.feature                     Home (se crea con su primer caso)
└── config/ambiente.properties
```

Los steps dependen de contratos (`screens/`), no de selectores; así la versión iOS puede agregarse sin tocarlos.

### Configuración

`src/test/resources/config/ambiente.properties` (sin secretos). Cualquier clave se sobrescribe con `-Dclave=valor`, por ejemplo `-Ddevice.udid=emulator-5554`. Las más usadas: `appium.url`, `device.udid`, `espera.segundos`, `otp.defecto`, `indisponibilidad.reintentos`, `indisponibilidad.max.iteraciones`.

### Agregar un caso nuevo

1. Escenario en `features/` con el tag del caso de Azure (`@38519`). Si atiende varios casos, lleva todos sus tags.
2. Entrada en el enum `control/CasoPrueba` y en la hoja **Catalogo** del archivo de control.
3. Reglas de datos del caso en `control/MotorReglas`.
4. Contratos en `screens/`, Page Objects en `screens/android/`, steps y componentes de negocio.

La metodología completa (etapas, puertas de confirmación, etiquetas de evidencia) está en [PROMPT_MAESTRO_SDET_Mobile_v2.0.md](PROMPT_MAESTRO_SDET_Mobile_v2.0.md).

## Problemas conocidos y soluciones

| Síntoma | Causa | Solución aplicada o sugerida |
|---|---|---|
| El OTP queda vacío y "Validar" no se habilita | El componente web del OTP ignora `sendKeys` | Se escribe pulsando teclas reales (`escribirConTeclas`) |
| Se cierra la hoja del OTP o sale "¿Estás seguro que quieres salir?" | `hideKeyboard` de Appium envía la tecla Atrás | El teclado se cierra tocando el encabezado (`ocultarTeclado`) |
| `Unresolved compilation problem` al ejecutar | La extensión Java de VS Code compila en `target/` con un classpath desactualizado | La opción 4 hace `clean`. Para el IDE: clic derecho en `pom.xml` → *Reload Projects* |
| Appium no abre sesión: `adbExec ... timed out` | El emulador quedó colgado al arrancar desde un snapshot dañado | Cerrarlo y abrirlo con *Cold Boot Now* (Device Manager) |
| Todo responde "Validación fallida" | Sistema caído o conexión fuera de Colombia | Esperar, o conectar el equipo o celular por VPN a Colombia |

## Datos sensibles

- La sábana y el listado de clientes contienen datos reales: no se imprimen en logs, chats ni documentos; los reportes enmascaran los documentos.
- El APK es confidencial y está excluido en `.gitignore`.
- Las evidencias (`ejecuciones/`) pueden mostrar datos de clientes en capturas: no se versionan.
