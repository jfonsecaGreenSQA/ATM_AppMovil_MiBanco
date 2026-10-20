package com.greensqa.mibanco.atm.control;

import com.greensqa.mibanco.atm.data.TipoDocumento;
import com.greensqa.mibanco.atm.data.TipoProducto;

import java.util.Arrays;
import java.util.Optional;

/**
 * Catalogo de casos construidos. Cada caso se enlaza al escenario Gherkin por su tag (@IdAzure).
 * Al publicar un caso nuevo se agrega aqui y en la hoja Catalogo del archivo de control.
 */
public enum CasoPrueba {

    REGISTRO_EXITOSO_CC("38498", "Registro exitoso con CC", "Registro", TipoDocumento.CC, false),
    REGISTRO_EXITOSO_CE("38502", "Registro exitoso con CE", "Registro", TipoDocumento.CE, false),
    SMS_REGISTRO_EXITOSO_CC("38499", "SMS por registro exitoso con CC", "Registro", TipoDocumento.CC, true),
    SMS_REGISTRO_EXITOSO_CE("38503", "SMS por registro exitoso con CE", "Registro", TipoDocumento.CE, true),
    LOGIN_EXITOSO_CC("38519", "Login exitoso con CC", "Autenticacion", TipoDocumento.CC, false),
    LOGIN_EXITOSO_CE("38520", "Login exitoso con CE", "Autenticacion", TipoDocumento.CE, false),
    RECUPERACION_CLAVE("38529", "Recuperacion de clave con CC", "Recuperacion de clave", TipoDocumento.CC, false),
    RECUPERACION_CLAVE_OTP("38526", "OTP en la recuperacion de clave", "Recuperacion de clave", TipoDocumento.CC, false),
    HOME_SALUDO("38699", "Saludo y pregunta del Home", "Home", TipoDocumento.CC, false),
    HOME_ULTIMO_INGRESO("38700", "Ultimo ingreso en el encabezado del Home", "Home", TipoDocumento.CC, false),
    HOME_ACCESO_BREB("38703", "Acceso Bre-B en la parte superior del Home", "Home", TipoDocumento.CC, false),
    HOME_ACCESO_CREDITO("38704", "Acceso Pedir Credito en la parte superior del Home", "Home", TipoDocumento.CC, false),
    HOME_MENU_INFERIOR("38711", "Menu inferior del Home", "Home", TipoDocumento.CC, false),
    HOME_SKELETON("38698", "Skeleton de carga del Home", "Home", TipoDocumento.CC, false),
    HOME_BANNER("38710", "Banner Exclusivo para ti del Home", "Home", TipoDocumento.CC, false),
    // Productos del Home: un mismo escenario; el producto viene del caso o de la columna TipoProducto (38709)
    HOME_PRODUCTO_AHORRO("38705", "Producto cuenta de ahorro en el Home", "Home", TipoDocumento.CC, TipoProducto.AHORRO),
    HOME_PRODUCTO_CORRIENTE("38706", "Producto cuenta corriente en el Home", "Home", TipoDocumento.CC, TipoProducto.CORRIENTE),
    HOME_PRODUCTO_PRESTAMO("38707", "Producto credito de libre inversion (Prestamos) en el Home", "Home", TipoDocumento.CC,
            TipoProducto.PRESTAMO),
    HOME_PRODUCTO_ROTATIVO("38708", "Producto credito rotativo en el Home", "Home", TipoDocumento.CC, TipoProducto.ROTATIVO),
    HOME_PRODUCTO_PRE_APROBADO("38702", "Credito pre-aprobado en el Home", "Home", TipoDocumento.CC, TipoProducto.PRE_APROBADO),
    HOME_SALDOS("38709", "Saldos de los productos en el Home", "Home", TipoDocumento.CC, null),
    // Cuentas (7x24): 39561 a 39565 repiten el texto de 38705 a 38709 y usan el mismo escenario
    CUENTAS_PRODUCTO_AHORRO("39561", "Producto cuenta de ahorro en el Home (Cuentas)", "Cuentas", TipoDocumento.CC,
            TipoProducto.AHORRO),
    CUENTAS_PRODUCTO_CORRIENTE("39562", "Producto cuenta corriente en el Home (Cuentas)", "Cuentas", TipoDocumento.CC,
            TipoProducto.CORRIENTE),
    CUENTAS_PRODUCTO_PRESTAMO("39563", "Producto credito de libre inversion (Prestamos) en el Home (Cuentas)", "Cuentas",
            TipoDocumento.CC, TipoProducto.PRESTAMO),
    CUENTAS_PRODUCTO_ROTATIVO("39564", "Producto credito rotativo en el Home (Cuentas)", "Cuentas", TipoDocumento.CC,
            TipoProducto.ROTATIVO),
    CUENTAS_SALDOS("39565", "Saldos de los productos en el Home (Cuentas)", "Cuentas", TipoDocumento.CC, null),
    CUENTAS_DETALLE("39566", "Detalle de cuenta de ahorros", "Cuentas", TipoDocumento.CC, false),
    CUENTAS_DETALLE_TRANSACCION("39571", "Detalle de una transaccion de la cuenta", "Cuentas", TipoDocumento.CC, false),
    CUENTAS_DESCARGAR_COMPROBANTE("39572", "Descargar comprobante de una transaccion", "Cuentas", TipoDocumento.CC, false),
    CUENTAS_COMPARTIR_COMPROBANTE("39573", "Compartir comprobante de una transaccion", "Cuentas", TipoDocumento.CC, false),
    // 39567 (principal), 39569 y 39570: un mismo escenario
    CUENTAS_MOVIMIENTOS_MAXIMO_20("39567", "Detalle de cuenta muestra maximo 20 movimientos", "Cuentas", TipoDocumento.CC, false),
    CUENTAS_MOVIMIENTOS_SIN_VER_MAS("39569", "Sin boton Ver mas ni carga adicional de movimientos", "Cuentas", TipoDocumento.CC, false),
    CUENTAS_MOVIMIENTOS_SIN_PAGINACION("39570", "Sin paginacion ni scroll infinito de movimientos", "Cuentas", TipoDocumento.CC, false),
    CUENTAS_MOVIMIENTOS_ULTIMO_MES("39568", "Detalle de cuenta muestra movimientos del ultimo mes", "Cuentas", TipoDocumento.CC, false);

