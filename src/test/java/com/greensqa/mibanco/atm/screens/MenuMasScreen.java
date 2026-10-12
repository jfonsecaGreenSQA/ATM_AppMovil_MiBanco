package com.greensqa.mibanco.atm.screens;

/** Opcion "Mas" del menu inferior (perfil, cambiar clave, cerrar sesion...). */
public interface MenuMasScreen {

    /** Cierra la sesion y confirma el dialogo "¿Quieres cerrar sesion?". */
    void cerrarSesion();
}
