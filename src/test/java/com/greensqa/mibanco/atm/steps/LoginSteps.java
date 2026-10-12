package com.greensqa.mibanco.atm.steps;

import com.greensqa.mibanco.atm.business.InicioAppBusiness;
import com.greensqa.mibanco.atm.business.LoginBusiness;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.Iteracion;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginSteps {

    private final InicioAppBusiness inicio = new InicioAppBusiness();
    private final LoginBusiness login = new LoginBusiness();
    private final Iteracion iteracion = ContextoIteracion.actual();

    @Given("que un cliente registrado está en la bienvenida de la app")
    public void clienteRegistradoEnBienvenida() {
        assertThat(inicio.irABienvenida())
                .as("La app muestra la pantalla de bienvenida")
                .isTrue();
    }

    @When("inicia sesión con su tipo y número de documento y su clave")
    public void iniciaSesion() {
        String tipoSeleccionado = login.iniciarSesion(iteracion);
        assertThat(tipoSeleccionado)
                .as("La app acepta el tipo de documento " + iteracion.tipoDocumento())
                .isEqualTo(iteracion.tipoDocumento().textoEnApp());
    }

    @Then("ingresa al Home sin errores")
    public void ingresaAlHomeSinErrores() {
        assertThat(login.homeVisible())
                .as("Se muestra el Home tras ingresar. Aviso de la app: '" + login.avisoTrasIngresar() + "'")
                .isTrue();
        assertThat(login.saludoHome()).as("Saludo del Home").startsWith("Hola,");
        assertThat(login.menuInferiorVisible()).as("Menu inferior del Home visible").isTrue();
        assertThat(login.avisoTrasIngresar()).as("Aviso de error en el Home").isEmpty();
    }
}
