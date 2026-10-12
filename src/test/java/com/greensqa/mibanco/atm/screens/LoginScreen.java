package com.greensqa.mibanco.atm.screens;

import com.greensqa.mibanco.atm.data.TipoDocumento;

import java.time.Duration;

/** Formulario "Ingresa tus datos para continuar" (tipo y numero de documento y clave). */
public interface LoginScreen {

    void esperarVisible();

    boolean estaVisible(Duration espera);

    boolean olvideMiClaveVisible(Duration espera);

    /** Enlace "¿Olvidaste tu contraseña?": abre la recuperacion de clave. */
    void olvideMiClave();

    void seleccionarTipoDocumento(TipoDocumento tipo);

    String tipoDocumentoSeleccionado();

    void ingresarNumeroDocumento(String numero);

    void ingresarClave(String clave);

    void ingresar();

    boolean hayAviso();

    String textoAviso();
}
