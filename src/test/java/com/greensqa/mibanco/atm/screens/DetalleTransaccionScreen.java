package com.greensqa.mibanco.atm.screens;

import java.time.Duration;

/** Detalle (comprobante) de una transaccion, abierto desde la lista de movimientos del detalle de cuenta. */
public interface DetalleTransaccionScreen {

    boolean estaVisible(Duration espera);

    /** Verificacion inmediata, sin espera. */
    boolean estaVisible();

    Datos datos();

    /** Toca "Compartir": la app genera la imagen del comprobante y abre el menu de compartir del sistema. */
    void compartir();

    /** Toca "Descargar": la app genera la imagen del comprobante y la guarda en el dispositivo. */
    void descargar();

    /** Aviso de la app tras descargar ("Tu comprobante se descargo correctamente."); vacio si no aparece. */
    java.util.Optional<String> avisoDescarga(Duration espera);

    /** Vuelve al detalle de la cuenta con la flecha de la pantalla. */
    void volver();

    /**
     * @param etiqueta    tipo de monto, p. ej. "MONTO ENVIADO"
     * @param monto       monto como se muestra ("-$1.156.484,73")
     * @param fecha       fecha y hora como se muestran ("21. Sept. 2026 - 10:47 a.m.")
     * @param comprobante numero de comprobante como se muestra
     * @param descripcion descripcion de la transaccion
     * @param compartir   boton "Compartir" visible
     * @param descargar   boton "Descargar" visible
     */
    record Datos(String etiqueta, String monto, String fecha, String comprobante, String descripcion, boolean compartir,
                 boolean descargar) {
    }
}
