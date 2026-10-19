package com.greensqa.mibanco.atm.screens;

import java.time.Duration;
import java.util.List;

/** Detalle de una cuenta, abierto desde su tarjeta en "Tus cuentas" del Home. */
public interface DetalleCuentaScreen {

    boolean estaVisible(Duration espera);

    /** Verificacion inmediata, sin espera. */
    boolean estaVisible();

    /** Parte superior del detalle, leida sin desplazar la pantalla. */
    Encabezado encabezado();

    /** Recorre la seccion "Movimientos" hasta el final. */
    Movimientos movimientos();

    /** Abre el detalle de la transaccion cuyo boton en la lista tiene ese texto completo. */
    void abrirMovimiento(String texto);

    /** Vuelve al Home con la flecha de la pantalla. */
    void volver();

    /**
     * @param tipo           tipo de cuenta, p. ej. "Cuentas de ahorro"
     * @param numero         numero de cuenta como se muestra, p. ej. "350-003574-524"
     * @param saldoDisponible monto como se muestra ("$0,00")
     * @param saldoEnCanje   monto como se muestra ("$0,00")
     * @param compartir      boton para compartir los datos de la cuenta
     * @param acciones       acciones rapidas de la cuenta, p. ej. "Tus llaves"
     */
    record Encabezado(String tipo, String numero, String saldoDisponible, String saldoEnCanje, boolean compartir,
                      List<String> acciones) {
    }

    /**
     * @param sinMovimientos la app muestra el aviso de que no hay movimientos en los ultimos 30 dias
     * @param aviso          texto del aviso que muestra la seccion (sin movimientos o el de las ultimas 20 transacciones)
     * @param textos         textos de la seccion despues del titulo "Movimientos", en orden
     * @param registros      texto completo de cada movimiento de la lista (sus botones), en orden y sin repetir
     */
    record Movimientos(boolean sinMovimientos, String aviso, List<String> textos, List<String> registros) {
    }
}
