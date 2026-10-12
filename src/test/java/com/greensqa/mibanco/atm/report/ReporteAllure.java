package com.greensqa.mibanco.atm.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.Enmascarador;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prepara los resultados de Allure de una ejecucion y genera el reporte al final de todas las iteraciones.
 * Salidas en la carpeta de la ejecucion: allure-report/ (conserva historial) y reporte_portable/index.html
 * (un solo archivo que se abre con doble clic).
 */
public final class ReporteAllure {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> ETIQUETAS_PROPIAS = List.of("epic", "feature", "story");

    private ReporteAllure() {
    }

    /** Copia el historial del ultimo reporte para que Allure muestre la tendencia entre ejecuciones. */
    public static void prepararHistorial(Path ejecuciones, Path ejecucionActual, Path resultados) throws IOException {
        Files.createDirectories(resultados);
        Optional<Path> historial;
        try (Stream<Path> dirs = Files.list(ejecuciones)) {
            historial = dirs.filter(d -> !d.equals(ejecucionActual))
                    .map(d -> d.resolve("allure-report").resolve("history"))
                    .filter(Files::isDirectory)
                    .max(Comparator.comparing(h -> h.getParent().getParent().getFileName().toString()));
        }
        if (historial.isPresent()) {
            Path destino = resultados.resolve("history");
            Files.createDirectories(destino);
            try (Stream<Path> archivos = Files.list(historial.get())) {
                for (Path a : archivos.toList()) {
                    Files.copy(a, destino.resolve(a.getFileName()));
                }
            }
        }
    }

