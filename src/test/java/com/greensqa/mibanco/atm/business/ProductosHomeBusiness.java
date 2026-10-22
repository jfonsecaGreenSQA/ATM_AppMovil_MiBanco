package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.Enmascarador;
import com.greensqa.mibanco.atm.data.PoolClientes;
import com.greensqa.mibanco.atm.data.TipoProducto;
import com.greensqa.mibanco.atm.screens.HomeScreen;
import com.greensqa.mibanco.atm.screens.Pantallas;
import com.greensqa.mibanco.atm.screens.ProductoHome;
import io.qameta.allure.Allure;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Productos del Home. No todos los clientes tienen todos los productos: con NumDocumento AUTO se prueba con los
 * clientes del listado hasta encontrar uno que muestre el producto, y cada Home revisado queda inventariado en el
 * listado (PRODUCTOS_HOME) para elegir mejor la proxima vez.
 */
public class ProductosHomeBusiness {

    /** Aviso de la app cuando la clave no corresponde al documento. */
    private static final String DATOS_INCORRECTOS = "Datos incorrectos";

    private final HomeScreen home = Pantallas.home();
    private final LoginBusiness login = new LoginBusiness();
    private final InicioAppBusiness inicio = new InicioAppBusiness();
    private List<ProductoHome> productos = List.of();

    /**
     * Ingresa con un cliente que tenga el producto de la iteracion.
     *
     * @return true si llego al Home con un cliente que muestra el producto; false si no llego al Home
     * @throws IllegalStateException "Dato no apto" si ningun cliente revisado tiene el producto
     */
    public boolean ingresarConProducto(Iteracion it) {
        TipoProducto producto = it.tipoProducto();
        if (!it.documentoAutomatico()) {
            login.iniciarSesion(it);
            return login.homeVisible() && conProductoOInforme(it, producto);
        }
        List<PoolClientes.ClienteRegistrado> candidatos = PoolClientes.get().registradosParaProducto(it.tipoDocumento(), producto);
        int maximo = Math.min(Config.entero("productos.max.clientes"), candidatos.size());
        for (int i = 0; i < maximo; i++) {
            PoolClientes.ClienteRegistrado cliente = candidatos.get(i);
            it.usarClienteRegistrado(cliente.numero(), cliente.clave());
            login.iniciarSesion(it);
            if (!login.homeVisible()) {
                String aviso = login.avisoTrasIngresar();
                if (!aviso.startsWith(DATOS_INCORRECTOS)) {
                    return false;
                }
                // Clave del listado desactualizada: no se reintenta (al tercer intento la app bloquea el acceso)
                PoolClientes.get().actualizar(it.tipoDocumento(), cliente.numero(), PoolClientes.Estado.FALLIDO, null,
                        it.caso().idAzure(), "Clave del listado rechazada (Datos incorrectos) al buscar " + producto
                                + "; revisar o recuperar la clave");
                Allure.step("La app rechazo la clave del listado para " + Enmascarador.documento(cliente.numero())
                        + ": el cliente queda FALLIDO en el listado y se prueba con otro");
                inicio.irABienvenida();
                continue;
            }
            if (tiene(inventariar(it), producto)) {
                return true;
            }
            Allure.step("El cliente " + Enmascarador.documento(cliente.numero()) + " no tiene " + producto.descripcion()
                    + " en el Home: se cierra la sesion y se prueba con otro cliente del listado");
            login.cerrarSesion();
        }
        throw new IllegalStateException("Dato no apto: " + (maximo == 0
                ? "no hay clientes " + it.tipoDocumento() + " REGISTRADO con clave que tengan o puedan tener " + producto
                : "ninguno de los " + maximo + " clientes revisados muestra " + producto.descripcion() + " en el Home")
                + ". Informe en el control el documento de un cliente que tenga el producto");
    }

    private boolean conProductoOInforme(Iteracion it, TipoProducto producto) {
        if (tiene(inventariar(it), producto)) {
            return true;
        }
        throw new IllegalStateException("Dato no apto: el cliente " + Enmascarador.documento(it.numDocumento())
                + " no muestra " + producto.descripcion() + " en el Home");
    }

    /** Lee los productos del Home y los registra en el listado de clientes. */
    public List<ProductoHome> inventariar(Iteracion it) {
        productos = home.productos();
        Set<TipoProducto> tipos = productos.stream().map(TipoProducto::de).flatMap(java.util.Optional::stream)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoProducto.class)));
        PoolClientes.get().registrarProductos(it.tipoDocumento(), it.numDocumento(), tipos);
        Allure.step("Productos del Home: " + (tipos.isEmpty() ? "ninguno conocido" : tipos)
                + " (" + productos.size() + " tarjetas)");
        return productos;
    }

    /** Productos del tipo indicado leidos en el ultimo inventario. */
    public List<ProductoHome> productosDe(TipoProducto tipo) {
        return productos.stream().filter(p -> TipoProducto.de(p).filter(tipo::equals).isPresent()).toList();
    }

    /** Deja la tarjeta en pantalla para la evidencia; si no la ubica lo deja anotado en el reporte. */
    public void mostrar(ProductoHome producto) {
        if (!home.mostrarProducto(producto)) {
            Allure.step("No se pudo dejar en pantalla la tarjeta '" + producto.titulo()
                    + "': la captura puede mostrar otro producto");
        }
    }

    private static boolean tiene(List<ProductoHome> productos, TipoProducto tipo) {
        return productos.stream().anyMatch(p -> TipoProducto.de(p).filter(tipo::equals).isPresent());
    }
}
