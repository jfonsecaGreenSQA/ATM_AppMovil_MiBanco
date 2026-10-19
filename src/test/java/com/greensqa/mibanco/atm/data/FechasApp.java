package com.greensqa.mibanco.atm.data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fechas de los movimientos como las muestra la app (observado el 19/10/2026): en la lista "13 Oct.. 2026 - 08:06 p.m"
 * y en el detalle de la transaccion "21. Sept. 2026 - 10:47 a.m.".
 */
public final class FechasApp {

    /** Dia, mes abreviado, anio y hora de 12 horas; tolera los puntos sobrantes que agrega la app. */
    public static final String FORMATO = "(\\d{1,2})\\.?\\s+([A-Za-záéíóú]+)\\.*\\s+(\\d{4})\\s*-\\s*(\\d{1,2}):(\\d{2})\\s*([ap])\\.?\\s*m\\.?";
    private static final Pattern FECHA = Pattern.compile("^" + FORMATO + "$", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> MESES = Map.ofEntries(Map.entry("ene", 1), Map.entry("feb", 2),
            Map.entry("mar", 3), Map.entry("abr", 4), Map.entry("may", 5), Map.entry("jun", 6), Map.entry("jul", 7),
            Map.entry("ago", 8), Map.entry("sep", 9), Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("dic", 12));

    private FechasApp() {
    }

    /** Fecha y hora del texto; vacio si no tiene el formato de la app. */
    public static Optional<LocalDateTime> desdeApp(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        Matcher m = FECHA.matcher(texto.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        Integer mes = MESES.get(m.group(2).toLowerCase().substring(0, Math.min(3, m.group(2).length())));
        if (mes == null) {
            return Optional.empty();
        }
        int hora = Integer.parseInt(m.group(4)) % 12 + ("p".equalsIgnoreCase(m.group(6)) ? 12 : 0);
        try {
            return Optional.of(LocalDateTime.of(Integer.parseInt(m.group(3)), mes, Integer.parseInt(m.group(1)), hora,
                    Integer.parseInt(m.group(5))));
        } catch (java.time.DateTimeException e) {
            return Optional.empty();
        }
    }
}
