package com.greensqa.mibanco.atm.screens;

import com.greensqa.mibanco.atm.data.TipoDocumento;

/** Formulario "Ingresa tus datos para continuar" (tipo y numero de documento y clave). */
public interface LoginScreen {

    void esperarVisible();

    void seleccionarTipoDocumento(TipoDocumento tipo);

    String tipoDocumentoSeleccionado();

    void ingresarNumeroDocumento(String numero);

    void ingresarClave(String clave);

    void ingresar();

    boolean hayAviso();

    String textoAviso();
}
