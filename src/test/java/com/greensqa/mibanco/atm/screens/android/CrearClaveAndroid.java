package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.CrearClaveScreen;
import org.openqa.selenium.By;

import java.time.Duration;

public class CrearClaveAndroid extends PantallaAndroid implements CrearClaveScreen {

    private static final By FORMULARIO = rid("kc-passwd-update-form");
    private static final By CLAVE = rid("password-new");
    private static final By CONFIRMACION = rid("password-confirm");
    private static final By REGLAMENTO = uiSelector(
            "new UiSelector().className(\"android.widget.CheckBox\").textStartsWith(\"Acepto el reglamento\")");
    private static final By TRATAMIENTO_DATOS = uiSelector(
            "new UiSelector().className(\"android.widget.CheckBox\").textStartsWith(\"He leído y autorizo\")");
    private static final By CONTINUAR = rid("submit-password");

    @Override
    public boolean estaVisible(Duration espera) {
        return visibleEn(FORMULARIO, espera);
    }

    @Override
    public void ingresarClave(String clave) {
        escribir(CLAVE, clave);
    }

    @Override
    public void confirmarClave(String clave) {
        escribir(CONFIRMACION, clave);
        ocultarTeclado();
    }

    @Override
    public void aceptarReglamentoDeUso() {
        tocarCasilla(REGLAMENTO);
    }

    @Override
    public void aceptarTratamientoDeDatos() {
        tocarCasilla(TRATAMIENTO_DATOS);
    }

    @Override
    public void continuar() {
        asegurarEnPantalla(CONTINUAR);
        tocar(CONTINUAR);
    }
}
