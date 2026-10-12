package com.greensqa.mibanco.atm.steps;

import com.greensqa.mibanco.atm.business.InicioAppBusiness;
import com.greensqa.mibanco.atm.business.RegistroBusiness;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.Iteracion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class RegistroSteps {

    private final InicioAppBusiness inicio = new InicioAppBusiness();
    private final RegistroBusiness registro = new RegistroBusiness();
    private final Iteracion iteracion = ContextoIteracion.actual();

    @Given("que un cliente activo no registrado está en la bienvenida de la app")
    public void clienteEnBienvenida() {
        assertThat(inicio.irABienvenida())
                .as("La app muestra la pantalla de bienvenida")
                .isTrue();
    }

    @When("inicia su registro con su tipo y número de documento")
    public void iniciaRegistroConDocumento() {
        String tipoSeleccionado = registro.iniciarRegistroConDocumento(iteracion);
        assertThat(tipoSeleccionado)
                .as("La app acepta el tipo de documento " + iteracion.tipoDocumento())
                .isEqualTo(iteracion.tipoDocumento().textoEnApp());
    }

    @And("valida su identidad con el código OTP")
    public void validaIdentidadConOtp() {
        assertThat(registro.validarIdentidad(iteracion.otp()))
                .as("Tras validar el OTP se muestra la creacion de clave")
                .isTrue();
    }

    @And("crea una clave válida aceptando los términos y condiciones")
    public void creaClaveAceptandoTerminos() {
        assertThat(registro.crearClaveAceptandoTerminos(iteracion.clave()))
                .as("La clave es aceptada y se muestra la confirmacion del registro")
                .isTrue();
    }

    @Then("ve la confirmación de registro exitoso")
    public void veConfirmacionRegistroExitoso() {
        assertThat(registro.tituloConfirmacion()).as("Titulo de la confirmacion").isEqualTo("¡Registro exitoso!");
        assertThat(registro.opcionIngresarVisible()).as("Opcion Ingresar visible").isTrue();
        registro.guardarClienteRegistrado(iteracion);
    }
}
