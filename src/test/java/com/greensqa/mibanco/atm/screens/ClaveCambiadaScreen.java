package com.greensqa.mibanco.atm.screens;

/** Confirmacion final de la recuperacion de clave ("Clave cambiada con exito"). */
public interface ClaveCambiadaScreen {

    /** Verificacion inmediata, sin espera. */
    boolean estaVisible();

    String titulo();

    /** Opcion "Ingresar": lleva al formulario de login. */
    void ingresar();
}
