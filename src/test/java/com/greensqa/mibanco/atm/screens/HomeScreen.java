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

    void abrirMenuMas();

    boolean hayAviso();
}
