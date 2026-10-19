package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.Movimiento;
import com.greensqa.mibanco.atm.driver.ArchivosDispositivo;
import com.greensqa.mibanco.atm.driver.DriverManager;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.DetalleCuentaScreen;
import com.greensqa.mibanco.atm.screens.DetalleTransaccionScreen;
import com.greensqa.mibanco.atm.screens.HomeScreen;
import com.greensqa.mibanco.atm.screens.MenuCompartirScreen;
import com.greensqa.mibanco.atm.screens.Pantallas;
import com.greensqa.mibanco.atm.screens.ProductoHome;
import io.qameta.allure.Allure;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Cuentas del cliente (detalle de cuenta). El ingreso reutiliza la busqueda de productos del Home: con AUTO toma un
 * cliente del listado que muestre la cuenta del tipo de la iteracion.
 */
public class CuentasBusiness {

    private final ProductosHomeBusiness productos = new ProductosHomeBusiness();
    private final HomeScreen home = Pantallas.home();
    private final DetalleCuentaScreen detalle = Pantallas.detalleCuenta();
    private final DetalleTransaccionScreen transaccion = Pantallas.detalleTransaccion();
    private DetalleCuentaScreen.Movimientos ultimaLectura;

    /** @return true si llego al Home con un cliente que muestra la cuenta (ver {@link ProductosHomeBusiness}) */
    public boolean ingresarConCuenta(Iteracion it) {
        return productos.ingresarConProducto(it);
    }

    /** Abre el detalle de la primera cuenta del tipo de la iteracion que mostro el Home. */
    public boolean abrirDetalle(Iteracion it) {
        ProductoHome cuenta = productos.productosDe(it.tipoProducto()).get(0);
        Allure.step("Cuenta que se abre: " + cuenta.titulo());
        home.abrirCuenta(cuenta.titulo());
        return detalle.estaVisible(Config.segundos("espera.carga.app.segundos"));
    }

    public DetalleCuentaScreen.Encabezado encabezado() {
        return detalle.encabezado();
    }

    public DetalleCuentaScreen.Movimientos movimientos() {
        ultimaLectura = detalle.movimientos();
        return ultimaLectura;
    }

    /** Movimientos de la ultima lectura con el formato de la app (descripcion, fecha y monto), en el orden de la lista. */
    public List<Movimiento> movimientosLeidos() {
        return registros().stream().map(Movimiento::desdeApp).flatMap(Optional::stream).toList();
    }

    /** Registros de la ultima lectura que no tienen el formato de un movimiento. */
    public List<String> registrosSinFormato() {
        return registros().stream().filter(r -> Movimiento.desdeApp(r).isEmpty()).toList();
    }

    /**
     * Vuelve a recorrer la lista desde donde quedo (el final) forzando el desplazamiento.
     *
     * @return movimientos que no estaban en la lectura anterior (vacio si la lista no crecio)
     */
    public List<String> movimientosNuevosAlForzarElFinal() {
        List<String> antes = registros();
        return detalle.movimientos().registros().stream().filter(r -> !antes.contains(r)).toList();
    }

    /**
     * Abre el detalle del primer movimiento de la lista (el mas reciente).
     *
     * @throws IllegalStateException "Dato no apto" si la cuenta no tiene movimientos
     */
    public Movimiento abrirPrimerMovimiento() {
        if (ultimaLectura == null) {
            movimientos();
        }
        Movimiento primero = movimientosLeidos().stream().findFirst().orElseThrow(() -> new IllegalStateException(
                "Dato no apto: la cuenta no tiene movimientos. Informe en el control un cliente con movimientos"));
        Allure.step("Movimiento que se abre: " + primero.descripcion() + " | " + primero.fecha() + " | " + primero.monto());
        detalle.abrirMovimiento(primero.texto());
        return primero;
    }

