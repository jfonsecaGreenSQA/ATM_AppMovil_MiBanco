package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.OtpScreen;
import org.openqa.selenium.By;

import java.time.Duration;

public class OtpAndroid extends PantallaAndroid implements OtpScreen {

    private static final By TITULO = rid("otp-title");
    // Un solo campo detras de las 6 casillas visibles
    private static final By CODIGO = rid("otp");
    private static final By VALIDAR = rid("kc-login");

    @Override
    public boolean estaVisible() {
        return presente(TITULO);
    }

    @Override
    public void ingresarCodigo(String codigo) {
        escribirConTeclas(CODIGO, codigo);
    }

    @Override
    public void validar() {
        // En el emulador Validar sigue visible con el teclado abierto; en el S23 el teclado lo tapa. Se cierra
        // tocando el titulo (texto sin accion): la franja del encabezado tocaria la hoja del OTP
        if (!visibleEn(VALIDAR, Duration.ofSeconds(2))) {
            cerrarTecladoTocando(TITULO);
        }
        tocar(VALIDAR);
    }
}
