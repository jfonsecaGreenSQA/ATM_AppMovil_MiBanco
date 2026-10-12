package com.greensqa.mibanco.atm.hooks;

/**
 * Falla la iteracion: el aviso de indisponibilidad del sistema persistio tras los reintentos de la accion que lo
 * provoco (ver business.ReintentoIndisponibilidad) o aparecio en un punto sin reintento.
 */
public class IndisponibilidadSistemaException extends AssertionError {

    public IndisponibilidadSistemaException(String aviso) {
        super("Indisponibilidad del sistema: " + aviso);
    }
}
