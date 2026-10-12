package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.ClaveCambiadaScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/** Confirmacion de la recuperacion de clave. Mismos id que la confirmacion del registro (observado en el S23). */
public class ClaveCambiadaAndroid extends PantallaAndroid implements ClaveCambiadaScreen {

    private static final By TITULO = rid("success-page-title");
    private static final By INGRESAR = AppiumBy.accessibilityId("Ingresar");

    @Override
    public boolean estaVisible() {
        return presente(TITULO);
    }

    @Override
    public String titulo() {
        return esperarVisible(TITULO).getText().trim();
    }

    @Override
    public void ingresar() {
        tocar(INGRESAR);
    }
}
