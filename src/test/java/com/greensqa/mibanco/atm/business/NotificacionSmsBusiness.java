package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.data.Enmascarador;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.BandejaSmsScreen;
import com.greensqa.mibanco.atm.screens.BandejaSmsScreen.Sms;
import com.greensqa.mibanco.atm.screens.Pantallas;
import io.qameta.allure.Allure;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SMS que la app envia al celular del cliente. La comparacion usa solo fechas del reloj del dispositivo
 * (marca tomada antes del flujo), por eso no le afecta la diferencia de hora con el equipo.
 */
public class NotificacionSmsBusiness {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final BandejaSmsScreen bandeja = Pantallas.bandejaSms();
    private long marca = -1;

    /** Verifica que la bandeja se pueda leer y guarda la fecha del ultimo SMS antes de iniciar el flujo. */
    public void marcarInicio() {
        marca = bandeja.recibidos().stream().mapToLong(Sms::fecha).max().orElse(0);
        Allure.step("Bandeja de SMS legible; ultimo SMS previo: " + (marca == 0 ? "(bandeja vacia)" : HORA.format(Instant.ofEpochMilli(marca))));
    }

    public Optional<Sms> esperarSmsDeRegistro() {
        return esperarSms(Config.get("sms.patron.registro"));
    }

    /**
     * Espera un SMS nuevo (posterior a la marca) cuyo texto cumpla el patron indicado y no cumpla
     * sms.excluir.patron. Adjunta todos los SMS nuevos como evidencia.
     */
    private Optional<Sms> esperarSms(String patron) {
        if (marca < 0) {
            throw new IllegalStateException("No se marco la bandeja de SMS antes de iniciar el flujo");
        }
        Pattern registro = Pattern.compile(patron);
        String excluir = Config.get("sms.excluir.patron");
        Pattern exclusion = excluir.isBlank() ? null : Pattern.compile(excluir);
        Optional<Sms> encontrado;
        try {
            // FluentWait termina con cualquier valor no nulo: se devuelve null (no Optional.empty) mientras no llegue
            encontrado = Optional.of(new FluentWait<>(this)
                    .withTimeout(Config.segundos("sms.espera.segundos"))
                    .pollingEvery(Duration.ofSeconds(5))
                    .until(n -> n.nuevos().stream()
                            .filter(s -> registro.matcher(s.cuerpo()).find())
                            .filter(s -> exclusion == null || !exclusion.matcher(s.cuerpo()).find())
                            .findFirst()
                            .orElse(null)));
        } catch (org.openqa.selenium.TimeoutException e) {
            encontrado = Optional.empty();
        }
        adjuntarNuevos();
        return encontrado;
    }

    private List<Sms> nuevos() {
        return bandeja.recibidos().stream().filter(s -> s.fecha() > marca).toList();
    }

    private void adjuntarNuevos() {
        List<Sms> nuevos = nuevos();
        String contenido = nuevos.isEmpty() ? "No llegaron SMS nuevos durante la iteracion"
                : nuevos.stream().map(s -> HORA.format(Instant.ofEpochMilli(s.fecha())) + " | De: " + s.remitente() + "\n" + s.cuerpo())
                .collect(Collectors.joining("\n\n"));
        // Los codigos de verificacion (OTP reales del banco) no quedan en la evidencia
        contenido = contenido.replaceAll("(?i)(c[oó]digo[^0-9\\n]{0,60})\\d{4,8}", "$1******");
        Evidencias.texto("SMS recibidos durante la iteracion (" + nuevos.size() + ")",
                Enmascarador.texto(contenido, ContextoIteracion.actual().valoresSensibles()));
        Evidencias.captura("Estado del dispositivo al validar el SMS");
    }
}
