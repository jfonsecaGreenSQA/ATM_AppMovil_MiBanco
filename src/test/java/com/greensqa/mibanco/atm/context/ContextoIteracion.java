package com.greensqa.mibanco.atm.context;

import com.greensqa.mibanco.atm.control.Iteracion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado compartido entre el orquestador y Cucumber durante una iteracion.
 * Las iteraciones se ejecutan en secuencia, por eso basta un estado estatico.
 */
public final class ContextoIteracion {

    private static Iteracion actual;
    private static String pasoActual = "";
    private static String pasoFallido;
    private static Boolean exitosa;
    private static String error;
    private static String indisponibilidad;
    private static final Map<String, String> INFO_EJECUCION = new LinkedHashMap<>();

    private ContextoIteracion() {
    }

    public static void iniciar(Iteracion iteracion) {
        actual = iteracion;
        pasoActual = "";
        pasoFallido = null;
        exitosa = null;
        error = null;
        indisponibilidad = null;
    }

    /** Aviso de indisponibilidad del sistema detectado en la iteracion; null si no hubo. */
    public static String indisponibilidad() {
        return indisponibilidad;
    }

    public static void indisponibilidad(String aviso) {
        indisponibilidad = aviso;
    }

    public static Iteracion actual() {
        if (actual == null) {
            throw new IllegalStateException("No hay iteracion activa: ejecute las pruebas desde el Orquestador");
        }
        return actual;
    }

    public static String pasoActual() {
        return pasoActual;
    }

    public static void pasoActual(String paso) {
        pasoActual = paso;
    }

    /** Paso que fallo; si ninguno fallo, el ultimo paso iniciado. */
    public static String pasoFallido() {
        return pasoFallido == null ? pasoActual : pasoFallido;
    }

    public static void pasoFallido(String paso) {
        if (pasoFallido == null) {
            pasoFallido = paso;
        }
    }

    public static void registrarResultado(boolean ok, String mensajeError) {
        exitosa = ok;
        error = mensajeError;
    }

    /** null si Cucumber no ejecuto ningun escenario para la iteracion. */
    public static Boolean exitosa() {
        return exitosa;
    }

    public static String error() {
        return error;
    }

    /** Datos del dispositivo/app para el entorno del reporte; se llenan en la primera sesion. */
    public static Map<String, String> infoEjecucion() {
        return INFO_EJECUCION;
    }
}
