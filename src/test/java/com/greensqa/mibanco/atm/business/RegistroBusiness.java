package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.Enmascarador;
import com.greensqa.mibanco.atm.data.PoolClientes;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.BienvenidaScreen;
import com.greensqa.mibanco.atm.screens.CrearClaveScreen;
import com.greensqa.mibanco.atm.screens.DialogosSistemaScreen;
import com.greensqa.mibanco.atm.screens.OtpScreen;
import com.greensqa.mibanco.atm.screens.Pantallas;
import com.greensqa.mibanco.atm.screens.RegistroDocumentoScreen;
import com.greensqa.mibanco.atm.screens.RegistroExitosoScreen;
import io.qameta.allure.Allure;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

/** Tareas de negocio del registro de usuario. Depende solo de contratos de pantalla. */
public class RegistroBusiness {

    private enum RespuestaDocumento { OTP, YA_REGISTRADO, OTRO_AVISO }

    private final BienvenidaScreen bienvenida = Pantallas.bienvenida();
    private final RegistroDocumentoScreen documento = Pantallas.registroDocumento();
    private final OtpScreen otp = Pantallas.otp();
    private final CrearClaveScreen crearClave = Pantallas.crearClave();
    private final RegistroExitosoScreen exito = Pantallas.registroExitoso();
    private final DialogosSistemaScreen sistema = Pantallas.dialogosSistema();
    private final ReintentoIndisponibilidad reintento = new ReintentoIndisponibilidad();

    /**
     * Inicia el registro con el tipo y numero de documento de la iteracion.
     * Con NumDocumento AUTO toma clientes DISPONIBLE del listado; si la app responde que el documento
     * ya esta registrado lo marca YA_REGISTRADO_PREVIO y toma el siguiente (maximo registro.max.intentos).
     *
     * @return tipo de documento que quedo seleccionado en la app
     */
    public String iniciarRegistroConDocumento(Iteracion it) {
        bienvenida.iniciarRegistro();
        documento.esperarVisible();
        documento.seleccionarTipoDocumento(it.tipoDocumento());
        String tipoSeleccionado = documento.tipoDocumentoSeleccionado();
        int maximo = Config.entero("registro.max.intentos");
        for (int intento = 1; ; intento++) {
            String numero = documentoParaIntento(it);
            documento.ingresarNumeroDocumento(numero);
            Evidencias.captura("Documento ingresado (intento " + intento + ")");
            reintento.ejecutar("continuar con el documento", documento::continuar, () -> { },
                    () -> otp.estaVisible() || documento.hayAviso(), Config.segundos("espera.segundos"));
            RespuestaDocumento respuesta = esperarRespuestaDocumento();
            if (respuesta == RespuestaDocumento.OTP) {
                return tipoSeleccionado;
            }
            String aviso = documento.textoAviso();
            Evidencias.captura("Aviso: " + aviso);
            if (respuesta == RespuestaDocumento.OTRO_AVISO) {
                throw new AssertionError("La app mostro un aviso inesperado al continuar: " + aviso);
            }
            PoolClientes.get().actualizar(it.tipoDocumento(), numero, PoolClientes.Estado.YA_REGISTRADO_PREVIO, null,
                    it.caso().idAzure(), "La app indico: " + aviso);
            it.descartarDocumento();
            if (!it.documentoAutomatico()) {
                throw new IllegalStateException("Dato no apto: el documento " + Enmascarador.documento(numero)
                        + " ya esta registrado (" + aviso + ")");
            }
            if (intento >= maximo) {
                throw new IllegalStateException("Dato no apto: " + maximo
                        + " documentos consecutivos del listado ya estaban registrados");
            }
            Allure.step("Documento " + Enmascarador.documento(numero) + " ya registrado; se toma el siguiente del listado (intento "
                    + (intento + 1) + " de " + maximo + ")");
            documento.cerrarAviso();
        }
    }

    /** @return true si tras validar el OTP se muestra la creacion de clave. */
    public boolean validarIdentidad(String codigoOtp) {
        otp.ingresarCodigo(codigoOtp);
        Evidencias.captura("OTP ingresado");
        reintento.ejecutar("validar el OTP", otp::validar, () -> { },
                () -> crearClave.estaVisible(Duration.ofMillis(300)), Config.segundos("espera.segundos"));
        return crearClave.estaVisible(Config.segundos("espera.segundos"));
    }

    /** @return true si la app sale de la creacion de clave y muestra la confirmacion del registro. */
    public boolean crearClaveAceptandoTerminos(String clave) {
        crearClave.ingresarClave(clave);
        crearClave.confirmarClave(clave);
        crearClave.aceptarReglamentoDeUso();
        crearClave.aceptarTratamientoDeDatos();
        Evidencias.captura("Clave y terminos diligenciados");
        reintento.ejecutar("crear la clave", crearClave::continuar, () -> { },
                () -> exito.estaVisible() || sistema.descartarGuardarClaveSiAparece(), Config.segundos("espera.segundos"));
        return esperarConfirmacion();
    }

    public String tituloConfirmacion() {
        return exito.titulo();
    }

    public boolean opcionIngresarVisible() {
        return exito.opcionIngresarVisible();
    }

    /** Deja el cliente REGISTRADO con su clave para los casos de login y Home. */
    public void guardarClienteRegistrado(Iteracion it) {
        PoolClientes.get().actualizar(it.tipoDocumento(), it.numDocumento(), PoolClientes.Estado.REGISTRADO, it.clave(),
                it.caso().idAzure(), "Registro exitoso - IdPrueba " + it.idPrueba());
        it.liberarDocumento();
    }

    private String documentoParaIntento(Iteracion it) {
        if (!it.documentoAutomatico()) {
            return it.numDocumento();
        }
        boolean lineaSms = it.caso().requiereLineaSms();
        String numero = PoolClientes.get().reservarDisponible(it.tipoDocumento(), it.caso().idAzure(), lineaSms)
                .orElseThrow(() -> new IllegalStateException("Dato no apto: no hay clientes " + it.tipoDocumento()
                        + " DISPONIBLE" + (lineaSms ? " con LINEA_SMS = SI" : "") + " en el listado"));
        it.reservarDocumento(numero);
        return numero;
    }

    private RespuestaDocumento esperarRespuestaDocumento() {
        return new FluentWait<>(documento)
                .withTimeout(Config.segundos("espera.segundos"))
                .pollingEvery(Duration.ofMillis(700))
                .withMessage("no aparecio la solicitud de OTP ni un aviso tras continuar con el documento")
                .until(d -> {
                    if (otp.estaVisible()) {
                        return RespuestaDocumento.OTP;
                    }
                    if (d.hayAviso()) {
                        return d.avisoIndicaDocumentoRegistrado() ? RespuestaDocumento.YA_REGISTRADO : RespuestaDocumento.OTRO_AVISO;
                    }
                    return null;
                });
    }

    /** Espera la confirmacion descartando el dialogo de Autofill del sistema si aparece. */
    private boolean esperarConfirmacion() {
        try {
            return new FluentWait<>(exito)
                    .withTimeout(Config.segundos("espera.segundos"))
                    .pollingEvery(Duration.ofMillis(700))
                    .until(e -> {
                        if (sistema.descartarGuardarClaveSiAparece()) {
                            Allure.step("Se descarto el dialogo del sistema para guardar la clave (Autofill)");
                        }
                        return e.estaVisible() ? Boolean.TRUE : null;
                    });
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }
}
