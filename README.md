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
| Recuperación de clave | 38529 · 38526 | **Construido** (un escenario, ambos tags). En la app la clave nueva se pide antes del OTP; el evento del OTP no se ve en la UI |
| Home: encabezado | 38699 (saludo y pregunta) · 38700 (+ último ingreso) | **Automatizado**, un escenario por caso. El nombre del saludo no se valida |
| Home: accesos directos | 38703 (Bre-B) · 38704 (Pedir Crédito) · 38711 (menú inferior) | **Automatizado**. Identidad visual y estado activo de Inicio: manual |
| Home: productos del cliente | 38705 (ahorro) · 38707 (préstamos / libre inversión) · 38708 (rotativo) · 38702 (pre-aprobado) · 38706 (corriente) · 38709 (saldos) | **Automatizado** (un escenario, seis tags). 38705, 38707, 38708 y 38702 ejecutados con éxito el 12/10. 38706 construido, sin datos: ningún cliente registrado tiene cuenta corriente. 38709 construido y verificado en los dos sentidos; solo valida el formato y el valor del saldo (la información de cada tarjeta es de 38705 a 38708); falta el monto esperado del backend (columna `Monto`) |
| Home: skeleton de carga | 38698 | **Automatizado** el 12/10. Se observa leyendo la página por DevTools del WebView (ver Arquitectura) |
| Home: banner "Exclusivo para ti" | 38710 | **Automatizado** el 15/10: valida la sección y que al deslizar el banner pase a la siguiente campaña (con una sola campaña queda como observación) |
| Cuentas: productos y saldos del Home | 39561 · 39562 · 39563 · 39564 · 39565 | **Automatizado** el 15/10: repiten el texto de 38705 a 38709 y usan el mismo escenario (sus tags están en `home.feature`). 39561, 39563 y 39564 ejecutados con éxito; 39562 sin datos (cuenta corriente) y 39565 sin monto del backend |
| Cuentas: detalle de cuenta | 39566 | **Automatizado** el 15/10 (`cuentas.feature`). Movimientos: acepta la lista o el aviso de que no hay. Ejecutado con éxito con y sin movimientos |
| Cuentas: movimientos, detalle de transacción y comprobante | 39567 · 39568 · 39569 · 39570 · 39571 · 39572 · 39573 | **Automatizado** el 19/10 (`cuentas.feature`) y ejecutado con éxito con un cliente con movimientos (documento y clave explícitos en el control; con `AUTO` los clientes del listado no tienen movimientos). El corte en 20 y la exclusión de movimientos de más de un mes quedan como observación hasta tener datos que los permitan. Descargar: la imagen se adjunta a la evidencia y se borra del dispositivo. Compartir: se cancela sin elegir app; el video se detiene antes porque el menú muestra contactos del dispositivo |
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
| 2. Inicializar ambiente (Emulador + Appium) | Si ya hay un emulador abierto lo usa; si no, lista los emuladores de este equipo (si hay uno solo lo toma; Enter = el último usado, guardado en `config/emulador_actual.txt`) y lo inicia en frío. Espera a que arranque, desactiva autocompletado de claves y animaciones, verifica la app e inicia Appium |
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
| TipoDocumento | `CC` o `CE`, el del caso |
| NumDocumento | Número de 6 a 11 dígitos, o `AUTO` para tomar un cliente del listado |
| Clave | Con `NumDocumento` = `AUTO`: vacía (registro y recuperación generan una que cumple RN-R07, 4 dígitos sin 3 iguales consecutivos; login y Home usan la del listado). **Con un número de documento informado la clave es obligatoria**: la que se crea en el registro, la actual en login/Home/productos o la nueva en la recuperación; si falta, la fila queda NO EJECUTADA |
| OTP | Vacío = OTP de certificación configurado (`otp.defecto`) |
| TipoProducto | Solo casos de productos del Home: `AHORRO`, `CORRIENTE`, `PRESTAMO`, `ROTATIVO`, `PRE_APROBADO`. Vacío = el producto del caso; obligatorio en 38709 |
| Monto | Solo casos de productos del Home: saldo esperado según el backend (p. ej. `22746864.51`). Vacío = solo se valida el formato del monto en la app (`$22.746.864,51`); obligatorio en 38709 |
| Observaciones | Libre |