    public boolean detalleTransaccionVisible() {
        return transaccion.estaVisible(Config.segundos("espera.segundos"));
    }

    public DetalleTransaccionScreen.Datos datosTransaccion() {
        return transaccion.datos();
    }

    /**
     * Descarga el comprobante y comprueba que la imagen quede guardada (archivo nuevo o actualizado). La imagen se
     * adjunta a la evidencia y se borra del dispositivo (autorizado por el usuario el 15/10/2026).
     */
    public ComprobanteDescargado descargarComprobante(DetalleTransaccionScreen.Datos datos) {
        String udid = DriverManager.udid();
        String ruta = Config.get("comprobante.descarga.ruta").replace("{numero}", datos.comprobante());
        long antes = ArchivosDispositivo.modificado(udid, ruta).orElse(-1);
        transaccion.descargar();
        Optional<String> aviso = transaccion.avisoDescarga(Config.segundos("espera.segundos"));
        boolean guardado;
        try {
            guardado = new FluentWait<>(ruta).withTimeout(Config.segundos("espera.segundos")).pollingEvery(Duration.ofMillis(700))
                    .until(r -> ArchivosDispositivo.modificado(udid, r).orElse(-1) > antes ? Boolean.TRUE : null);
        } catch (TimeoutException e) {
            guardado = false;
        }
        int bytes = 0;
        if (guardado) {
            try {
                byte[] imagen = ArchivosDispositivo.leer(udid, ruta);
                bytes = imagen.length;
                Evidencias.capturaPng("Comprobante descargado", imagen);
            } catch (IOException e) {
                Allure.step("No se pudo copiar el comprobante a la evidencia: " + e.getMessage());
            }
        }
        boolean borrado = ArchivosDispositivo.borrar(udid, ruta);
        return new ComprobanteDescargado(aviso.orElse(""), ruta, guardado, bytes, borrado);
    }

    /**
     * Abre el menu de compartir y lo cierra sin elegir app. El menu muestra contactos y apps del dispositivo: antes de
     * abrirlo se detiene el video y del menu solo se guardan su encabezado y su vista previa.
     */
    public MenuCompartir compartirComprobante() {
        Evidencias.detenerVideo();
        Allure.step("El video se detiene antes de abrir el menu de compartir (muestra contactos del dispositivo)");
        MenuCompartirScreen menu = Pantallas.menuCompartir();
        transaccion.compartir();
        try {
            boolean visible = menu.estaVisible(Config.segundos("espera.segundos"));
            String encabezado = visible ? menu.encabezado() : "";
            String vistaPrevia = visible ? menu.vistaPrevia() : "";
            Evidencias.texto("Menu de compartir", "Encabezado: " + encabezado + "\nVista previa: " + vistaPrevia);
            return new MenuCompartir(visible, encabezado, vistaPrevia, cerrar(menu));
        } catch (RuntimeException e) {
            cerrar(menu);
            throw e;
        }
    }

    private boolean cerrar(MenuCompartirScreen menu) {
        if (menu.estaVisible()) {
            menu.cancelar();
        }
        return transaccion.estaVisible(Config.segundos("espera.segundos"));
    }

    /**
     * @param aviso    aviso de la app tras descargar; vacio si no aparecio
     * @param ruta     ruta esperada de la imagen en el dispositivo
     * @param guardado la imagen se creo o actualizo tras tocar Descargar
     * @param bytes    tamano de la imagen copiada a la evidencia
     * @param borrado  la imagen ya no esta en el dispositivo
     */
    public record ComprobanteDescargado(String aviso, String ruta, boolean guardado, int bytes, boolean borrado) {
    }

    /** @param cerrado al cancelar el menu se volvio al detalle de la transaccion */
    public record MenuCompartir(boolean visible, String encabezado, String vistaPrevia, boolean cerrado) {
    }

    private List<String> registros() {
        return ultimaLectura == null ? List.of() : ultimaLectura.registros();
    }
}
