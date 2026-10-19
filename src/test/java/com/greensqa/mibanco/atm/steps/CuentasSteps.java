package com.greensqa.mibanco.atm.steps;

import com.greensqa.mibanco.atm.business.CuentasBusiness;
import com.greensqa.mibanco.atm.business.InicioAppBusiness;
import com.greensqa.mibanco.atm.business.LoginBusiness;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.FechasApp;
import com.greensqa.mibanco.atm.data.Montos;
import com.greensqa.mibanco.atm.data.Movimiento;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.DetalleCuentaScreen;
import com.greensqa.mibanco.atm.screens.DetalleTransaccionScreen;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Detalle de cuenta (39566). Los textos esperados son los observados en la app el 15/10/2026; las diferencias con el
 * texto del caso estan en el control de cambios de Consolidacion_Casos_de_Prueba.docx.
 */
public class CuentasSteps {

    /** "Cuentas de ahorro" (observado); acepta tambien la forma del caso "Cuenta de ahorros". */
    private static final String TIPO_AHORRO = "(?i)^cuentas? (de )?ahorros?$";
    /** Numero completo, sin enmascarar: digitos con guiones ("350-003574-524"). */
    private static final String NUMERO_COMPLETO = "^\\d[\\d-]{7,}\\d$";
    /** El caso 39567 limita la lista a las ultimas 20 transacciones. */
    private static final int MAXIMO_MOVIMIENTOS = 20;
    /** Controles de carga adicional que 39569 y 39570 piden que no existan: "Ver mas", "Cargar mas", paginas. */
    private static final String OPCION_PAGINACION = "(?i)^(ver|cargar|mostrar) m[aá]s.*|^p[aá]gina.*|^siguiente$|^anterior$|^\\d{1,2}$";

    private final InicioAppBusiness inicio = new InicioAppBusiness();
    private final LoginBusiness login = new LoginBusiness();
    private final CuentasBusiness cuentas = new CuentasBusiness();
    private final Iteracion iteracion = ContextoIteracion.actual();
    private DetalleCuentaScreen.Encabezado encabezado;
    private Movimiento movimiento;
    private DetalleTransaccionScreen.Datos transaccion;
    private CuentasBusiness.ComprobanteDescargado descarga;
    private DetalleCuentaScreen.Movimientos listaMovimientos;
    private List<Movimiento> leidos = List.of();
    private CuentasBusiness.MenuCompartir compartir;

    @Given("que un cliente registrado con cuenta de ahorros inició sesión en la app")
    public void clienteConCuentaDeAhorrosInicioSesion() {
        assertThat(inicio.irABienvenida()).as("La app muestra la pantalla de bienvenida").isTrue();
        boolean enHome = cuentas.ingresarConCuenta(iteracion);
        assertThat(enHome).as("Se muestra el Home tras ingresar. Aviso de la app: '%s'", enHome ? "" : login.avisoTrasIngresar())
                .isTrue();
    }

    @When("abre el detalle de su cuenta de ahorros desde \"Tus cuentas\"")
    public void abreDetalleDeCuenta() {
        assertThat(cuentas.abrirDetalle(iteracion)).as("Se muestra el detalle de la cuenta").isTrue();
        encabezado = cuentas.encabezado();
    }

    @Then("ve el tipo de cuenta y su número completo")
    public void veTipoYNumeroDeCuenta() {
        Allure.step("Cuenta: " + encabezado.tipo() + " " + enmascarar(encabezado.numero()));
        assertThat(encabezado.tipo()).as("Tipo de cuenta").matches(TIPO_AHORRO);
        assertThat(encabezado.numero()).as("Numero de cuenta completo, sin enmascarar").matches(NUMERO_COMPLETO);
    }

    @And("ve la opción para compartir los datos de la cuenta")
    public void veOpcionCompartir() {
        assertThat(encabezado.compartir()).as("Boton para compartir los datos de la cuenta").isTrue();
    }

    @And("ve el saldo disponible y el saldo en canje con formato de moneda")
    public void veSaldos() {
        Allure.step("Saldo disponible: " + encabezado.saldoDisponible() + " | Saldo en canje: " + encabezado.saldoEnCanje());
        assertThat(Montos.formatoApp(encabezado.saldoDisponible()))
                .as("Saldo disponible con formato $22.746.864,51: '%s'", encabezado.saldoDisponible()).isTrue();
        assertThat(Montos.formatoApp(encabezado.saldoEnCanje()))
                .as("Saldo en canje con formato $22.746.864,51: '%s'", encabezado.saldoEnCanje()).isTrue();
    }