Las hojas **Instructivo** y **Casos de Prueba** del mismo archivo documentan su uso. La lista de casos (hoja oculta **Catalogo**) está en orden alfabético.

Los textos que valida la automatización en el Home son los observados en la app. Las diferencias con el texto de los casos de Azure están en la sección 9 (control de cambios) de [Consolidacion_Casos_de_Prueba.docx](Casos%20de%20Prueba/Consolidacion_Casos_de_Prueba.docx).

## Listado de clientes (`datos/pool_clientes.xlsx`)

Fuente única de los clientes usados en las pruebas (hoja **Clientes**). Reglas: tipo de documento CC y solo clientes con `ESTADOCLIENTE = ACTIVO` en la sábana.

| ESTADO_REGISTRO | Significado |
|---|---|
| DISPONIBLE | Se puede usar para registrarse (`AUTO` toma el primero) |
| EN_USO | Reservado por una iteración en curso |
| REGISTRADO | Registrado por la automatización; la columna CLAVE guarda su clave (sirve para login) |
| YA_REGISTRADO_PREVIO | Ya estaba registrado antes; se depura al final de cada ejecución |
| FALLIDO | La iteración falló sin confirmar el registro, o la app rechazó la clave guardada ("Datos incorrectos"); no se reutiliza automáticamente |

Cada cambio de estado se guarda de inmediato porque el registro de un cliente es irrepetible.

La columna `LINEA_SMS = SI` marca los clientes cuyo celular registrado es la línea de la SIM del celular de pruebas. Solo los casos de SMS los usan (con `AUTO`) y los demás casos nunca los toman. El login (`AUTO`) toma un cliente REGISTRADO con clave y no lo consume.

La recuperación de clave (`AUTO`) toma el mismo cliente REGISTRADO que el login y **le cambia la clave**: al tocar Validar en el OTP lo marca EN_USO y, con la confirmación en pantalla, guarda la clave nueva y lo deja REGISTRADO. Si la iteración falla después de enviar el cambio, queda FALLIDO con la observación "Posible cambio de clave" para revisarlo a mano (no vuelve a DISPONIBLE ni siquiera por indisponibilidad).

### Productos del Home (columna `PRODUCTOS_HOME`)

No todos los clientes tienen todos los productos. Cada vez que un cliente entra al Home (login, casos del Home o de productos), la automatización guarda en `PRODUCTOS_HOME` los productos que mostró (`AHORRO,PRESTAMO,ROTATIVO,PRE_APROBADO`, o `NINGUNO`). En los casos de productos con `AUTO` se toma primero un cliente que ya mostró el producto y luego los que aún no se han revisado, hasta `productos.max.clientes` (3). Si la app rechaza la clave de un candidato ("Datos incorrectos"), queda FALLIDO y se prueba con otro, sin llegar al tercer intento que bloquea el acceso. Si ninguno tiene el producto, la fila falla con "Dato no apto".

### Casos de SMS

Se ejecutan en el celular físico con la SIM de la línea de prueba (opción **3** del menú). La automatización lee la bandeja de SMS con UiAutomator2 (`mobile: listSms`) por medio de la app *Appium Settings*, que necesita el permiso de SMS; si el fabricante lo bloquea, concédalo en Ajustes › Aplicaciones. Solo se tienen en cuenta los SMS que llegan después de iniciar la iteración. Los textos esperados se configuran en `sms.patron.ingreso` y `sms.patron.registro`.

### Sesión

