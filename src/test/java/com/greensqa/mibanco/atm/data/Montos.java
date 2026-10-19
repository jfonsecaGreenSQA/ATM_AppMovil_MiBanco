package com.greensqa.mibanco.atm.data;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Montos del Home ("$22.746.864,51") y del archivo de control (columna Monto). */
public final class Montos {

    /** Formato observado en la app y aceptado (control de cambios): punto de miles, coma decimal, sin espacio. */
    private static final Pattern FORMATO_APP = Pattern.compile("^\\$\\d{1,3}(\\.\\d{3})*,\\d{2}$");
    /** Separador decimal: el ultimo punto o coma seguido de 1 o 2 digitos al final. */
    private static final Pattern DECIMAL = Pattern.compile("^(.*)[.,](\\d{1,2})$");

    private Montos() {
    }

    public static boolean formatoApp(String monto) {
        return monto != null && FORMATO_APP.matcher(monto.trim()).matches();
    }

    /** Valor de un monto con el formato de la app; vacio si no cumple el formato. */
    public static Optional<BigDecimal> desdeApp(String monto) {
        if (!formatoApp(monto)) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(monto.trim().substring(1).replace(".", "").replace(',', '.')));
    }

    /** Monto de un movimiento: el formato de la app con signo menos opcional ("-$921,00", observado el 19/10/2026). */
    public static boolean formatoAppConSigno(String monto) {
        return monto != null && formatoApp(monto.trim().startsWith("-") ? monto.trim().substring(1) : monto);
    }

    /** Valor de un monto de movimiento (negativo si lleva "-"); vacio si no cumple el formato. */
    public static Optional<BigDecimal> desdeAppConSigno(String monto) {
        if (!formatoAppConSigno(monto)) {
            return Optional.empty();
        }
        String m = monto.trim();
        return m.startsWith("-") ? desdeApp(m.substring(1)).map(BigDecimal::negate) : desdeApp(m);
    }

    /**
     * Valor de la columna Monto. Acepta como lo escriba el usuario o lo entregue Excel: "22746864.51", "22746864,51",
     * "22.746.864,51", "$ 22,746,864.51" o un entero. Vacio si no es un numero.
     */
    public static Optional<BigDecimal> desdeControl(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String s = valor.replace("$", "").replace(" ", "").trim();
        if (!s.matches("[\\d.,]+") || !s.matches(".*\\d.*")) {
            return Optional.empty();
        }
        Matcher m = DECIMAL.matcher(s);
        String entero = m.matches() ? m.group(1) : s;
        String decimales = m.matches() ? m.group(2) : "0";
        return Optional.of(new BigDecimal(entero.replace(".", "").replace(",", "") + "." + decimales));
    }

    public static String texto(BigDecimal monto) {
        return monto == null ? "" : monto.setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }
}
