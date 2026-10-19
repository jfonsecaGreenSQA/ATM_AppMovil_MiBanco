package com.greensqa.mibanco.atm.screens;

import java.util.Optional;

/**
 * Aviso de indisponibilidad del sistema ("Validacion fallida. No es posible atender tu solicitud por este canal...").
 * Puede aparecer en cualquier pantalla de cualquier flujo.
 */
public interface IndisponibilidadScreen {

    /** @return el texto del aviso si esta en pantalla (verificacion inmediata, sin espera). */
    Optional<String> avisoVisible();

    /**
     * Pagina de error del servidor en lugar de la app (p. ej. "503 Service Temporarily Unavailable" al abrir el login,
     * observado el 15/10/2026). No tiene boton para aceptarla: no se reintenta, la iteracion queda por indisponibilidad.
     *
     * @return el texto del error si esta en pantalla (verificacion inmediata, sin espera)
     */
    Optional<String> errorDelServidor();

    /** Acepta el aviso ("Entiendo") para poder repetir la accion que lo provoco. */
    void cerrarAviso();
}
