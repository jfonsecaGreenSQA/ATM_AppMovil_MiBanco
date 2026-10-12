package com.greensqa.mibanco.atm.screens;

import java.util.List;

/** Bandeja de SMS del dispositivo de la ejecucion (SIM con el celular registrado del cliente). */
public interface BandejaSmsScreen {

    /** @param fecha epoch ms segun el reloj del dispositivo */
    record Sms(String remitente, long fecha, String cuerpo) {
    }

    /** SMS recibidos, del mas reciente al mas antiguo. */
    List<Sms> recibidos();
}
