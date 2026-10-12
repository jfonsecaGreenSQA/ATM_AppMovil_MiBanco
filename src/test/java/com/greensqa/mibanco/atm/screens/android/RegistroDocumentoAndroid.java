package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.data.TipoDocumento;
import com.greensqa.mibanco.atm.screens.RegistroDocumentoScreen;
import org.openqa.selenium.By;

public class RegistroDocumentoAndroid extends PantallaAndroid implements RegistroDocumentoScreen {

    private static final By FORMULARIO = rid("kc-register-form");
    private static final By TIPO_DOCUMENTO = rid("partyIdentificationType");
    private static final By NUMERO_DOCUMENTO = rid("partyIdentificationValue");
    private static final By CONTINUAR = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"Continuar\")");
    private static final By AVISO_TITULO = rid("mb-message-modal-title");
    private static final By AVISO_DESCRIPCION = rid("mb-message-modal-desc");
    private static final By AVISO_ENTIENDO = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"Entiendo\")");
    private static final String TITULO_YA_REGISTRADO = "Ya eres cliente del banco";

    @Override
    public void esperarVisible() {
        esperarVisible(FORMULARIO);
    }

    @Override
    public void seleccionarTipoDocumento(TipoDocumento tipo) {
        tocar(TIPO_DOCUMENTO);
        // El <select> HTML se muestra como dialogo nativo de Android
        tocar(uiSelector("new UiSelector().resourceId(\"android:id/text1\").text(\"" + tipo.textoEnApp() + "\")"));
    }

    @Override
    public String tipoDocumentoSeleccionado() {
        return esperarVisible(TIPO_DOCUMENTO).getText();
    }

    @Override
    public void ingresarNumeroDocumento(String numero) {
        escribir(NUMERO_DOCUMENTO, numero);
        ocultarTeclado();
    }

    @Override
    public void continuar() {
        tocar(CONTINUAR);
    }

    @Override
    public boolean hayAviso() {
        return presente(AVISO_TITULO);
    }

    @Override
    public boolean avisoIndicaDocumentoRegistrado() {
        return TITULO_YA_REGISTRADO.equals(esperarVisible(AVISO_TITULO).getText().trim());
    }

    @Override
    public String textoAviso() {
        String titulo = esperarVisible(AVISO_TITULO).getText();
        String descripcion = presente(AVISO_DESCRIPCION) ? driver().findElement(AVISO_DESCRIPCION).getText() : "";
        return (titulo + ". " + descripcion).trim();
    }

    @Override
    public void cerrarAviso() {
        tocar(AVISO_ENTIENDO);
    }
}