    public static Set<Path> archivosResultado(Path resultados) {
        try (Stream<Path> s = Files.list(resultados)) {
            return s.filter(p -> p.getFileName().toString().endsWith("-result.json")).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Cada fila del control es una prueba distinta en Allure: nombre con el IdPrueba, historyId propio
     * (si no, Allure las agruparia como reintentos del mismo escenario) y parametros de la iteracion.
     */
    public static void enriquecer(Set<Path> previos, Path resultados, Iteracion it, String dispositivo) throws IOException {
        for (Path archivo : archivosResultado(resultados)) {
            if (previos.contains(archivo)) {
                continue;
            }
            JsonObject r = GSON.fromJson(Files.readString(archivo, StandardCharsets.UTF_8), JsonObject.class);
            r.addProperty("name", nombre(it));
            r.addProperty("historyId", md5(it.caso().idAzure() + "|" + it.idPrueba()));
            JsonArray labels = r.has("labels") ? r.getAsJsonArray("labels") : new JsonArray();
            JsonArray filtradas = new JsonArray();
            labels.forEach(l -> {
                if (!ETIQUETAS_PROPIAS.contains(l.getAsJsonObject().get("name").getAsString())) {
                    filtradas.add(l);
                }
            });
            agregarEtiquetas(filtradas, it);
            r.add("labels", filtradas);
            JsonArray parametros = r.has("parameters") ? r.getAsJsonArray("parameters") : new JsonArray();
            parametros(it, dispositivo).forEach((k, v) -> parametros.add(par(k, v)));
            r.add("parameters", parametros);
            Files.writeString(archivo, GSON.toJson(r), StandardCharsets.UTF_8);
        }
    }

    /** Filas rechazadas por el motor de reglas o pendientes por detencion: quedan como omitidas, con su motivo. */
    public static void registrarNoEjecutada(Path resultados, Iteracion it) throws IOException {
        long ahora = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString();
        JsonObject r = new JsonObject();
        r.addProperty("uuid", uuid);
        // Incluye la fila: un IdPrueba duplicado no debe agruparse con la iteracion que si se ejecuto
        r.addProperty("historyId", md5("NO_EJECUTADA|" + it.fila().caso() + "|" + it.idPrueba() + "|" + it.fila().fila()));
        r.addProperty("name", nombre(it));
        r.addProperty("fullName", "Control fila " + (it.fila().fila() + 1) + " - " + nombre(it));
        r.addProperty("status", "skipped");
        r.addProperty("stage", "finished");
        r.addProperty("start", ahora);
        r.addProperty("stop", ahora);
        JsonObject detalle = new JsonObject();
        boolean pendiente = it.estado() == Iteracion.Estado.PENDIENTE;
        detalle.addProperty("message", pendiente ? it.detalle() : "No ejecutada por el motor de reglas: " + it.detalle());
        r.add("statusDetails", detalle);
        JsonArray labels = new JsonArray();
        agregarEtiquetas(labels, it);
        labels.add(etiqueta("suite", pendiente ? "Filas pendientes" : "Filas no ejecutadas"));
        labels.add(etiqueta("tag", it.estado().name()));
        r.add("labels", labels);
        JsonArray parametros = new JsonArray();
        parametros(it, "").forEach((k, v) -> parametros.add(par(k, v)));
        r.add("parameters", parametros);
        Files.writeString(resultados.resolve(uuid + "-result.json"), GSON.toJson(r), StandardCharsets.UTF_8);
    }

    public static void escribirAmbiente(Path resultados, Map<String, String> info) throws IOException {
        String ambiente = info.entrySet().stream()
                .map(e -> e.getKey().replace(" ", ".") + "=" + e.getValue().replace("\\", "/"))
                .collect(Collectors.joining("\n"));
        Files.writeString(resultados.resolve("environment.properties"), ambiente, StandardCharsets.UTF_8);

        JsonObject ejecutor = new JsonObject();
        ejecutor.addProperty("name", "GreenSQA - Orquestador ATM");
        ejecutor.addProperty("type", "local");
        ejecutor.addProperty("reportName", Config.get("reporte.nombre"));
        ejecutor.addProperty("buildName", resultados.getParent().getFileName().toString());
        Files.writeString(resultados.resolve("executor.json"), GSON.toJson(ejecutor), StandardCharsets.UTF_8);

        JsonArray categorias = new JsonArray();
        categorias.add(categoria("Indisponibilidad del sistema", List.of("skipped"), "(?s).*Indisponibilidad del sistema.*"));
        categorias.add(categoria("Pendientes por detencion del proceso", List.of("skipped"), "(?s)Pendiente:.*"));
        categorias.add(categoria("Datos de prueba no aptos", List.of("failed", "broken"), "(?s).*Dato no apto.*"));
        categorias.add(categoria("Filas no ejecutadas por el motor de reglas", List.of("skipped"), null));
        categorias.add(categoria("Defectos de producto (validacion fallida)", List.of("failed"), null));
        categorias.add(categoria("Fallas de ambiente o automatizacion", List.of("broken"), null));
        Files.writeString(resultados.resolve("categories.json"), GSON.toJson(categorias), StandardCharsets.UTF_8);
    }

    /** Genera el reporte con historial y la version portable de un solo archivo. */
    public static Path generar(Path ejecucion, Path resultados) throws IOException, InterruptedException {
        Path cli = Config.ruta("ruta.allure.cli");
        Path allure = cli.resolve("bin").resolve(windows() ? "allure.bat" : "allure");
        if (!Files.exists(allure)) {
            throw new IllegalStateException("No se encontro Allure en " + cli + ". Ejecute la opcion de prerrequisitos del menu.");
        }
        aplicarMarca(cli);
        ejecutarAllure(allure, resultados, ejecucion.resolve("allure-report"), false);
        Path portable = ejecucion.resolve("reporte_portable");
        ejecutarAllure(allure, resultados, portable, true);
        return portable.resolve("index.html");
    }

    private static void ejecutarAllure(Path allure, Path resultados, Path salida, boolean unArchivo)
            throws IOException, InterruptedException {
        List<String> cmd = new java.util.ArrayList<>(List.of(allure.toString(), "generate", resultados.toString(),
                "-o", salida.toString(), "--clean", "--name", Config.get("reporte.nombre"), "--lang", Config.get("reporte.idioma")));
        if (unArchivo) {
            cmd.add("--single-file");
        }
        if (windows()) {
            cmd.addAll(0, List.of("cmd", "/c"));
        }
        int codigo = new ProcessBuilder(cmd).inheritIO().start().waitFor();
        if (codigo != 0) {
            throw new IllegalStateException("Allure termino con codigo " + codigo + " al generar " + salida);
        }
    }

    /**
     * Logos en el encabezado del reporte (plugin custom-logo de Allure). Toma config/branding/logo_green.*
     * y config/branding/logo_cliente.* (png, svg o jpg); si no hay logos se deja el de Allure.
     */
    private static void aplicarMarca(Path cli) throws IOException {
        Path branding = Config.ruta("ruta.branding");
        Optional<String> green = imagenComoDataUri(branding, "logo_green");
        Optional<String> cliente = imagenComoDataUri(branding, "logo_cliente");
        Path yml = cli.resolve("config").resolve("allure.yml");
        String config = Files.readString(yml, StandardCharsets.UTF_8);
        String plugin = "  - custom-logo-plugin";
        if (green.isEmpty() && cliente.isEmpty()) {
            Files.writeString(yml, config.replace("\n" + plugin, ""), StandardCharsets.UTF_8);
            return;
        }
        StringBuilder fondo = new StringBuilder();
        int x = 8;
        for (Optional<String> logo : List.of(green, cliente)) {
            if (logo.isPresent()) {
                fondo.append(fondo.length() == 0 ? "" : ", ")
                        .append("url('").append(logo.get()).append("') no-repeat ").append(x).append("px center / auto 34px");
                x += 112;
            }
        }
        String css = ".side-nav__brand {\n  background: " + fondo + " !important;\n"
                + "  margin-left: 0 !important;\n  height: 56px;\n  min-width: 230px;\n}\n"
                + ".side-nav__brand-text, .side-nav__brand span { display: none !important; }\n";
        Files.writeString(cli.resolve("plugins").resolve("custom-logo-plugin").resolve("static").resolve("styles.css"),
                css, StandardCharsets.UTF_8);
        if (!config.contains("custom-logo-plugin")) {
            Files.writeString(yml, config.stripTrailing() + "\n" + plugin + "\n", StandardCharsets.UTF_8);
        }
    }

    private static Optional<String> imagenComoDataUri(Path carpeta, String base) throws IOException {
        Map<String, String> tipos = Map.of("png", "image/png", "svg", "image/svg+xml", "jpg", "image/jpeg", "jpeg", "image/jpeg");
        for (Map.Entry<String, String> t : tipos.entrySet()) {
            Path archivo = carpeta.resolve(base + "." + t.getKey());
            if (Files.exists(archivo)) {
                return Optional.of("data:" + t.getValue() + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(archivo)));
            }
        }
        return Optional.empty();
    }

    private static String nombre(Iteracion it) {
        String caso = it.caso() == null ? (it.fila().caso().isBlank() ? "(sin caso)" : it.fila().caso()) : it.caso().textoCatalogo();
        return caso + " [" + (it.idPrueba().isBlank() ? "sin IdPrueba" : it.idPrueba()) + "]";
    }

    private static void agregarEtiquetas(JsonArray labels, Iteracion it) {
        labels.add(etiqueta("epic", "App Mi Banco"));
        if (it.caso() != null) {
            labels.add(etiqueta("feature", it.caso().proceso()));
            labels.add(etiqueta("story", it.caso().textoCatalogo()));
        }
    }

    private static Map<String, String> parametros(Iteracion it, String dispositivo) {
        Map<String, String> p = new java.util.LinkedHashMap<>();
        p.put("IdPrueba", it.idPrueba());
        p.put("TipoDocumento", it.fila().tipoDocumento());
        String numero = it.numDocumento() != null ? it.numDocumento() : it.fila().numDocumento();
        p.put("NumDocumento", numero.isBlank() || numero.equalsIgnoreCase("AUTO") ? "AUTO" : Enmascarador.documento(numero));
        if (!dispositivo.isBlank()) {
            p.put("Dispositivo", dispositivo);
        }
        return p;
    }

    private static JsonObject etiqueta(String nombre, String valor) {
        return par(nombre, valor);
    }

    private static JsonObject par(String nombre, String valor) {
        JsonObject o = new JsonObject();
        o.addProperty("name", nombre);
        o.addProperty("value", valor);
        return o;
    }

    private static JsonObject categoria(String nombre, List<String> estados, String regex) {
        JsonObject c = new JsonObject();
        c.addProperty("name", nombre);
        JsonArray s = new JsonArray();
        estados.forEach(s::add);
        c.add("matchedStatuses", s);
        if (regex != null) {
            c.addProperty("messageRegex", regex);
        }
        return c;
    }

    private static String md5(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean windows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }
}
