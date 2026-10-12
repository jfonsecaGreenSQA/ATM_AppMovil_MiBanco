package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.IndisponibilidadScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.Optional;

public class IndisponibilidadAndroid extends PantallaAndroid implements IndisponibilidadScreen {

    private static final By TITULO = rid("mb-message-modal-title");
    private static final By DESCRIPCION = rid("mb-message-modal-desc");
    private static final By ENTIENDO = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"Entiendo\")");
    private static final String TITULO_INDISPONIBILIDAD = "Validación fallida";
    private static final String TEXTO_INDISPONIBILIDAD = "No es posible atender tu solicitud por este canal";

    @Override
    public Optional<String> avisoVisible() {
        try {
            String titulo = textoDe(TITULO);
            String descripcion = textoDe(DESCRIPCION);
            if (titulo.startsWith(TITULO_INDISPONIBILIDAD) || descripcion.contains(TEXTO_INDISPONIBILIDAD)) {
                return Optional.of((titulo + ". " + descripcion).trim());
            }
        } catch (WebDriverException e) {
            // Sin sesion o pantalla cambiando: no se puede afirmar indisponibilidad
        }
        return Optional.empty();
    }

    @Override
    public void cerrarAviso() {
        tocar(ENTIENDO);
        new WebDriverWait(driver(), espera()).until(ExpectedConditions.invisibilityOfElementLocated(TITULO));
    }

    private String textoDe(By by) {
        List<WebElement> e = driver().findElements(by);
        return e.isEmpty() ? "" : e.get(0).getText().trim();
    }
}
