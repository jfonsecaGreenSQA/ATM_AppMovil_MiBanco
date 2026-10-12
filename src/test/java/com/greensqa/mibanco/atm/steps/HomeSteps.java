package com.greensqa.mibanco.atm.steps;

import com.greensqa.mibanco.atm.business.HomeBusiness;
import com.greensqa.mibanco.atm.business.InicioAppBusiness;
import com.greensqa.mibanco.atm.business.LoginBusiness;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.Iteracion;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class HomeSteps {

    private final InicioAppBusiness inicio = new InicioAppBusiness();
    private final LoginBusiness login = new LoginBusiness();
    private final HomeBusiness home = new HomeBusiness();
    private final Iteracion iteracion = ContextoIteracion.actual();

    @Given("que un cliente registrado inició sesión en la app")
    public void clienteInicioSesion() {
        assertThat(inicio.irABienvenida()).as("La app muestra la pantalla de bienvenida").isTrue();
        login.iniciarSesion(iteracion);
        assertThat(login.homeVisible())
                .as("Se muestra el Home tras ingresar. Aviso de la app: '" + login.avisoTrasIngresar() + "'")
                .isTrue();
    }

    @Given("que un cliente registrado con ingresos previos inició sesión en la app")
    public void clienteConIngresosPreviosInicioSesion() {
        clienteInicioSesion();
        assertThat(home.asegurarIngresoPrevio(iteracion))
                .as("El cliente tiene un ingreso previo (se muestra 'Último ingreso')")
                .isTrue();
    }

    @Then("ve el saludo de bienvenida")
    public void veSaludoDeBienvenida() {
        assertThat(home.saludoVisible()).as("Se muestra el saludo 'Hola, ...' con el nombre del cliente").isTrue();
    }

    @Then("ve el acceso {string} en la parte superior del Home")
    public void veAccesoSuperior(String acceso) {
        assertThat(home.accesoSuperiorVisible(acceso))
                .as("El acceso '" + acceso + "' se muestra en la parte superior del Home")
                .isTrue();
    }

    @Then("ve el menú inferior con los accesos {string} en ese orden")
    public void veMenuInferiorEnOrden(String accesos) {
        List<String> esperados = Arrays.stream(accesos.split(",")).map(String::trim).toList();
        assertThat(home.accesosMenuInferior())
                .as("Accesos del menu inferior de izquierda a derecha")
                .containsExactlyElementsOf(esperados);
    }

    @And("ve la pregunta {string}")
    public void veLaPregunta(String pregunta) {
        assertThat(home.preguntaActividad()).as("Pregunta de actividad del Home").isEqualTo(pregunta);
    }

    @And("ve la fecha y hora de su último ingreso")
    public void veFechaYHoraDeUltimoIngreso() {
        assertThat(home.ultimoIngreso()).as("Etiqueta 'Último ingreso' visible").isPresent();
        Optional<LocalDateTime> fecha = home.fechaUltimoIngreso();
        assertThat(fecha)
                .as("'%s' cumple el formato 'Último ingreso: DD/MM/AAAA - HH:MM:SS' con una fecha valida",
                        home.ultimoIngreso().orElse(""))
                .isPresent();
        assertThat(fecha.get()).as("El ultimo ingreso no es una fecha futura").isBefore(LocalDateTime.now().plusMinutes(5));
    }
}
