package com.greensqa.mibanco.atm.screens;

import java.time.Duration;

/**
 * Pantalla "Crea tu clave de 4 numeros". En el registro pide aceptar los terminos; en la recuperacion de clave es el
 * mismo formulario sin las casillas.
 */
public interface CrearClaveScreen {

    boolean estaVisible(Duration espera);

    void ingresarClave(String clave);

    void confirmarClave(String clave);

    void aceptarReglamentoDeUso();

    void aceptarTratamientoDeDatos();

    void continuar();
}
