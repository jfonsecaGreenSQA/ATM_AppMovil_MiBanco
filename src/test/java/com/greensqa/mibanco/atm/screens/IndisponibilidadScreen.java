package com.greensqa.mibanco.atm.screens;

import java.util.Optional;

/**
 * Aviso de indisponibilidad del sistema ("Validacion fallida. No es posible atender tu solicitud por este canal...").
 * Puede aparecer en cualquier pantalla de cualquier flujo.
 */
public interface IndisponibilidadScreen {

    /** @return el texto del aviso si esta en pantalla (verificacion inmediata, sin espera). */
    Optional<String> avisoVisible();

    /** Acepta el aviso ("Entiendo") para poder repetir la accion que lo provoco. */
    void cerrarAviso();
}
