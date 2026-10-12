package com.greensqa.mibanco.atm.screens;

import com.greensqa.mibanco.atm.data.TipoDocumento;

import java.time.Duration;

/** Formulario "Ingresa los siguientes datos para recuperar tu clave" (tipo y numero de documento). */
public interface RecuperarClaveScreen {

    boolean estaVisible(Duration espera);

    void seleccionarTipoDocumento(TipoDocumento tipo);

    String tipoDocumentoSeleccionado();

    void ingresarNumeroDocumento(String numero);

    void continuar();

    /** Verificacion inmediata, sin espera. */
    boolean hayAviso();

    String textoAviso();
}
