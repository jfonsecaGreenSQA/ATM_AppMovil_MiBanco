package com.greensqa.mibanco.atm.screens;

import com.greensqa.mibanco.atm.data.TipoDocumento;

/** Formulario "Ahora, tu numero de documento" y su aviso modal. */
public interface RegistroDocumentoScreen {

    void esperarVisible();

    void seleccionarTipoDocumento(TipoDocumento tipo);

    String tipoDocumentoSeleccionado();

    void ingresarNumeroDocumento(String numero);

    void continuar();

    boolean hayAviso();

    /** El aviso indica que el documento ya esta registrado ("Ya eres cliente del banco"). */
    boolean avisoIndicaDocumentoRegistrado();

    String textoAviso();

    void cerrarAviso();
}
