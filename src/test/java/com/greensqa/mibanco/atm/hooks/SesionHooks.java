package com.greensqa.mibanco.atm.hooks;

import com.greensqa.mibanco.atm.business.LoginBusiness;
import com.greensqa.mibanco.atm.business.ProductosHomeBusiness;
import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.driver.DriverManager;
import com.greensqa.mibanco.atm.screens.Pantallas;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

/**
 * Cierra la sesion al terminar los escenarios que ingresan a la app: si queda abierta, el siguiente ingreso del mismo
 * cliente responde "Sesion activa" hasta que venza en el servidor. Corre antes del cierre del driver (order mayor).
 * Un error aqui no cambia el resultado de la prueba.
 */
public class SesionHooks {

    @After(value = "@login or @home or @cuentas", order = 20000)
    public void cerrarSesionAbierta(Scenario escenario) {
        if (!DriverManager.activo()) {
            return;
        }
        try {
            // Desde el detalle de una transaccion o de la cuenta se vuelve al Home con la flecha de la app: Atras del
            // sistema en el Home abre "¿Quieres cerrar sesion?"
            if (Pantallas.menuCompartir().estaVisible()) {
                Pantallas.menuCompartir().cancelar();
            }
            if (Pantallas.detalleTransaccion().estaVisible()) {
                Pantallas.detalleTransaccion().volver();
                Pantallas.detalleCuenta().estaVisible(Config.segundos("espera.segundos"));
            }
            if (Pantallas.detalleCuenta().estaVisible()) {
                Pantallas.detalleCuenta().volver();
                Pantallas.home().estaVisible(Config.segundos("espera.segundos"));
            }
            if (Pantallas.home().estaVisible()) {
                inventariarProductos(escenario);
                boolean cerrada = new LoginBusiness().cerrarSesion();
                System.out.println("[SESION] " + (cerrada ? "Sesion cerrada" : "Se pidio cerrar sesion pero no aparecio la bienvenida"));
            }
        } catch (RuntimeException e) {
            System.err.println("[SESION] No se pudo cerrar la sesion: " + e.getMessage());
        }
    }

    /**
     * Registra en el listado (PRODUCTOS_HOME) los productos que muestra el Home del cliente, para que los casos de
     * productos elijan clientes que los tengan. Los escenarios de productos y de cuentas ya lo hicieron.
     */
    private void inventariarProductos(Scenario escenario) {
        Iteracion it = ContextoIteracion.actual();
        if (escenario.getSourceTagNames().contains("@productos") || escenario.getSourceTagNames().contains("@cuentas")
                || it.numDocumento() == null) {
            return;
        }
        try {
            new ProductosHomeBusiness().inventariar(it);
        } catch (RuntimeException e) {
            System.err.println("[PRODUCTOS] No se pudo inventariar el Home: " + e.getMessage());
        }
    }
}
