package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.OtpScreen;
import org.openqa.selenium.By;

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
        // Con el teclado abierto Validar sigue visible; ocultarlo aqui tocaria la hoja del OTP
        tocar(VALIDAR);
    }
}