    private final String idAzure;
    private final String nombre;
    private final String proceso;
    private final TipoDocumento tipoDocumento;
    private final boolean requiereLineaSms;
    private final TipoProducto producto;
    private final boolean deProductos;

    CasoPrueba(String idAzure, String nombre, String proceso, TipoDocumento tipoDocumento, boolean requiereLineaSms) {
        this.idAzure = idAzure;
        this.nombre = nombre;
        this.proceso = proceso;
        this.tipoDocumento = tipoDocumento;
        this.requiereLineaSms = requiereLineaSms;
        this.producto = null;
        this.deProductos = false;
    }

    /** Caso de productos del Home; {@code producto} null = lo define la columna TipoProducto del control. */
    CasoPrueba(String idAzure, String nombre, String proceso, TipoDocumento tipoDocumento, TipoProducto producto) {
        this.idAzure = idAzure;
        this.nombre = nombre;
        this.proceso = proceso;
        this.tipoDocumento = tipoDocumento;
        this.requiereLineaSms = false;
        this.producto = producto;
        this.deProductos = true;
    }

    public String idAzure() {
        return idAzure;
    }

    public String nombre() {
        return nombre;
    }

    public String proceso() {
        return proceso;
    }

    /** Tipo de documento que cubre el caso (en Azure, CC y CE son casos distintos). */
    public TipoDocumento tipoDocumento() {
        return tipoDocumento;
    }

    /** El cliente debe tener como celular la linea de la SIM del dispositivo (LINEA_SMS = SI en el listado). */
    public boolean requiereLineaSms() {
        return requiereLineaSms;
    }

