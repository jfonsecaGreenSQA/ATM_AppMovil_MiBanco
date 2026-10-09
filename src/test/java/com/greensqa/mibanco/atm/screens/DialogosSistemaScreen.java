package com.greensqa.mibanco.atm.screens;

/** Dialogos del sistema operativo que interrumpen el flujo de la app. */
public interface DialogosSistemaScreen {

    /** @return true si aparecio el dialogo de guardar clave (Autofill) y se descarto. */
    boolean descartarGuardarClaveSiAparece();
}
