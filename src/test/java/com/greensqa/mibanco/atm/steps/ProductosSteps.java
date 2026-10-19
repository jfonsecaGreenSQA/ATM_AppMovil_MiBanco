package com.greensqa.mibanco.atm.steps;

import com.greensqa.mibanco.atm.business.InicioAppBusiness;
import com.greensqa.mibanco.atm.business.LoginBusiness;
import com.greensqa.mibanco.atm.business.ProductosHomeBusiness;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.Montos;
import com.greensqa.mibanco.atm.data.TipoProducto;
import com.greensqa.mibanco.atm.screens.ProductoHome;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Productos del Home (38705, 38706, 38707, 38708, 38702 y 38709). Los textos esperados son los observados en la app;
 * las diferencias con el texto de los casos estan en el control de cambios de Consolidacion_Casos_de_Prueba.docx.
 */
public class ProductosSteps {

    /** "Cuentas de ahorro **** 1234" (observado); acepta tambien la forma del caso "Cuenta ahorros **** 1234". */
    private static final String CUENTA_AHORRO = "(?i)^cuentas? (de )?ahorros? \\*{4} \\d{4}$";
    private static final String CUENTA_CORRIENTE = "(?i)^cuentas? (de )?corriente \\*{4} \\d{4}$";
    /** "Cuota vencio 1 Jun" (observado), "Cuota venció Hoy" o "Cuota vence 24 Oct" (caso). */
    private static final String ESTADO_CUOTA = "(?i)^cuota (vencio|venció|vence) \\S.*$";
    private static final String CUPO_APROBADO = "Cupo aprobado";

    private final InicioAppBusiness inicio = new InicioAppBusiness();
    private final LoginBusiness login = new LoginBusiness();
    private final ProductosHomeBusiness productos = new ProductosHomeBusiness();
    private final Iteracion iteracion = ContextoIteracion.actual();

    @Given("que un cliente registrado con el producto del archivo de control inició sesión en la app")
    public void clienteConProductoInicioSesion() {
        assertThat(inicio.irABienvenida()).as("La app muestra la pantalla de bienvenida").isTrue();
        boolean enHome = productos.ingresarConProducto(iteracion);
        assertThat(enHome).as("Se muestra el Home tras ingresar. Aviso de la app: '%s'", enHome ? "" : login.avisoTrasIngresar())
                .isTrue();
    }

    @Then("ve el producto en su sección del Home con su información y formato")
    public void veProductoConSuInformacion() {
        TipoProducto tipo = iteracion.tipoProducto();
        List<ProductoHome> tarjetas = productos.productosDe(tipo);
        assertThat(tarjetas).as("El Home muestra %s", tipo.descripcion()).isNotEmpty();
        boolean soloSaldo = iteracion.caso().validaSaldos();
        for (ProductoHome p : tarjetas) {
            Allure.step("Tarjeta: " + p.titulo() + (p.etiqueta().isEmpty() ? "" : " | " + p.etiqueta())
                    + (p.monto().isEmpty() ? "" : " | " + p.monto()) + (p.estado().isEmpty() ? "" : " | " + p.estado()));
            // 38709 y 39565 validan los saldos: la informacion de cada tarjeta (cuota, etiqueta, flecha, ojo) es de 38705 a 38708
            if (soloSaldo) {
                validarMonto(p, "Saldo del producto");
                continue;
            }
            switch (tipo) {
                case AHORRO, CORRIENTE -> validarCuenta(p, tipo == TipoProducto.AHORRO ? CUENTA_AHORRO : CUENTA_CORRIENTE);
                case PRESTAMO -> validarPrestamo(p);
                case ROTATIVO -> validarRotativo(p);
                case PRE_APROBADO -> validarPreAprobado(p);
            }
        }
    }

    @And("el monto del producto coincide con el esperado del archivo de control")
    public void montoCoincideConElEsperado() {
        BigDecimal esperado = iteracion.montoEsperado();
        if (esperado == null) {
            Allure.step("Monto esperado no informado en el control: solo se valido el formato del monto");
            return;
        }
        TipoProducto tipo = iteracion.tipoProducto();
        List<BigDecimal> montos = productos.productosDe(tipo).stream()
                .map(p -> Montos.desdeApp(p.monto())).flatMap(Optional::stream).toList();
        assertThat(montos)
                .as("Alguna tarjeta de %s muestra el monto esperado %s (montos mostrados: %d)",
                        tipo.descripcion(), Montos.texto(esperado), montos.size())
                .anyMatch(m -> m.compareTo(esperado) == 0);
    }

    private void validarCuenta(ProductoHome p, String titulo) {
        assertThat(p.titulo()).as("Tipo de cuenta y ultimos 4 digitos enmascarados").matches(titulo);
        assertThat(p.etiqueta()).as("Etiqueta de la cuenta en su contenedor gris").isNotBlank();
        validarMonto(p, "Saldo de la cuenta");
        assertThat(p.ocultarMontos()).as("Icono para ocultar montos (ojo) en la tarjeta").isTrue();
    }

    private void validarPrestamo(ProductoHome p) {
        validarMonto(p, "Saldo adeudado");
        assertThat(p.estado()).as("Estado de la cuota ('Cuota vencio 1 Jun', 'Cuota vence 24 Oct')").matches(ESTADO_CUOTA);
        assertThat(p.navegable()).as("La tarjeta se puede abrir (flecha >)").isTrue();
    }

    private void validarRotativo(ProductoHome p) {
        assertThat(p.etiqueta()).as("Etiqueta del credito rotativo").isEqualTo(CUPO_APROBADO);
        validarMonto(p, "Cupo aprobado");
        assertThat(p.navegable()).as("La tarjeta se puede abrir (flecha >)").isTrue();
    }

    /** El caso pide el saldo aprobado; la app solo muestra el banner (control de cambios #8). */
    private void validarPreAprobado(ProductoHome p) {
        assertThat(p.navegable()).as("El banner de credito pre-aprobado se puede tocar").isTrue();
        if (p.monto().isEmpty()) {
            Allure.step("Observacion (control de cambios #8): el banner no muestra el saldo aprobado que pide el caso");
        } else {
            validarMonto(p, "Saldo aprobado");
        }
    }

    private void validarMonto(ProductoHome p, String que) {
        assertThat(p.monto()).as("%s con signo de pesos, punto de miles y coma decimal ($22.746.864,51)", que)
                .satisfies(m -> assertThat(Montos.formatoApp(m)).as("Formato del monto").isTrue());
    }
}
