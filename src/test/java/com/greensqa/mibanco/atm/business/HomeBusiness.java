package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.HomeScreen;
import com.greensqa.mibanco.atm.screens.ObservacionCarga;
import com.greensqa.mibanco.atm.screens.Pantallas;
import io.qameta.allure.Allure;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Encabezado del Home: saludo, pregunta de actividad y ultimo ingreso. Sin selectores ni plataforma. */
public class HomeBusiness {

    /** "Hola, <nombre>": el nombre no se valida (aun no se sabe como debe llegar), solo que el saludo lo muestre. */
    private static final Pattern SALUDO = Pattern.compile("^Hola,\\s*\\S.*$");
    /** Formato observado en la app y aceptado por el equipo: DD/MM/AAAA - HH:MM:SS. */
    private static final Pattern ULTIMO_INGRESO = Pattern.compile("^Último ingreso: (\\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2})$");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu - HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private final HomeScreen home = Pantallas.home();
    private final LoginBusiness login = new LoginBusiness();

    /**
     * Lo observado en la carga del Home tras ingresar (38698).
     *
     * @param indicadoresDeCarga maximo de indicadores "Cargando..." vistos a la vez; 0 = no se vio el skeleton
     * @param montosAlInicio     los montos aparecieron sin que se viera antes el skeleton (la carga fue mas rapida)
     */
    public record CargaHome(boolean llegoAlHome, int indicadoresDeCarga, boolean montosAlInicio, boolean montosAlTerminar) {
    }

    /**
     * Ingresa y observa la carga del Home (38698). El skeleton dura unos 4 s y, mientras el Home carga, cada consulta
     * por UiAutomator tarda 1-5 s: la observacion corre en segundo plano desde antes de tocar Ingresar.
     */
    public CargaHome ingresarObservandoCarga(Iteracion it) {
        try (ObservacionCarga observacion = home.observarCarga()) {
            login.iniciarSesion(it);
            if (!home.estaVisible() && !login.homeVisible()) {
                return new CargaHome(false, observacion.maximoIndicadores(), false, false);
            }
            observacion.esperarFin(Config.segundos("espera.segundos"));
            observacion.capturaSkeleton().ifPresent(png -> Evidencias.capturaPng("Skeleton de carga del Home", png));
            Evidencias.captura("Home cargado");
            return new CargaHome(true, observacion.maximoIndicadores(), observacion.montosAntesDelSkeleton(),
                    observacion.terminoConMontos());
        }
    }

    /**
     * Precondicion de 38700: el cliente debe tener un ingreso previo para que aparezca "Ultimo ingreso". Si es su
     * primer ingreso, se cierra la sesion y se vuelve a ingresar.
     */
    public boolean asegurarIngresoPrevio(Iteracion it) {
        if (home.ultimoIngreso().isPresent()) {
            return true;
        }
        Allure.step("Primer ingreso del cliente (sin 'Ultimo ingreso'): se cierra la sesion y se vuelve a ingresar");
        login.cerrarSesion();
        login.iniciarSesion(it);
        return login.homeVisible() && home.ultimoIngreso().isPresent();
    }

    /** Se muestra el saludo "Hola, ..." con un nombre. */
    public boolean saludoVisible() {
        return SALUDO.matcher(home.saludo()).matches();
    }

    public boolean accesoSuperiorVisible(String nombre) {
        return home.accesoSuperiorVisible(nombre);
    }

    public List<String> accesosMenuInferior() {
        return home.accesosMenuInferior();
    }

    public String preguntaActividad() {
        return home.preguntaActividad();
    }

    public Optional<String> ultimoIngreso() {
        return home.ultimoIngreso();
    }

    /** Fecha y hora del ultimo ingreso si la etiqueta cumple el formato y es una fecha valida. */
    public Optional<LocalDateTime> fechaUltimoIngreso() {
        return home.ultimoIngreso().map(ULTIMO_INGRESO::matcher).filter(Matcher::matches).flatMap(m -> {
            try {
                return Optional.of(LocalDateTime.parse(m.group(1), FECHA));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        });
    }
}
