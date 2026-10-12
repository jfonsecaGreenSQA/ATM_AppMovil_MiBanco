package com.greensqa.mibanco.atm.steps;

import com.greensqa.mibanco.atm.business.RecuperacionClaveBusiness;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.Iteracion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** Recuperacion de clave. El paso inicial ("cliente registrado en la bienvenida") esta en LoginSteps. */
public class RecuperacionClaveSteps {

    private final RecuperacionClaveBusiness recuperacion = new RecuperacionClaveBusiness();
    private final Iteracion iteracion = ContextoIteracion.actual();

    @When("solicita recuperar su clave con su tipo y número de documento")
    public void solicitaRecuperarClave() {
        String tipoSeleccionado = recuperacion.solicitarRecuperacion(iteracion);
        assertThat(tipoSeleccionado)
                .as("La app acepta el tipo de documento " + iteracion.tipoDocumento())
                .isEqualTo(iteracion.tipoDocumento().textoEnApp());
        boolean pideClave = recuperacion.nuevaClaveSolicitada();
        assertThat(pideClave)
                .as("Tras el documento la app pide la clave nueva. Aviso de la app: '" + recuperacion.avisoTrasDocumento() + "'")
                .isTrue();
    }

    @And("crea una nueva clave válida")
    public void creaNuevaClave() {
        assertThat(recuperacion.crearNuevaClave(iteracion.clave()))
                .as("La clave nueva es aceptada y la app pide el OTP")
                .isTrue();
    }

    @And("confirma el cambio con el código OTP")
    public void confirmaConOtp() {
        boolean confirmado = recuperacion.confirmarConOtp(iteracion);
        assertThat(confirmado)
                .as("El OTP es validado y se muestra la confirmacion del cambio de clave. Error del OTP: '"
                        + recuperacion.errorOtp() + "'")
                .isTrue();
    }

    @Then("ve la confirmación de clave cambiada con éxito")
    public void veConfirmacionClaveCambiada() {
        assertThat(recuperacion.tituloConfirmacion()).as("Titulo de la confirmacion").isEqualTo("Clave cambiada con éxito");
    }

    @And("al continuar llega al inicio de sesión")
    public void llegaAlInicioDeSesion() {
        assertThat(recuperacion.continuarAlInicioDeSesion())
                .as("Tras Ingresar se muestra el formulario de login")
                .isTrue();
    }
}
