package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.driver.DriverManager;
import com.greensqa.mibanco.atm.screens.BienvenidaScreen;
import com.greensqa.mibanco.atm.screens.Pantallas;
import io.qameta.allure.Allure;

import java.time.Duration;

/** Punto de partida comun de los flujos: la pantalla de bienvenida. */
public class InicioAppBusiness {

    private final BienvenidaScreen bienvenida = Pantallas.bienvenida();

    /** Regla del proyecto: si la app esta en primer plano se continua; si no, se abre. */
    public boolean irABienvenida() {
        Duration carga = Config.segundos("espera.carga.app.segundos");
        boolean enPrimerPlano = DriverManager.asegurarAppEnPrimerPlano();
        if (enPrimerPlano) {
            Allure.step("La app ya estaba en primer plano: se continua desde la pantalla actual");
        } else {
            Allure.step("La app no estaba en primer plano: se abrio");
        }
        if (bienvenida.estaVisible(enPrimerPlano ? Config.segundos("espera.pantalla.actual.segundos") : carga)) {
            return true;
        }
        Allure.step("La app no estaba en la bienvenida: se reinicia la app");
        DriverManager.reiniciarApp();
        return bienvenida.estaVisible(carga);
    }
}
