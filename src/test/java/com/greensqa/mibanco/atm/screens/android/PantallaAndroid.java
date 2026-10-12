package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.driver.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;

/**
 * Utilidades comunes de los Page Objects Android. La UI es web (Modyo) dentro de un WebView:
 * los id del HTML se exponen como resource-id sin prefijo de paquete, por eso se ubican con UiSelector.
 */
abstract class PantallaAndroid {

    protected AndroidDriver driver() {
        return DriverManager.driver();
    }

    protected static By rid(String resourceId) {
        return AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"" + resourceId + "\")");
    }

    protected static By uiSelector(String selector) {
        return AppiumBy.androidUIAutomator(selector);
    }

    protected Duration espera() {
        return Config.segundos("espera.segundos");
    }

    protected WebElement esperarVisible(By by) {
        return esperarVisible(by, espera());
    }

    protected WebElement esperarVisible(By by, Duration espera) {
        return new WebDriverWait(driver(), espera).until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    protected boolean visibleEn(By by, Duration espera) {
        try {
            esperarVisible(by, espera);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Verificacion inmediata, sin espera. */
    protected boolean presente(By by) {
        return !driver().findElements(by).isEmpty();
    }

    protected void tocar(By by) {
        tocar(new WebDriverWait(driver(), espera()).until(ExpectedConditions.elementToBeClickable(by)));
    }

    /**
     * Toque por gesto: en el WebView, element.click() de UiAutomator2 tarda ~10 s esperando la sincronizacion
     * de accesibilidad; clickGesture toca el centro del elemento en menos de 1 s (medido en el emulador).
     */
    protected void tocar(WebElement elemento) {
        driver().executeScript("mobile: clickGesture", Map.of("elementId", ((RemoteWebElement) elemento).getId()));
    }

    protected void escribir(By by, String valor) {
        WebElement campo = esperarVisible(by);
        tocar(campo);
        campo.clear();
        campo.sendKeys(valor);
    }

    /**
     * Escribe pulsando teclas reales: setValue de UiAutomator2 asigna el texto sin eventos de teclado y
     * los componentes web que escuchan esos eventos (p. ej. las 6 casillas del OTP) no lo reciben.
     */
    protected void escribirConTeclas(By by, String valor) {
        tocar(esperarVisible(by));
        for (char c : valor.toCharArray()) {
            if (!Character.isDigit(c)) {
                throw new IllegalArgumentException("escribirConTeclas solo admite digitos");
            }
            driver().pressKey(new KeyEvent(AndroidKey.valueOf("DIGIT_" + c)));
        }
    }

    /**
     * Cierra el teclado tocando el encabezado de la pantalla, zona sin accion entre la flecha de volver y los
     * campos: el WebView quita el foco del campo y oculta el teclado. No se usa hideKeyboard: Appium envia la
     * tecla Atras cuando el teclado no cierra con Escape, y en esta app Atras cierra la hoja del OTP o abre el
     * dialogo "¿Estas seguro que quieres salir?".
     */
    protected void ocultarTeclado() {
        ocultarTeclado(0.17);
    }

    /** @param alto proporcion de la altura de la pantalla donde se toca (zona sin accion de esa pantalla) */
    protected void ocultarTeclado(double alto) {
        Dimension pantalla = driver().manage().window().getSize();
        driver().executeScript("mobile: clickGesture", Map.of("x", pantalla.width / 2, "y", (int) (pantalla.height * alto)));
    }

    /**
     * Si el teclado esta abierto lo cierra tocando un texto sin accion de la pantalla (p. ej. su titulo): el WebView
     * quita el foco del campo. En celulares con teclado alto (Samsung S23) el teclado tapa campos y botones que en
     * el emulador quedan visibles. Si el texto no esta en pantalla, usa la franja del encabezado.
     */
    protected void cerrarTecladoTocando(By textoSinAccion) {
        if (!driver().isKeyboardShown()) {
            return;
        }
        java.util.List<WebElement> zona = driver().findElements(textoSinAccion);
        if (zona.isEmpty()) {
            ocultarTeclado();
        } else {
            tocar(zona.get(0));
        }
    }

    /** Desliza hacia arriba hasta que el elemento quede completo dentro de la pantalla (maximo 3 intentos). */
    protected WebElement asegurarEnPantalla(By by) {
        Dimension pantalla = driver().manage().window().getSize();
        WebElement e = esperarVisible(by);
        for (int i = 0; i < 3 && e.getRect().y + e.getRect().height > pantalla.height * 0.9; i++) {
            driver().executeScript("mobile: swipeGesture", Map.of(
                    "left", pantalla.width / 4, "top", pantalla.height / 3,
                    "width", pantalla.width / 2, "height", pantalla.height / 3,
                    "direction", "up", "percent", 0.5));
            e = esperarVisible(by);
        }
        return e;
    }

    /**
     * Toca la casilla de un checkbox web en su esquina superior izquierda: el centro del elemento cae
     * sobre los enlaces del texto. Fragil ante cambios de diseno (ver solicitud de Test IDs).
     */
    protected void tocarCasilla(By by) {
        Rectangle r = asegurarEnPantalla(by).getRect();
        int desplazamiento = r.height / 4;
        driver().executeScript("mobile: clickGesture", Map.of("x", r.x + desplazamiento, "y", r.y + desplazamiento));
    }
}
