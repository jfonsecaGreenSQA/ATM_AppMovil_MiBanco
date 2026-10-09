package com.greensqa.mibanco.atm.control;

import java.util.Arrays;
import java.util.Optional;

/**
 * Catalogo de casos construidos. Cada caso se enlaza al escenario Gherkin por su tag (@IdAzure).
 * Al publicar un caso nuevo se agrega aqui y en la hoja Catalogo del archivo de control.
 */
public enum CasoPrueba {

    REGISTRO_EXITOSO_CC("38498", "Registro exitoso con CC", "Registro");

    private final String idAzure;
    private final String nombre;
    private final String proceso;

    CasoPrueba(String idAzure, String nombre, String proceso) {
        this.idAzure = idAzure;
        this.nombre = nombre;
        this.proceso = proceso;
    }

    public String idAzure() {
        return idAzure;
    }

    public String nombre() {
        return nombre;
    }

    public String proceso() {
        return proceso;
    }

    public String tag() {
        return "@" + idAzure;
    }

    /** Texto con el que aparece en la lista desplegable de la columna Caso. */
    public String textoCatalogo() {
        return idAzure + " - " + nombre;
    }

    /** Reconoce el caso por el Id de Azure al inicio del texto ("38498 - ..."). */
    public static Optional<CasoPrueba> desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String id = texto.trim().split("\\s*-\\s*", 2)[0].trim();
        return Arrays.stream(values()).filter(c -> c.idAzure.equals(id)).findFirst();
    }
}