    @And("ve las acciones rápidas de la cuenta")
    public void veAccionesRapidas() {
        Allure.step("Acciones rapidas: " + encabezado.acciones());
        assertThat(encabezado.acciones()).as("Acciones rapidas de la cuenta").isNotEmpty();
    }

    /**
     * Acepta la lista de movimientos o el aviso de que no hay (opcion aprobada el 15/10/2026): ningun cliente de prueba
     * tiene movimientos, por lo que el formato de cada registro se valida cuando haya datos (39567 a 39571).
     */
    @And("ve la sección de movimientos")
    public void veSeccionDeMovimientos() {
        DetalleCuentaScreen.Movimientos movimientos = cuentas.movimientos();
        assertThat(movimientos.aviso())
                .as("La seccion Movimientos muestra el aviso de las ultimas 20 transacciones o el de que no hay movimientos."
                        + " Textos leidos: %s", movimientos.textos())
                .isNotEmpty();
        Allure.step(movimientos.sinMovimientos()
                ? "Observacion: el cliente no tiene movimientos (\"" + movimientos.aviso() + "\"); la lista de"
                        + " transacciones del caso no se pudo validar con este cliente"
                : "Movimientos: \"" + movimientos.aviso() + "\" (" + movimientos.textos().size() + " textos leidos)");
    }

    @And("recorre la lista de movimientos hasta el final")
    public void recorreListaDeMovimientos() {
        listaMovimientos = cuentas.movimientos();
        if (listaMovimientos.sinMovimientos()) {
            throw new IllegalStateException("Dato no apto: la cuenta no tiene movimientos (\"" + listaMovimientos.aviso()
                    + "\"). Informe en el control un cliente con movimientos");
        }
        leidos = cuentas.movimientosLeidos();
        Evidencias.texto("Movimientos leidos", String.join("\n", listaMovimientos.registros()));
        Allure.step("Movimientos en la lista: " + leidos.size() + (leidos.isEmpty() ? ""
                : " (del " + leidos.get(0).fecha() + " al " + leidos.get(leidos.size() - 1).fecha() + ")"));
        assertThat(cuentas.registrosSinFormato()).as("Cada movimiento muestra descripcion, fecha y hora y monto con formato")
                .isEmpty();
        assertThat(leidos).as("La lista muestra movimientos").isNotEmpty();
    }

    @Then("ve el aviso de las últimas 20 transacciones")
    public void veAvisoUltimas20() {
        assertThat(listaMovimientos.aviso()).as("Aviso sobre la lista de movimientos")
                .startsWith("Los movimientos").contains("últimas 20 transacciones");
    }

    @And("la lista tiene como máximo 20 movimientos ordenados del más reciente al más antiguo")
    public void listaConMaximo20Ordenada() {
        assertThat(leidos).as("Movimientos en la lista (maximo 20)").hasSizeLessThanOrEqualTo(MAXIMO_MOVIMIENTOS);
        for (int i = 1; i < leidos.size(); i++) {
            assertThat(leidos.get(i).fecha()).as("Movimiento %d (%s) no es posterior al %d", i + 1, leidos.get(i).descripcion(), i)
                    .isBeforeOrEqualTo(leidos.get(i - 1).fecha());
        }
        if (leidos.size() < MAXIMO_MOVIMIENTOS) {
            Allure.step("Observacion: el cliente tiene " + leidos.size() + " movimientos; para comprobar el corte en 20 se"
                    + " necesita un cliente con mas de 20");
        }
    }

    @And("al final de la lista no hay opción de ver más ni paginación y no se cargan más movimientos")
    public void sinVerMasNiPaginacion() {
        assertThat(listaMovimientos.textos()).as("Opciones de cargar mas movimientos o paginacion al final de la lista")
                .noneMatch(t -> t.matches(OPCION_PAGINACION));
        assertThat(cuentas.movimientosNuevosAlForzarElFinal()).as("Movimientos que aparecen al forzar el desplazamiento al final")
                .isEmpty();
    }

