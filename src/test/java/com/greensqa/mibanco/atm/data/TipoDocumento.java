package com.greensqa.mibanco.atm.data;

import java.util.Arrays;
import java.util.Optional;

public enum TipoDocumento {

    CC("Cédula de ciudadanía"),
    CE("Cédula de extranjería");

    private final String textoEnApp;

    TipoDocumento(String textoEnApp) {
        this.textoEnApp = textoEnApp;
    }

    /** Texto con el que la app muestra el tipo en el selector. */
    public String textoEnApp() {
        return textoEnApp;
    }

    public static Optional<TipoDocumento> desde(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(t -> t.name().equalsIgnoreCase(valor.trim())).findFirst();
    }
}
