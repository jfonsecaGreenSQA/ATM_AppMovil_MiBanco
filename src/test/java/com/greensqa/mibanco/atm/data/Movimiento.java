package com.greensqa.mibanco.atm.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un movimiento de la lista del detalle de cuenta. La app lo muestra como un boton con descripcion, fecha y monto
 * ("GMF PAGO DE PRESTAMO NRO.220013176249 13 Oct.. 2026 - 08:06 p.m -$921,00", observado el 19/10/2026).
 *
 * @param texto       texto completo del boton, para volver a ubicarlo
 * @param descripcion tipo o descripcion de la transaccion
 * @param fecha       fecha y hora
 * @param monto       monto como se muestra ("-$921,00")
 * @param valor       monto con signo
 */
public record Movimiento(String texto, String descripcion, LocalDateTime fecha, String monto, BigDecimal valor) {

    private static final Pattern MOVIMIENTO = Pattern.compile("^(.+?)\\s+(" + FechasApp.FORMATO + ")\\s+(-?\\$[\\d.]+,\\d{2})$",
            Pattern.CASE_INSENSITIVE);

    /** Vacio si el texto no tiene el formato de un movimiento. */
    public static Optional<Movimiento> desdeApp(String texto) {
        Matcher m = MOVIMIENTO.matcher(texto.trim().replaceAll("\\s+", " "));
        if (!m.matches()) {
            return Optional.empty();
        }
        String monto = m.group(m.groupCount());
        Optional<LocalDateTime> fecha = FechasApp.desdeApp(m.group(2));
        Optional<BigDecimal> valor = Montos.desdeAppConSigno(monto);
        if (fecha.isEmpty() || valor.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Movimiento(texto, m.group(1).trim(), fecha.get(), monto, valor.get()));
    }
}
