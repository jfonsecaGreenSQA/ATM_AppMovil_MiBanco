package com.greensqa.mibanco.atm.runners;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.ControlReader;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.control.MotorReglas;
import com.greensqa.mibanco.atm.control.ResultadoControlWriter;
import com.greensqa.mibanco.atm.data.PoolClientes;
import com.greensqa.mibanco.atm.report.ReporteAllure;
import io.cucumber.core.cli.Main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controlador de la ejecucion: lee el archivo de control, aplica el motor de reglas, ejecuta cada fila
 * como una iteracion del escenario Cucumber del caso y, al final de todas, genera el reporte.
 * Todo queda en ejecuciones/AAAAMMDD_HHMMSS/.
 */
public final class Orquestador {

    private Orquestador() {
    }

    public static void main(String[] args) throws Exception {
        Path ejecuciones = Config.ruta("ruta.ejecuciones");
        Path ejecucion = ejecuciones.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Path resultados = ejecucion.resolve("allure-results");
        Path evidencias = ejecucion.resolve("evidencias");
        Files.createDirectories(evidencias);
        System.setProperty("allure.results.directory", resultados.toString());
        System.setProperty("cucumber.publish.quiet", "true");
        ReporteAllure.prepararHistorial(ejecuciones, ejecucion, resultados);

        Path control = Config.ruta("ruta.control");
        titulo("Ejecucion " + ejecucion.getFileName() + " | control: " + control.getFileName());
        List<Iteracion> iteraciones = new MotorReglas().evaluar(ControlReader.leer(control));

        int maximoIndisponibilidad = Config.entero("indisponibilidad.max.iteraciones");
        int canceladas = 0;
        String novedad = null;
        for (Iteracion it : iteraciones) {
            switch (it.estado()) {
                case OMITIDA -> System.out.println("[OMITIDA]      fila " + (it.fila().fila() + 1) + " " + it.idPrueba());
                case NO_EJECUTADA -> {
                    System.out.println("[NO EJECUTADA] " + it.idPrueba() + ": " + it.detalle());
                    ReporteAllure.registrarNoEjecutada(resultados, it);
                }
                default -> {
                    if (novedad != null) {
                        it.pendiente("Pendiente: proceso detenido por indisponibilidad del sistema; ejecutar en otra oportunidad");
                        System.out.println("[PENDIENTE]    " + it.idPrueba());
                        ReporteAllure.registrarNoEjecutada(resultados, it);
                        continue;
                    }
                    ejecutar(it, evidencias, resultados);
                    if (it.estado() == Iteracion.Estado.CANCELADA && ++canceladas >= maximoIndisponibilidad) {
                        novedad = "EJECUCION DETENIDA: indisponibilidad del sistema en " + canceladas
                                + " iteraciones. Las filas restantes quedan PENDIENTE para ejecutarse en otra oportunidad.";
                        titulo(novedad);
                    }
                }
            }
        }

        Path copiaControl = ResultadoControlWriter.escribir(control, ejecucion, iteraciones);
        int depurados = PoolClientes.get().depurarYaRegistradosPrevios();
        Map<String, String> ambiente = ambiente();
        if (novedad != null) {
            ambiente.put("NOVEDAD", novedad);
            Files.writeString(ejecucion.resolve("NOVEDAD_INDISPONIBILIDAD.txt"), novedad + System.lineSeparator());
        }
        ReporteAllure.escribirAmbiente(resultados, ambiente);
        Path reporte = ReporteAllure.generar(ejecucion, resultados);

        long exitosas = contar(iteraciones, Iteracion.Estado.EXITOSA);
        long fallidas = contar(iteraciones, Iteracion.Estado.FALLIDA);
        titulo("Resumen");
        System.out.println("Exitosas: " + exitosas + " | Fallidas: " + fallidas
                + " | Canceladas (indisponibilidad): " + contar(iteraciones, Iteracion.Estado.CANCELADA)
                + " | Pendientes: " + contar(iteraciones, Iteracion.Estado.PENDIENTE)
                + " | No ejecutadas: " + contar(iteraciones, Iteracion.Estado.NO_EJECUTADA)
                + " | Omitidas: " + contar(iteraciones, Iteracion.Estado.OMITIDA));
        if (novedad != null) {
            System.out.println("NOVEDAD: " + novedad);
        }
        if (depurados > 0) {
            System.out.println("Listado de clientes: se depuraron " + depurados + " documentos YA_REGISTRADO_PREVIO");
        }
        System.out.println("Carpeta de la ejecucion: " + ejecucion);
        System.out.println("Control con resultados:  " + copiaControl);
        System.out.println("Reporte:                 " + reporte);
        // 2 = detenida por indisponibilidad; 1 = hubo fallas; 0 = sin fallas
        System.exit(novedad != null ? 2 : fallidas > 0 ? 1 : 0);
    }

    private static void ejecutar(Iteracion it, Path evidencias, Path resultados) throws Exception {
        titulo("Iteracion " + it.idPrueba() + " - " + it.caso().textoCatalogo());
        it.iniciar(evidencias.resolve(it.idPrueba().replaceAll("[^A-Za-z0-9_-]", "_")));
        ContextoIteracion.iniciar(it);
        Set<Path> previos = ReporteAllure.archivosResultado(resultados);
        Main.run(new String[]{
                "--glue", "com.greensqa.mibanco.atm.steps",
                "--glue", "com.greensqa.mibanco.atm.hooks",
                "--plugin", "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
                "--plugin", "com.greensqa.mibanco.atm.hooks.ResultadoListener",
                "--plugin", "pretty",
                "--monochrome",
                "--tags", it.caso().tag(),
                "classpath:features"
        }, Thread.currentThread().getContextClassLoader());

        Boolean exitosa = ContextoIteracion.exitosa();
        if (exitosa == null) {
            it.noEjecutada("No existe escenario con el tag " + it.caso().tag());
        } else if (ContextoIteracion.indisponibilidad() != null) {
            it.cancelar(ContextoIteracion.indisponibilidad());
            ReporteAllure.enriquecer(previos, resultados, it, ContextoIteracion.infoEjecucion().getOrDefault("Dispositivo", ""));
        } else {
            it.finalizar(exitosa, exitosa ? "" : ContextoIteracion.error());
            ReporteAllure.enriquecer(previos, resultados, it, ContextoIteracion.infoEjecucion().getOrDefault("Dispositivo", ""));
        }
        System.out.println("[" + it.estado() + "] " + it.idPrueba() + (it.detalle().isBlank() ? "" : ": " + it.detalle()));
    }

    private static Map<String, String> ambiente() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("App", "Mi Banco (" + Config.get("app.package") + ")");
        info.put("Ambiente", Config.get("ambiente.nombre"));
        info.putAll(ContextoIteracion.infoEjecucion());
        info.put("Appium", Config.get("appium.url"));
        info.put("Ejecutado.por", System.getProperty("user.name"));
        info.put("Empresa", "GreenSQA");
        return info;
    }

    private static long contar(List<Iteracion> iteraciones, Iteracion.Estado estado) {
        return iteraciones.stream().filter(i -> i.estado() == estado).count();
    }

    private static void titulo(String texto) {
        System.out.println("\n==================== " + texto + " ====================");
    }
}
