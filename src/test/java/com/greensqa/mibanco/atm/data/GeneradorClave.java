package com.greensqa.mibanco.atm.data;

import java.security.SecureRandom;

/** Regla RN-R07: clave de 4 digitos numericos sin 3 digitos iguales consecutivos (ej. invalidas 1113, 2333). */
public final class GeneradorClave {

    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneradorClave() {
    }

    public static boolean cumpleRnR07(String clave) {
        return clave != null && clave.matches("\\d{4}") && !clave.matches(".*(\\d)\\1\\1.*");
    }

    /** Genera una clave valida que ademas no sea una secuencia (1234 / 4321). */
    public static String generar() {
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            String clave = sb.toString();
            if (cumpleRnR07(clave) && !esSecuencia(clave)) {
                return clave;
            }
        }
    }

    private static boolean esSecuencia(String c) {
        boolean asc = true;
        boolean desc = true;
        for (int i = 1; i < c.length(); i++) {
            int d = c.charAt(i) - c.charAt(i - 1);
            asc &= d == 1;
            desc &= d == -1;
        }
        return asc || desc;
    }
}