    /** Producto que valida el caso de productos del Home; vacio si lo define el control (38709) o no aplica. */
    public Optional<TipoProducto> producto() {
        return Optional.ofNullable(producto);
    }

    public boolean deProductos() {
        return deProductos;
    }

    /** Caso de saldos (38709, 39565): compara el Monto del control y de cada tarjeta solo valida el formato del saldo. */
    public boolean validaSaldos() {
        return deProductos && producto == null;
    }

    public String tag() {
        return "@" + idAzure;
    }

    /** Texto con el que aparece en la lista desplegable de la columna Caso. */
    public String textoCatalogo() {
        return idAzure + " - " + nombre;
    }

    /** Reconoce el caso por el Id de Azure al inicio del texto ("38498 - ..."). */
    /** Prefijo del IdPrueba que se genera cuando el control no lo trae (p. ej. REG-CC-001). */
    public String prefijoIdPrueba() {
        return switch (this) {
            case REGISTRO_EXITOSO_CC -> "REG-CC";
            case REGISTRO_EXITOSO_CE -> "REG-CE";
            case SMS_REGISTRO_EXITOSO_CC -> "SMS-CC";
            case SMS_REGISTRO_EXITOSO_CE -> "SMS-CE";
            case LOGIN_EXITOSO_CC -> "LOG-CC";
            case LOGIN_EXITOSO_CE -> "LOG-CE";
            case RECUPERACION_CLAVE -> "REC-CLAVE";
            case RECUPERACION_CLAVE_OTP -> "REC-OTP";
            case HOME_SALUDO -> "HOME-SALUDO";
            case HOME_ULTIMO_INGRESO -> "HOME-INGRESO";
            case HOME_ACCESO_BREB -> "HOME-BREB";
            case HOME_ACCESO_CREDITO -> "HOME-CREDITO";
            case HOME_MENU_INFERIOR -> "HOME-MENU";
            case HOME_SKELETON -> "HOME-SKELETON";
            case HOME_BANNER -> "HOME-BANNER";
            case HOME_PRODUCTO_AHORRO -> "PROD-AHORRO";
            case HOME_PRODUCTO_CORRIENTE -> "PROD-CORRIENTE";
            case HOME_PRODUCTO_PRESTAMO -> "PROD-PRESTAMO";
            case HOME_PRODUCTO_ROTATIVO -> "PROD-ROTATIVO";
            case HOME_PRODUCTO_PRE_APROBADO -> "PROD-PREAPROBADO";
            case HOME_SALDOS -> "PROD-SALDOS";
            case CUENTAS_PRODUCTO_AHORRO -> "CTA-AHORRO";
            case CUENTAS_PRODUCTO_CORRIENTE -> "CTA-CORRIENTE";
            case CUENTAS_PRODUCTO_PRESTAMO -> "CTA-PRESTAMO";
            case CUENTAS_PRODUCTO_ROTATIVO -> "CTA-ROTATIVO";
            case CUENTAS_SALDOS -> "CTA-SALDOS";
            case CUENTAS_DETALLE -> "CTA-DETALLE";
            case CUENTAS_DETALLE_TRANSACCION -> "CTA-TRX";
            case CUENTAS_DESCARGAR_COMPROBANTE -> "CTA-DESCARGA";
            case CUENTAS_COMPARTIR_COMPROBANTE -> "CTA-COMPARTIR";
            case CUENTAS_MOVIMIENTOS_MAXIMO_20 -> "CTA-MOV20";
            case CUENTAS_MOVIMIENTOS_SIN_VER_MAS -> "CTA-VERMAS";
            case CUENTAS_MOVIMIENTOS_SIN_PAGINACION -> "CTA-PAGINACION";
            case CUENTAS_MOVIMIENTOS_ULTIMO_MES -> "CTA-MES";
        };
    }

    public static Optional<CasoPrueba> desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String id = texto.trim().split("\\s*-\\s*", 2)[0].trim();
        return Arrays.stream(values()).filter(c -> c.idAzure.equals(id)).findFirst();
    }
}
