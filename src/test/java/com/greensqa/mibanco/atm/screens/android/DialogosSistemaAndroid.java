package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.DialogosSistemaScreen;
import org.openqa.selenium.By;

public class DialogosSistemaAndroid extends PantallaAndroid implements DialogosSistemaScreen {

    // Hoja "Save password to Google Password Manager" del servicio Autofill
    private static final By AUTOFILL_NO_GUARDAR = rid("android:id/autofill_save_no");

    @Override
    public boolean descartarGuardarClaveSiAparece() {
        if (presente(AUTOFILL_NO_GUARDAR)) {
            tocar(driver().findElement(AUTOFILL_NO_GUARDAR));
            return true;
        }
        return false;
    }
}
