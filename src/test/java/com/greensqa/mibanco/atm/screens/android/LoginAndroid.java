package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.data.TipoDocumento;
import com.greensqa.mibanco.atm.screens.LoginScreen;
import org.openqa.selenium.By;

/** Formulario de login (Keycloak). Selectores observados en el page source del Samsung S23 (Android 16). */
public class LoginAndroid extends PantallaAndroid implements LoginScreen {

    private static final By FORMULARIO = rid("kc-form-login");
    private static final By TIPO_DOCUMENTO = rid("documentType");
    private static final By NUMERO_DOCUMENTO = rid("username");
    private static final By CLAVE = rid("password");
    private static final By INGRESAR = rid("kc-login");
    private static final By AVISO_TITULO = rid("mb-message-modal-title");
    private static final By AVISO_DESCRIPCION = rid("mb-message-modal-desc");
    // Franja superior sobre el formulario, sin accion aun con el formulario desplazado por el teclado
    private static final double ZONA_SIN_ACCION = 0.06;

    @Override
    public void esperarVisible() {
        esperarVisible(FORMULARIO);
    }

    @Override
    public void seleccionarTipoDocumento(TipoDocumento tipo) {
        tocar(TIPO_DOCUMENTO);
        // Igual que en el registro, el selector se muestra como dialogo nativo de Android
        tocar(uiSelector("new UiSelector().resourceId(\"android:id/text1\").text(\"" + tipo.textoEnApp() + "\")"));
    }

    @Override
    public String tipoDocumentoSeleccionado() {
        return esperarVisible(TIPO_DOCUMENTO).getText();
    }

    /** Cierra el teclado al terminar: abierto, tapa el campo de la clave y UiAutomator2 no lo encuentra. */
    @Override
    public void ingresarNumeroDocumento(String numero) {
        escribir(NUMERO_DOCUMENTO, numero);
        ocultarTeclado(ZONA_SIN_ACCION);
    }

    @Override
    public void ingresarClave(String clave) {
        escribir(CLAVE, clave);
        ocultarTeclado(ZONA_SIN_ACCION);
    }

    @Override
    public void ingresar() {
        tocar(INGRESAR);
    }

    @Override
    public boolean hayAviso() {
        return presente(AVISO_TITULO);
    }

    @Override
    public String textoAviso() {
        String titulo = esperarVisible(AVISO_TITULO).getText();
        String descripcion = presente(AVISO_DESCRIPCION) ? driver().findElement(AVISO_DESCRIPCION).getText() : "";
        return (titulo + ". " + descripcion).trim();
    }
}
