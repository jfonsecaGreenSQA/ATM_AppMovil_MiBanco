package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.MenuCompartirScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Hoja de compartir de Android, observada el 19/10/2026 en el S23 (Android 16, paquete com.android.intentresolver):
 * headline "1 imagen" y content_preview_text "Comprobante 000000000000". El paquete cambia segun la version de
 * Android: se ubica por el final del resource-id.
 */
public class MenuCompartirAndroid extends PantallaAndroid implements MenuCompartirScreen {

    private static final By ENCABEZADO = uiSelector("new UiSelector().resourceIdMatches(\".*:id/headline\")");
    private static final By VISTA_PREVIA = uiSelector("new UiSelector().resourceIdMatches(\".*:id/content_preview_text\")");

    @Override
    public boolean estaVisible(Duration espera) {
        return visibleEn(VISTA_PREVIA, espera);
    }

    @Override
    public boolean estaVisible() {
        return presente(VISTA_PREVIA);
    }

    @Override
    public String encabezado() {
        return texto(ENCABEZADO);
    }

    @Override
    public String vistaPrevia() {
        return texto(VISTA_PREVIA);
    }

    @Override
    public void cancelar() {
        driver().navigate().back();
        try {
            new WebDriverWait(driver(), espera()).until(ExpectedConditions.invisibilityOfElementLocated(VISTA_PREVIA));
        } catch (TimeoutException e) {
            // Si sigue abierto, quien llama lo detecta al verificar la pantalla de la app
        }
    }

    private String texto(By by) {
        List<WebElement> e = driver().findElements(by);
        return e.isEmpty() ? "" : e.get(0).getText().trim();
    }
}