    /** "Hoy - 1 mes" segun el caso 39568; se compara contra el inicio de ese dia. */
    @Then("todos los movimientos tienen fecha dentro del último mes")
    public void movimientosDelUltimoMes() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime limite = hoy.minusMonths(1).atStartOfDay();
        Allure.step("Rango: desde " + limite.toLocalDate() + " hasta " + hoy);
        for (Movimiento m : leidos) {
            assertThat(m.fecha()).as("Fecha de %s", m.descripcion()).isAfterOrEqualTo(limite)
                    .isBeforeOrEqualTo(hoy.plusDays(1).atStartOfDay());
        }
        Allure.step("Observacion: para comprobar que se excluyen los movimientos anteriores se necesita un cliente con"
                + " movimientos de mas de un mes (el mas antiguo de la lista es del " + leidos.get(leidos.size() - 1).fecha() + ")");
    }

    @And("abre el detalle de uno de sus movimientos")
    public void abreDetalleDeUnMovimiento() {
        movimiento = cuentas.abrirPrimerMovimiento();
        assertThat(cuentas.detalleTransaccionVisible()).as("Se muestra el detalle de la transaccion").isTrue();
        transaccion = cuentas.datosTransaccion();
        Allure.step("Detalle de la transaccion: " + transaccion.etiqueta() + " | " + transaccion.monto() + " | "
                + transaccion.fecha() + " | Comprobante N° " + transaccion.comprobante() + " | " + transaccion.descripcion());
    }

    @Then("ve el monto, la fecha y la hora del movimiento")
    public void veMontoFechaYHoraDelMovimiento() {
        assertThat(transaccion.etiqueta()).as("Etiqueta del monto (p. ej. MONTO ENVIADO)").isNotBlank();
        assertThat(Montos.formatoAppConSigno(transaccion.monto()))
                .as("Monto con formato $22.746.864,51 (con signo si es un debito): '%s'", transaccion.monto()).isTrue();
        assertThat(Montos.desdeAppConSigno(transaccion.monto())).as("Monto igual al de la lista de movimientos")
                .hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo(movimiento.valor()));
        assertThat(FechasApp.desdeApp(transaccion.fecha())).as("Fecha y hora con el formato de la app: '%s'", transaccion.fecha())
                .contains(movimiento.fecha());
    }

    /** La app no muestra la cuenta de origen ni la de destino que pide 39571 (control de cambios): queda como observacion. */
    @And("ve el número de comprobante y la descripción del movimiento")
    public void veComprobanteYDescripcion() {
        assertThat(transaccion.comprobante()).as("Numero de comprobante").isNotBlank();
        if (transaccion.comprobante().matches("^0+$")) {
            Allure.step("Observacion: el numero de comprobante se muestra en ceros (" + transaccion.comprobante() + ")");
        }
        assertThat(normalizar(transaccion.descripcion())).as("Descripcion igual a la de la lista de movimientos")
                .isEqualTo(normalizar(movimiento.descripcion()));
        Allure.step("Observacion: el detalle no muestra cuenta de origen ni de destino (las pide el caso 39571)");
    }

    @And("descarga el comprobante")
    public void descargaComprobante() {
        descarga = cuentas.descargarComprobante(transaccion);
    }

    @Then("ve el aviso de comprobante descargado")
    public void veAvisoDeComprobanteDescargado() {
        assertThat(descarga.aviso()).as("Aviso de la app tras descargar el comprobante").startsWith("Tu comprobante se descarg");
    }

    @And("la imagen del comprobante queda guardada en el dispositivo")
    public void imagenDelComprobanteGuardada() {
        Allure.step("Imagen " + descarga.ruta() + ": guardada " + descarga.guardado() + ", " + descarga.bytes()
                + " bytes adjuntos a la evidencia, borrada del dispositivo " + descarga.borrado());
        assertThat(descarga.guardado()).as("La imagen del comprobante se guarda en %s", descarga.ruta()).isTrue();
        assertThat(descarga.bytes()).as("Tamano de la imagen del comprobante").isPositive();
        if (!descarga.borrado()) {
            Allure.step("Observacion: no se pudo borrar " + descarga.ruta() + " del dispositivo; borrarla a mano");
        }
    }

    @And("comparte el comprobante")
    public void comparteComprobante() {
        compartir = cuentas.compartirComprobante();
    }

    @Then("se abre el menú de compartir del dispositivo con la imagen del comprobante")
    public void seAbreMenuDeCompartir() {
        Allure.step("Menu de compartir: '" + compartir.encabezado() + "' | vista previa: '" + compartir.vistaPrevia() + "'");
        assertThat(compartir.visible()).as("Se abre el menu de compartir del dispositivo").isTrue();
        assertThat(compartir.encabezado()).as("El menu comparte una imagen").containsIgnoringCase("imagen");
        assertThat(compartir.vistaPrevia()).as("Vista previa del comprobante").startsWithIgnoringCase("Comprobante");
        assertThat(compartir.cerrado()).as("Al cancelar sin elegir app vuelve al detalle de la transaccion").isTrue();
    }

    private static String normalizar(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }

    private static String enmascarar(String numero) {
        return numero.length() < 8 ? numero : numero.substring(0, 4) + "****" + numero.substring(numero.length() - 3);
    }
}
