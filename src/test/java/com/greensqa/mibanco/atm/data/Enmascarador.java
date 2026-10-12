package com.greensqa.mibanco.atm.data;

import java.util.Collection;

/** Enmascara datos sensibles en adjuntos de texto (las capturas y videos muestran la pantalla tal cual). */
public final class Enmascarador {

    private Enmascarador() {
    }

    /** 96***04 */
    public static String documento(String numero) {
        if (numero == null || numero.length() < 5) {
            return "****";
        }
        return numero.substring(0, 2) + "*".repeat(numero.length() - 4) + numero.substring(numero.length() - 2);
    }

    public static String texto(String texto, Collection<String> sensibles) {
        if (texto == null) {
            return "";
        }
        String r = texto;
        for (String s : sensibles) {
            if (s != null && s.length() >= 4) {
                r = r.replace(s, "#".repeat(s.length()));
            }
        }
        // ultimos digitos del celular que muestra la pantalla de OTP ("*** *** 1234")
        return r.replaceAll("(\\*\\*\\* \\*\\*\\* )\\d{4}", "$1####");
    }
}
