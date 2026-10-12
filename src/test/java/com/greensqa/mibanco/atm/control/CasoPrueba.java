package com.greensqa.mibanco.atm.control;

import com.greensqa.mibanco.atm.data.TipoDocumento;

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
    HOME_MENU_INFERIOR("38711", "Menu inferior del Home", "Home", TipoDocumento.CC, false);

    private final String idAzure;
    private final String nombre;
    private final String proceso;
    private final TipoDocumento tipoDocumento;
    private final boolean requiereLineaSms;

    CasoPrueba(String idAzure, String nombre, String proceso, TipoDocumento tipoDocumento, boolean requiereLineaSms) {
        this.idAzure = idAzure;
        this.nombre = nombre;
        this.proceso = proceso;
        this.tipoDocumento = tipoDocumento;
        this.requiereLineaSms = requiereLineaSms;
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

    public String tag() {
        return "@" + idAzure;
    }

    /** Texto con el que aparece en la lista desplegable de la columna Caso. */
    public String textoCatalogo() {
        return idAzure + " - " + nombre;
    }

    /** Reconoce el caso por el Id de Azure al inicio del texto ("38498 - ..."). */
    public static Optional<CasoPrueba> desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String id = texto.trim().split("\\s*-\\s*", 2)[0].trim();
        return Arrays.stream(values()).filter(c -> c.idAzure.equals(id)).findFirst();
    }
}
