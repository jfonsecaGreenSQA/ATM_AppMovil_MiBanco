package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.Enmascarador;
import com.greensqa.mibanco.atm.data.GeneradorClave;
import com.greensqa.mibanco.atm.data.PoolClientes;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.BienvenidaScreen;
import com.greensqa.mibanco.atm.screens.ClaveCambiadaScreen;
import com.greensqa.mibanco.atm.screens.CrearClaveScreen;
import com.greensqa.mibanco.atm.screens.DialogosSistemaScreen;
import com.greensqa.mibanco.atm.screens.LoginScreen;
import com.greensqa.mibanco.atm.screens.OtpScreen;
import com.greensqa.mibanco.atm.screens.Pantallas;
import com.greensqa.mibanco.atm.screens.RecuperarClaveScreen;
import io.qameta.allure.Allure;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * Tareas de negocio de la recuperacion de clave. Depende solo de contratos de pantalla.
 * Orden observado en la app: documento, clave nueva, OTP, confirmacion y login.
 */
public class RecuperacionClaveBusiness {

    private final InicioAppBusiness inicio = new InicioAppBusiness();
    private final BienvenidaScreen bienvenida = Pantallas.bienvenida();
    private final LoginScreen login = Pantallas.login();
    private final RecuperarClaveScreen recuperar = Pantallas.recuperarClave();
    private final CrearClaveScreen nuevaClave = Pantallas.crearClave();
    private final OtpScreen otp = Pantallas.otp();
    private final ClaveCambiadaScreen confirmacion = Pantallas.claveCambiada();
    private final DialogosSistemaScreen sistema = Pantallas.dialogosSistema();
    private final ReintentoIndisponibilidad reintento = new ReintentoIndisponibilidad();

    /**
     * Abre la recuperacion desde el login y continua con el documento del cliente.
     * Con NumDocumento AUTO toma un cliente REGISTRADO con clave del listado.
     *
     * @return tipo de documento que quedo seleccionado en la app
     */
    public String solicitarRecuperacion(Iteracion it) {
        resolverCliente(it);
        abrirRecuperacion();
        recuperar.seleccionarTipoDocumento(it.tipoDocumento());
        String tipoSeleccionado = recuperar.tipoDocumentoSeleccionado();
        recuperar.ingresarNumeroDocumento(it.numDocumento());
        Evidencias.captura("Documento ingresado");
        reintento.ejecutar("continuar con el documento", recuperar::continuar, () -> { },
                () -> nuevaClave.estaVisible(Duration.ofMillis(300)) || recuperar.hayAviso(), Config.segundos("espera.segundos"));
        return tipoSeleccionado;
    }

    /**
     * De la bienvenida a la pagina de recuperacion. A veces el WebView no expone parte de la pagina en el arbol de UI:
     * el enlace o el formulario se ven en pantalla pero UiAutomator2 no los encuentra (observado el 2026-10-12 en el S23
     * y en el emulador Android 17). Esperar, tocar o desplazar no lo recupera; volver a navegar si. Solo en ese caso se
     * reinicia la app y se repite la navegacion, una vez, con evidencia del primer intento.
     */
    private void abrirRecuperacion() {
        Duration esperaEnlace = Config.segundos("espera.elemento.webview.segundos");
        for (int intento = 1; ; intento++) {
            bienvenida.iniciarSesion();
            login.esperarVisible();
            if (login.olvideMiClaveVisible(esperaEnlace)) {
                login.olvideMiClave();
                if (recuperar.estaVisible(Config.segundos("espera.segundos"))) {
                    return;
                }
            }
            String falla = "La pantalla no expone el enlace '¿Olvidaste tu contraseña?' o la pagina de recuperacion"
                    + " en el arbol de UI (intento " + intento + ")";
            Evidencias.captura(falla);
            Evidencias.codigoFuente(falla);
            if (intento >= 2) {
                throw new AssertionError(falla + " tras volver a navegar desde la bienvenida");
            }
            Allure.step(falla + ": se reinicia la app y se repite la navegacion");
            if (!inicio.irABienvenida()) {
                throw new AssertionError("La app no volvio a la bienvenida tras reiniciarla");
            }
        }
    }

    /** @return true si tras el documento la app pide la clave nueva. */
    public boolean nuevaClaveSolicitada() {
        return nuevaClave.estaVisible(Config.segundos("espera.segundos"));
    }

    /** Aviso mostrado tras continuar con el documento, o vacio si no hay. */
    public String avisoTrasDocumento() {
        return recuperar.hayAviso() ? recuperar.textoAviso() : "";
    }

