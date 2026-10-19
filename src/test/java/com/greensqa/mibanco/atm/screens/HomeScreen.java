package com.greensqa.mibanco.atm.screens;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Home del cliente autenticado ("Hola, ...", accesos, productos y menu inferior). */
public interface HomeScreen {

    boolean estaVisible(Duration espera);

    /** Verificacion inmediata, sin espera. */
    boolean estaVisible();

    String saludo();

    String preguntaActividad();

    /** Etiqueta "Ultimo ingreso: ..."; solo aparece si el cliente ya habia ingresado antes. */
    Optional<String> ultimoIngreso();

    boolean menuInferiorVisible();

    /** Acceso directo de la parte superior (Bre-B, Pedir Credito...) visible, ubicado por su nombre. */
    boolean accesoSuperiorVisible(String nombre);

    /** Etiquetas de los botones del menu inferior, de izquierda a derecha. */
    List<String> accesosMenuInferior();

    /**
     * Productos del cliente en el Home (cuentas, creditos y banner de pre-aprobado), recorriendo la pantalla hasta el
     * final. Espera a que terminen de cargar (sin skeleton).
     */
    List<ProductoHome> productos();

    /**
     * Empieza a observar en segundo plano la carga del Home (skeleton "Cargando..." y montos). Se inicia antes de
     * ingresar: el skeleton dura pocos segundos.
     */
    ObservacionCarga observarCarga();

    /** Seccion de campanas "Exclusivo para ti": desplaza el Home hasta ella y lee el banner. */
    Campanas campanas();

    /** Desliza el banner hacia la siguiente campana y lo vuelve a leer (igual si no hay otra). */
    Campanas siguienteCampana();

    /**
     * @param titulo  titulo de la seccion; vacio si no aparece
     * @param campana nombre de la campana visible segun su descripcion de accesibilidad (p. ej. "Promo 2")
     * @param total   campanas del carrusel segun sus puntos de paginacion; 1 si no hay paginacion
     * @param actual  posicion de la campana visible (1 = primera)
     */
    record Campanas(String titulo, String campana, int total, int actual) {
    }

    /** Abre el detalle de la cuenta cuya tarjeta en "Tus cuentas" tiene ese titulo (p. ej. "Cuentas de ahorro **** 1234"). */
    void abrirCuenta(String titulo);

    void abrirMenuMas();

    boolean hayAviso();
}
