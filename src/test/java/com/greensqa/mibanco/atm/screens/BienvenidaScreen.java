package com.greensqa.mibanco.atm.screens;

import java.time.Duration;

/** Pantalla inicial "Te damos la bienvenida". */
public interface BienvenidaScreen {

    boolean estaVisible(Duration espera);

    void iniciarRegistro();
}