    /** @return true si la clave nueva es aceptada y la app pide el OTP. */
    public boolean crearNuevaClave(String clave) {
        Runnable diligenciar = () -> {
            nuevaClave.ingresarClave(clave);
            nuevaClave.confirmarClave(clave);
        };
        diligenciar.run();
        Evidencias.captura("Clave nueva diligenciada");
        // Si el aviso de indisponibilidad borra los campos, se vuelven a escribir (escribir limpia el campo antes)
        reintento.ejecutar("crear la clave nueva", nuevaClave::continuar, diligenciar,
                () -> otp.estaVisible() || sistema.descartarGuardarClaveSiAparece(), Config.segundos("espera.segundos"));
        return esperar(otp::estaVisible);
    }

    /**
     * Ingresa y valida el OTP. Desde que se toca Validar la clave del cliente puede cambiar: queda EN_USO en el listado
     * y, si la iteracion falla, los hooks lo dejan FALLIDO para revision manual. Con la confirmacion en pantalla la
     * clave nueva se guarda de inmediato.
     *
     * @return true si se muestra la confirmacion del cambio de clave
     */
    public boolean confirmarConOtp(Iteracion it) {
        otp.ingresarCodigo(it.otp());
        Evidencias.captura("OTP ingresado");
        PoolClientes.get().actualizar(it.tipoDocumento(), it.numDocumento(), PoolClientes.Estado.EN_USO, null,
                it.caso().idAzure(), "Cambio de clave en curso (recuperacion) - IdPrueba " + it.idPrueba());
        it.reservarClienteRegistrado(it.numDocumento());
        reintento.ejecutar("validar el OTP", otp::validar, () -> { },
                () -> confirmacion.estaVisible() || sistema.descartarGuardarClaveSiAparece(), Config.segundos("espera.segundos"));
        if (!esperar(confirmacion::estaVisible)) {
            return false;
        }
        PoolClientes.get().actualizar(it.tipoDocumento(), it.numDocumento(), PoolClientes.Estado.REGISTRADO, it.clave(),
                it.caso().idAzure(), "Clave cambiada por recuperacion - IdPrueba " + it.idPrueba());
        it.liberarDocumento();
        return true;
    }

    /** Error mostrado en la hoja del OTP, o vacio si no hay. */
    public String errorOtp() {
        return otp.estaVisible() ? otp.error() : "";
    }

    public String tituloConfirmacion() {
        return confirmacion.titulo();
    }

    /** @return true si al tocar Ingresar en la confirmacion la app muestra el formulario de login. */
    public boolean continuarAlInicioDeSesion() {
        confirmacion.ingresar();
        return login.estaVisible(Config.segundos("espera.segundos"));
    }

    /** Cliente de la iteracion y clave nueva: si no viene del control se genera distinta de la actual. */
    private void resolverCliente(Iteracion it) {
        String claveActual;
        if (it.documentoAutomatico()) {
            PoolClientes.ClienteRegistrado cliente = PoolClientes.get().registradoConClave(it.tipoDocumento())
                    .orElseThrow(() -> new IllegalStateException("Dato no apto: no hay clientes " + it.tipoDocumento()
                            + " REGISTRADO con clave en el listado"));
            it.usarDocumento(cliente.numero());
            claveActual = cliente.clave();
        } else {
            claveActual = PoolClientes.get().claveDe(it.tipoDocumento(), it.numDocumento()).orElse(null);
        }
        if (it.clave() == null) {
            String clave;
            do {
                clave = GeneradorClave.generar();
            } while (clave.equals(claveActual));
            it.asignarClave(clave);
        } else if (it.clave().equals(claveActual)) {
            throw new IllegalStateException("Dato no apto: la Clave del control es igual a la clave actual del cliente "
                    + Enmascarador.documento(it.numDocumento()));
        }
        Allure.step("Cliente de la iteracion: " + Enmascarador.documento(it.numDocumento()));
    }

    /** Espera la pantalla descartando el dialogo de Autofill del sistema si aparece. */
    private boolean esperar(BooleanSupplier pantallaVisible) {
        try {
            return new FluentWait<>(sistema)
                    .withTimeout(Config.segundos("espera.segundos"))
                    .pollingEvery(Duration.ofMillis(700))
                    .until(s -> {
                        if (s.descartarGuardarClaveSiAparece()) {
                            Allure.step("Se descarto el dialogo del sistema para guardar la clave (Autofill)");
                        }
                        return pantallaVisible.getAsBoolean() ? Boolean.TRUE : null;
                    });
        } catch (TimeoutException e) {
            return false;
        }
    }
}
