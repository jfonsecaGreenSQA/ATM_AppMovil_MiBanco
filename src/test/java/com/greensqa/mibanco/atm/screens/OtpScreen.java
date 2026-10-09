package com.greensqa.mibanco.atm.screens;

/** Hoja "Queremos validar que eres tu" del componente OTP. */
public interface OtpScreen {

    boolean estaVisible();

    void ingresarCodigo(String codigo);

    void validar();
}
