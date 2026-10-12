package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.data.TipoDocumento;
import com.greensqa.mibanco.atm.screens.RecuperarClaveScreen;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Recuperacion de clave, paso del documento. Selectores observados en el S23 (Android 16) el 2026-10-12.
 * La pagina reutiliza los id del registro (kc-register-form, partyIdentification*): se reconoce por su titulo, que no
 * tiene id (Test IDs solicitados: recuperar_clave_form, recuperar_clave_title, recuperar_clave_continuar_button).
 */
public class RecuperarClaveAndroid extends PantallaAndroid implements RecuperarClaveScreen {

    private static final By TITULO = uiSelector(
            "new UiSelector().className(\"android.widget.TextView\").textStartsWith(\"Ingresa los siguientes datos para recuperar\")");
    private static final By FORMULARIO = rid("kc-register-form");
    private static final By TIPO_DOCUMENTO = rid("partyIdentificationType");
    private static final By NUMERO_DOCUMENTO = rid("partyIdentificationValue");
    private static final By CONTINUAR = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"Continuar\")");
    // Modal de mensajes de la app, comun a las paginas de Keycloak (no observado aun en esta pagina)
    private static final By AVISO_TITULO = rid("mb-message-modal-title");
    private static final By AVISO_DESCRIPCION = rid("mb-message-modal-desc");

    @Override
    public boolean estaVisible(Duration espera) {
        return visibleEn(TITULO, espera) && visibleEn(FORMULARIO, Duration.ofSeconds(5));
    }

    @Override
    public void seleccionarTipoDocumento(TipoDocumento tipo) {
        tocar(TIPO_DOCUMENTO);
        // Igual que en el registro, el <select> HTML se muestra como dialogo nativo de Android
        tocar(uiSelector("new UiSelector().resourceId(\"android:id/text1\").text(\"" + tipo.textoEnApp() + "\")"));
    }

    @Override
    public String tipoDocumentoSeleccionado() {
        return esperarVisible(TIPO_DOCUMENTO).getText();
    }

    @Override
    public void ingresarNumeroDocumento(String numero) {
        escribir(NUMERO_DOCUMENTO, numero);
        cerrarTecladoTocando(TITULO);
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
    public String textoAviso() {
        String titulo = esperarVisible(AVISO_TITULO).getText();
        String descripcion = presente(AVISO_DESCRIPCION) ? driver().findElement(AVISO_DESCRIPCION).getText() : "";
        return (titulo + ". " + descripcion).trim();
    }
}