Los escenarios `@login` y `@home` cierran sesión al terminar (Más › Cerrar sesión), en un hook que corre antes de cerrar el driver; antes de cerrarla registran los productos del Home en el listado. Si la sesión queda abierta, el siguiente ingreso del mismo cliente responde "Sesión activa" hasta que venza en el servidor.

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
├── driver/       Sesión de Appium (UiAutomator2), adb y lectura del WebView por DevTools (WebViewDevTools)
├── evidence/     Capturas, video, logcat y log de Appium
├── report/       Enriquecimiento y generación del reporte Allure
├── context/      Estado de la iteración en curso
└── config/       Lectura de ambiente.properties
src/test/resources/
├── features/     Escenarios .feature (un tag @IdAzure por caso), uno por proceso:
│   ├── autenticacion/autenticacion.feature   Registro, login y recuperacion de clave
│   └── home/home.feature                     Home: encabezado, accesos, productos y skeleton
└── config/ambiente.properties
```

Los steps dependen de contratos (`screens/`), no de selectores; así la versión iOS puede agregarse sin tocarlos.

**Lectura del WebView por DevTools.** Mientras el Home carga, cada consulta por UiAutomator tarda de 1 a 5 s y el skeleton dura unos 4 s. Para el 38698 la página se lee por Chrome DevTools: el APK de pruebas es debuggable y el WebView publica el socket `webview_devtools_remote_<pid>`. `WebViewDevTools` hace `adb forward` a ese socket y evalúa JavaScript de solo lectura por WebSocket (~70 ms por lectura); el WebView rechaza conexiones con encabezado `Origin` y el WebSocket de Java no lo envía. Requiere un APK debuggable.

### Configuración

`src/test/resources/config/ambiente.properties` (sin secretos). Cualquier clave se sobrescribe con `-Dclave=valor`, por ejemplo `-Ddevice.udid=emulator-5554`. Las más usadas: `appium.url`, `device.udid`, `espera.segundos`, `otp.defecto`, `indisponibilidad.reintentos`, `indisponibilidad.max.iteraciones`, `productos.max.clientes`.

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
| Falla un paso porque la app quedó en otra pantalla (p. ej. "Iniciar sesión" no aparece y la app ya está en el login) | Alguien tocó el celular durante la ejecución (observado el 12/10 en el S23: el logcat registró un toque que no hizo la automatización) | No usar el dispositivo mientras corre. Para confirmarlo, buscar `ViewPostIme pointer` en el logcat de la evidencia y compararlo con los tiempos de los pasos en Allure |
| El 38698 no ve el skeleton | Con UiAutomator cada consulta tarda 1-5 s durante la carga y el skeleton dura ~4 s | Se lee la página por DevTools del WebView (requiere APK debuggable) |
| "Datos incorrectos" al ingresar | La clave guardada en el listado ya no corresponde (p. ej. se cambió fuera de la automatización) | El cliente queda FALLIDO. No reintentar a mano más de una vez: al tercer intento la app bloquea el acceso |
| "Sesión activa" al ingresar | Una sesión anterior del mismo cliente quedó abierta (p. ej. una exploración manual sin cerrar sesión) | Esperar a que venza o usar otro cliente; cerrar siempre sesión al explorar |
| La ejecución falla con "instrumentation process is not running" | Otra sesión de Appium contra el mismo dispositivo (p. ej. Appium Inspector) venció y Appium detuvo el servidor UiAutomator2 | No abrir otras sesiones de Appium mientras corre la automatización |
| Recuperación de clave: el cliente queda FALLIDO con "Posible cambio de clave" | La iteración falló después de enviar el OTP y no se sabe qué clave quedó | Entrar con la clave del listado; si no funciona, recuperarla a mano y actualizar CLAVE y ESTADO_REGISTRO = REGISTRADO |

## Datos sensibles

- La sábana y el listado de clientes contienen datos reales: no se imprimen en logs, chats ni documentos; los reportes enmascaran los documentos.
- El APK es confidencial y está excluido en `.gitignore`.
- Las evidencias (`ejecuciones/`) pueden mostrar datos de clientes en capturas: no se versionan.
