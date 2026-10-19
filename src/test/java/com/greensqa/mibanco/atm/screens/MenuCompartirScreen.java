package com.greensqa.mibanco.atm.screens;

import java.time.Duration;

/**
 * Menu de compartir del sistema (hoja de Android). Muestra contactos y apps del dispositivo: no se guarda su captura
 * como evidencia, solo su encabezado y su vista previa.
 */
public interface MenuCompartirScreen {

    boolean estaVisible(Duration espera);

    /** Verificacion inmediata, sin espera. */
    boolean estaVisible();

    /** Encabezado, p. ej. "1 imagen". */
    String encabezado();

    /** Texto de la vista previa del contenido, p. ej. "Comprobante 000000000000". */
    String vistaPrevia();

    /** Cierra el menu sin elegir app (tecla Atras del sistema). */
    void cancelar();
}
