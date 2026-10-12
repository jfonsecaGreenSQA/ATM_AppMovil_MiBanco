package com.greensqa.mibanco.atm.steps;

import com.greensqa.mibanco.atm.business.InicioAppBusiness;
import com.greensqa.mibanco.atm.business.NotificacionSmsBusiness;
import com.greensqa.mibanco.atm.screens.BandejaSmsScreen.Sms;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pasos de los escenarios de SMS. El Given marca la bandeja del dispositivo antes del flujo (y falla temprano si
 * no se puede leer, sin gastar un cliente); el SMS esperado es el que llega despues de esa marca.
 */
public class NotificacionSteps {

    private final InicioAppBusiness inicio = new InicioAppBusiness();
    private final NotificacionSmsBusiness sms = new NotificacionSmsBusiness();

    @Given("que un cliente activo no registrado con la línea del dispositivo está en la bienvenida de la app")
    public void clienteNoRegistradoConLineaEnBienvenida() {
        sms.marcarInicio();
        assertThat(inicio.irABienvenida()).as("La app muestra la pantalla de bienvenida").isTrue();
    }

    @And("recibe en su celular el SMS de registro exitoso")
    public void recibeSmsDeRegistroExitoso() {
        validarRecibido(sms.esperarSmsDeRegistro(), "registro exitoso");
    }

    private void validarRecibido(Optional<Sms> recibido, String evento) {
        assertThat(recibido)
                .as("Llega al celular un SMS nuevo de " + evento + " (ver adjunto 'SMS recibidos durante la iteracion')")
                .isPresent();
    }
}
