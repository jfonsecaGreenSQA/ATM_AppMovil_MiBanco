package com.greensqa.mibanco.atm.driver;

import com.greensqa.mibanco.atm.config.Config;
import io.appium.java_client.Setting;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.appmanagement.ApplicationState;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

/**
 * Unico punto que crea el driver. La sesion no reinstala ni lanza la app (noReset + autoLaunch=false):
 * cada flujo decide con {@link #asegurarAppEnPrimerPlano()} si continua o abre la app.
 */
public final class DriverManager {

    private static AndroidDriver driver;

    private DriverManager() {
    }

    public static AndroidDriver iniciar() {
        UiAutomator2Options opciones = new UiAutomator2Options()
                .setAppPackage(paquete())
                .setAppActivity(Config.get("app.activity"))
                .setNoReset(true)
                .setNewCommandTimeout(Duration.ofMinutes(5))
                .setUiautomator2ServerLaunchTimeout(Duration.ofSeconds(90));
        opciones.setCapability("appium:autoLaunch", false);
        String udid = Config.get("device.udid");
        if (!udid.isBlank()) {
            opciones.setUdid(udid);
        }
        try {
            driver = new AndroidDriver(URI.create(Config.get("appium.url")).toURL(), opciones);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("appium.url invalida: " + Config.get("appium.url"), e);
        }
        // El WebView casi nunca queda "idle": sin este ajuste UiAutomator2 espera hasta 10 s antes de cada comando
        driver.setSetting(Setting.WAIT_FOR_IDLE_TIMEOUT, Config.entero("uiautomator.idle.ms"));
        return driver;
    }

    public static AndroidDriver driver() {
        if (driver == null) {
            throw new IllegalStateException("No hay sesion de Appium activa");
        }
        return driver;
    }

    public static boolean activo() {
        return driver != null;
    }

    /** @return true si la app ya estaba en primer plano; false si hubo que abrirla. */
    public static boolean asegurarAppEnPrimerPlano() {
        if (driver().queryAppState(paquete()) == ApplicationState.RUNNING_IN_FOREGROUND) {
            return true;
        }
        driver().activateApp(paquete());
        return false;
    }

    /** Cierra y vuelve a abrir la app (cuando esta en primer plano pero no en la pantalla esperada). */
    public static void reiniciarApp() {
        driver().terminateApp(paquete());
        driver().activateApp(paquete());
    }

    public static String udid() {
        Object udid = driver().getCapabilities().getCapability("udid");
        if (udid == null) {
            udid = driver().getCapabilities().getCapability("appium:udid");
        }
        return udid == null ? Config.get("device.udid") : udid.toString();
    }

    public static String capacidad(String nombre) {
        Object v = driver().getCapabilities().getCapability(nombre);
        if (v == null) {
            v = driver().getCapabilities().getCapability("appium:" + nombre);
        }
        return v == null ? "" : v.toString();
    }

    public static String paquete() {
        return Config.get("app.package");
    }

    public static void cerrar() {
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                driver = null;
            }
        }
    }
}
