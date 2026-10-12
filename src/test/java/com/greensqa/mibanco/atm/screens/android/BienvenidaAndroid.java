package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.BienvenidaScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.time.Duration;

public class BienvenidaAndroid extends PantallaAndroid implements BienvenidaScreen {

    private static final By TITULO = rid("mbw-title");
    private static final By REGISTRARME = AppiumBy.accessibilityId("Registrarme");
    private static final By INICIAR_SESION = AppiumBy.accessibilityId("Iniciar sesión");

    @Override
    public boolean estaVisible(Duration espera) {
        return visibleEn(TITULO, espera) && visibleEn(REGISTRARME, Duration.ofSeconds(5));
    }

    @Override
    public void iniciarRegistro() {
        tocar(REGISTRARME);
    }

    @Override
    public void iniciarSesion() {
        tocar(INICIAR_SESION);
    }
}
