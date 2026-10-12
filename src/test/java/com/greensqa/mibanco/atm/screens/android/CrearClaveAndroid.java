package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.CrearClaveScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;

import java.time.Duration;
import java.util.Map;

public class CrearClaveAndroid extends PantallaAndroid implements CrearClaveScreen {

    private static final By FORMULARIO = rid("kc-passwd-update-form");
    // Titulo sin id; zona sin accion para cerrar el teclado
    private static final By TITULO = uiSelector("new UiSelector().className(\"android.widget.TextView\").textStartsWith(\"Crea tu clave\")");
    private static final By CLAVE = rid("password-new");
    private static final By CONFIRMACION = rid("password-confirm");
    private static final By REGLAMENTO = uiSelector(
            "new UiSelector().className(\"android.widget.CheckBox\").textStartsWith(\"Acepto el reglamento\")");
    private static final By TRATAMIENTO_DATOS = uiSelector(
            "new UiSelector().className(\"android.widget.CheckBox\").textStartsWith(\"He leído y autorizo\")");
    // En el WebView del S23 (Android 16) las casillas no se exponen como CheckBox: solo su texto, con la casilla a la izquierda
    private static final By TEXTO_REGLAMENTO = uiSelector(
            "new UiSelector().className(\"android.widget.TextView\").textStartsWith(\"Acepto el\")");
    private static final By TEXTO_TRATAMIENTO_DATOS = uiSelector(
            "new UiSelector().className(\"android.widget.TextView\").textStartsWith(\"He leído y autorizo\")");
    private static final By CONTINUAR = rid("submit-password");

    @Override
    public boolean estaVisible(Duration espera) {
        return visibleEn(FORMULARIO, espera);
    }

    @Override
    public void ingresarClave(String clave) {
        escribir(CLAVE, clave);
        // En el S23 el teclado abierto tapa la confirmacion
        cerrarTecladoTocando(TITULO);
    }

    @Override
    public void confirmarClave(String clave) {
        escribir(CONFIRMACION, clave);
        cerrarTecladoTocando(TITULO);
    }

    @Override
    public void aceptarReglamentoDeUso() {
        marcarCasilla(REGLAMENTO, TEXTO_REGLAMENTO);
    }

    @Override
    public void aceptarTratamientoDeDatos() {
        marcarCasilla(TRATAMIENTO_DATOS, TEXTO_TRATAMIENTO_DATOS);
    }

    /**
     * Con CheckBox expuesto (emulador) toca su esquina. Sin el (S23) toca la casilla a la izquierda de la primera
     * linea del texto, lejos de los enlaces. Si no queda marcada, Continuar sigue deshabilitado y el paso falla
     * antes de registrar. Fragil ante cambios de diseno: ver solicitud de Test IDs.
     */
    private void marcarCasilla(By casilla, By texto) {
        if (!driver().findElements(casilla).isEmpty()) {
            tocarCasilla(casilla);
            return;
        }
        Rectangle r = asegurarEnPantalla(texto).getRect();
        int margen = driver().manage().window().getSize().width * 84 / 1080;
        driver().executeScript("mobile: clickGesture", Map.of("x", r.x - margen, "y", r.y + r.height / 2));
    }

    @Override
    public void continuar() {
        asegurarEnPantalla(CONTINUAR);
        tocar(CONTINUAR);
    }
}
