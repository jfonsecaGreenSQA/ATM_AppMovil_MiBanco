package com.greensqa.mibanco.atm.hooks;

import org.opentest4j.TestAbortedException;

/** Cancela la iteracion sin marcarla fallida: la app reporto indisponibilidad del sistema. */
public class IndisponibilidadSistemaException extends TestAbortedException {

    public IndisponibilidadSistemaException(String aviso) {
        super("Indisponibilidad del sistema: " + aviso);
    }
}
