package com.greensqa.mibanco.atm.screens;

/** Hoja "Queremos validar que eres tu" del componente OTP. */
public interface OtpScreen {

    boolean estaVisible();

    void ingresarCodigo(String codigo);

    void validar();

    /** Mensaje de error del codigo (p. ej. "El código ingresado no es válido"), o vacio si no hay. Sin espera. */
    String error();
}
