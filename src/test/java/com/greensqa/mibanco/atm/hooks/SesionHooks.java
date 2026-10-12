package com.greensqa.mibanco.atm.hooks;

import com.greensqa.mibanco.atm.business.LoginBusiness;
import com.greensqa.mibanco.atm.driver.DriverManager;
import com.greensqa.mibanco.atm.screens.Pantallas;
import io.cucumber.java.After;

/**
 * Cierra la sesion al terminar los escenarios que ingresan a la app: si queda abierta, el siguiente ingreso del mismo
 * cliente responde "Sesion activa" hasta que venza en el servidor. Corre antes del cierre del driver (order mayor).
 * Un error aqui no cambia el resultado de la prueba.
 */
public class SesionHooks {

    @After(value = "@login or @home", order = 20000)
    public void cerrarSesionAbierta() {
        if (!DriverManager.activo()) {
            return;
        }
        try {
            if (Pantallas.home().estaVisible()) {
                boolean cerrada = new LoginBusiness().cerrarSesion();
                System.out.println("[SESION] " + (cerrada ? "Sesion cerrada" : "Se pidio cerrar sesion pero no aparecio la bienvenida"));
            }
        } catch (RuntimeException e) {
            System.err.println("[SESION] No se pudo cerrar la sesion: " + e.getMessage());
        }
    }
}
