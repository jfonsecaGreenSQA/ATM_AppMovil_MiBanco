package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.RegistroExitosoScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

public class RegistroExitosoAndroid extends PantallaAndroid implements RegistroExitosoScreen {

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
    public boolean opcionIngresarVisible() {
        return presente(INGRESAR) && driver().findElement(INGRESAR).isDisplayed();
    }
}
