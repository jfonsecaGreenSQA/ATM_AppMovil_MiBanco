package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.Enmascarador;
import com.greensqa.mibanco.atm.data.PoolClientes;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.BienvenidaScreen;
import com.greensqa.mibanco.atm.screens.HomeScreen;
import com.greensqa.mibanco.atm.screens.LoginScreen;
import com.greensqa.mibanco.atm.screens.MenuMasScreen;
import com.greensqa.mibanco.atm.screens.Pantallas;
import io.qameta.allure.Allure;

/** Tareas de negocio del ingreso a la app con documento y clave. Depende solo de contratos de pantalla. */
public class LoginBusiness {

    private final BienvenidaScreen bienvenida = Pantallas.bienvenida();
    private final LoginScreen login = Pantallas.login();
    private final HomeScreen home = Pantallas.home();
    private final MenuMasScreen menuMas = Pantallas.menuMas();
    private final ReintentoIndisponibilidad reintento = new ReintentoIndisponibilidad();

    /**
     * Ingresa con el cliente de la iteracion y espera la respuesta de la app: el Home o un aviso.
     * Con NumDocumento AUTO toma un cliente REGISTRADO con clave del listado.
     *
     * @return tipo de documento que quedo seleccionado en la app
     */
    public String iniciarSesion(Iteracion it) {
        resolverCliente(it);
        bienvenida.iniciarSesion();
        login.esperarVisible();
        login.seleccionarTipoDocumento(it.tipoDocumento());
        String tipoSeleccionado = login.tipoDocumentoSeleccionado();
        login.ingresarNumeroDocumento(it.numDocumento());
        login.ingresarClave(it.clave());
        Evidencias.captura("Datos de ingreso diligenciados");
        // El aviso de indisponibilidad borra la clave: se vuelve a escribir antes de reintentar
        reintento.ejecutar("ingresar", login::ingresar, () -> login.ingresarClave(it.clave()),
                () -> home.estaVisible() || home.hayAviso() || login.hayAviso(), Config.segundos("espera.carga.app.segundos"));
        return tipoSeleccionado;
    }

    public boolean homeVisible() {
        return home.estaVisible(Config.segundos("espera.segundos"));
    }

    public String saludoHome() {
        return home.saludo();
    }

    public boolean menuInferiorVisible() {
        return home.menuInferiorVisible();
    }

    /**
     * Cierra la sesion desde Mas > Cerrar sesion y espera la bienvenida. Sin cerrarla, el siguiente ingreso del mismo
     * cliente responde "Sesion activa" hasta que venza en el servidor.
     *
     * @return true si la app volvio a la bienvenida
     */
    public boolean cerrarSesion() {
        home.abrirMenuMas();
        menuMas.cerrarSesion();
        return bienvenida.estaVisible(Config.segundos("espera.segundos"));
    }

    /** Aviso de error mostrado tras ingresar, o vacio si no hay. */
    public String avisoTrasIngresar() {
        if (login.hayAviso()) {
            return login.textoAviso();
        }
        return home.hayAviso() ? "Aviso en el Home" : "";
    }

    private void resolverCliente(Iteracion it) {
        // Con AUTO, otro componente pudo elegir ya al cliente (p. ej. por producto del Home): se respeta
        if (it.documentoAutomatico() && it.numDocumento() == null) {
            PoolClientes.ClienteRegistrado cliente = PoolClientes.get().registradoConClave(it.tipoDocumento())
                    .orElseThrow(() -> new IllegalStateException("Dato no apto: no hay clientes " + it.tipoDocumento()
                            + " REGISTRADO con clave en el listado"));
            it.usarClienteRegistrado(cliente.numero(), cliente.clave());
        } else if (it.clave() == null) {
            String clave = PoolClientes.get().claveDe(it.tipoDocumento(), it.numDocumento())
                    .orElseThrow(() -> new IllegalStateException("Dato no apto: el documento "
                            + Enmascarador.documento(it.numDocumento()) + " no esta REGISTRADO con clave en el listado;"
                            + " informe la Clave en el control"));
            it.usarClienteRegistrado(it.numDocumento(), clave);
        }
        Allure.step("Cliente de la iteracion: " + Enmascarador.documento(it.numDocumento()));
    }
}
