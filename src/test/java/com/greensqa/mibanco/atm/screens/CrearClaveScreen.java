package com.greensqa.mibanco.atm.screens;

import java.time.Duration;

/** Pantalla "Crea tu clave de 4 numeros" con aceptacion de terminos. */
public interface CrearClaveScreen {

    boolean estaVisible(Duration espera);

    void ingresarClave(String clave);

    void confirmarClave(String clave);

    void aceptarReglamentoDeUso();

    void aceptarTratamientoDeDatos();

    void continuar();
}
