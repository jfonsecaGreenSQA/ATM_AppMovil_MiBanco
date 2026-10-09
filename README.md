# Automatización App Móvil Mi Banco (ATM)

Automatización de pruebas funcionales de la app móvil **Mi Banco** (Android) desarrollada por **GreenSQA**.
Los casos de prueba vienen de Azure DevOps y se automatizan uno por uno con Appium, Cucumber (BDD en español) y Java.
Toda la operación (ambiente, ejecución y reportes) se hace desde el menú `ATM_MiBanco.bat`, y lo que se ejecuta lo define un archivo Excel de control.

## Estado de los casos

| Escenario | Casos de Azure | Estado |
|---|---|---|
| Registro exitoso | 38498 (CC) · 38502 (CE) | **Automatizado con CC.** CE bloqueado por falta de datos |
| Registro exitoso con envío de SMS | 38499 (CC) · 38503 (CE) | Pendiente: requiere dispositivo real con línea asociada al cliente |
| Login exitoso → Home | 38519 (CC) · 38520 (CE) | En construcción (pantalla de login mapeada) |
| Recuperación de clave | 38529 · 38526 | Pendiente |
| Home: encabezado, accesos, productos, banner, skeleton | 38698 a 38711 | Pendiente |
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

## Resultados de una ejecución

Cada ejecución crea `ejecuciones/AAAAMMDD_HHMMSS/` con:

- `reporte_portable/index.html`: reporte Allure en un solo archivo, para compartir.
- `allure-report/`: reporte Allure completo, con historial de ejecuciones anteriores.
- `Control_Resultado.xlsx`: copia del control con las columnas Resultado, Detalle, DocumentoUsado (enmascarado) e Inicio.
- `evidencias/<IdPrueba>/`: captura y árbol de UI por paso, video, logcat y log de Appium.

| Resultado | Significado |
|---|---|
| EXITOSA | Pasaron todos los pasos |
| FALLIDA | Falló una validación o un paso |
| CANCELADA | La app mostró el aviso de indisponibilidad del sistema; no es un resultado de la prueba y el cliente vuelve a quedar disponible |
| PENDIENTE | No se ejecutó porque la ejecución se detuvo por indisponibilidad; reprogramar |
| OMITIDA | `Ejecutar` distinto de `SI` |
| NO EJECUTADA | La fila no cumple las reglas del caso (dato inválido, caso no construido, IdPrueba duplicado) |

**Indisponibilidad del sistema:** el aviso "Validación fallida. No es posible atender tu solicitud por este canal" puede aparecer en cualquier pantalla. Se revisa después de cada paso: si aparece, la iteración queda CANCELADA con evidencia. Al llegar a `indisponibilidad.max.iteraciones` (2) se detiene la ejecución y el resto queda PENDIENTE. Si no aparece, no afecta el flujo.

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
├── features/     Escenarios .feature (un tag @IdAzure por caso)
└── config/ambiente.properties
```

Los steps dependen de contratos (`screens/`), no de selectores; así la versión iOS puede agregarse sin tocarlos.

### Configuración

`src/test/resources/config/ambiente.properties` (sin secretos). Cualquier clave se sobrescribe con `-Dclave=valor`, por ejemplo `-Ddevice.udid=emulator-5554`. Las más usadas: `appium.url`, `device.udid`, `espera.segundos`, `otp.defecto`, `indisponibilidad.max.iteraciones`.

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
