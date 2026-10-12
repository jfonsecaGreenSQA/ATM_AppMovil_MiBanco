package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.driver.Adb;
import com.greensqa.mibanco.atm.driver.DriverManager;
import com.greensqa.mibanco.atm.screens.BandejaSmsScreen;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Lee la bandeja con "mobile: listSms" de UiAutomator2. El driver la consulta por medio de la app Appium Settings
 * (io.appium.settings), que necesita el permiso READ_SMS: Appium lo concede al instalarla, pero algunos fabricantes
 * lo bloquean y hay que concederlo a mano.
 */
public class BandejaSmsAndroid extends PantallaAndroid implements BandejaSmsScreen {

    private static final String APP_SETTINGS = "io.appium.settings";
    private static final int MAXIMO = 50;

    @Override
    public List<Sms> recibidos() {
        try {
            return consultar();
        } catch (WebDriverException primerIntento) {
            // Sin permiso READ_SMS se intenta concederlo una vez antes de fallar
            concederPermiso();
            try {
                return consultar();
            } catch (WebDriverException e) {
                throw new IllegalStateException("No se pudo leer la bandeja de SMS del dispositivo. Conceda el permiso"
                        + " de SMS a la app 'Appium Settings' en Ajustes > Aplicaciones del telefono y vuelva a ejecutar."
                        + " Detalle: " + e.getMessage().lines().findFirst().orElse(""), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Sms> consultar() {
        Map<String, Object> respuesta = (Map<String, Object>) driver().executeScript("mobile: listSms", Map.of("max", MAXIMO));
        List<Map<String, Object>> items = (List<Map<String, Object>>) respuesta.getOrDefault("items", List.of());
        return items.stream()
                .map(i -> new Sms(texto(i.get("address")), new BigDecimal(texto(i.get("date"))).longValue(), texto(i.get("body"))))
                .toList();
    }

    private void concederPermiso() {
        try {
            Adb.ejecutar(DriverManager.udid(), "shell", "pm", "grant", APP_SETTINGS, "android.permission.READ_SMS");
        } catch (IOException e) {
            System.err.println("[SMS] No se pudo conceder READ_SMS a " + APP_SETTINGS + ": " + e.getMessage());
        }
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }
}
