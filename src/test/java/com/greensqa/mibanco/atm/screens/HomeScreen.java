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

    void abrirMenuMas();

    boolean hayAviso();
}
