package com.greensqa.mibanco.atm.evidence;

import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.data.Enmascarador;
import com.greensqa.mibanco.atm.driver.Adb;
import com.greensqa.mibanco.atm.driver.DriverManager;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.logging.LogEntry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Evidencia en todo momento (no solo en fallo): cada archivo se guarda en la carpeta de la iteracion
 * y se adjunta al reporte Allure. Un error al tomar evidencia se registra pero no falla la prueba.
 */
public final class Evidencias {

    private static int consecutivo;

    private Evidencias() {
    }

    public static void reiniciarConsecutivo() {
        consecutivo = 0;
    }

    public static void captura(String nombre) {
        if (!DriverManager.activo()) {
            return;
        }
        try {
            byte[] png = DriverManager.driver().getScreenshotAs(OutputType.BYTES);
            adjuntar(nombre, "image/png", "png", png);
        } catch (RuntimeException e) {
            aviso("captura", nombre, e);
        }
    }

    public static void codigoFuente(String nombre) {
        if (!DriverManager.activo()) {
            return;
        }
        try {
            String xml = Enmascarador.texto(DriverManager.driver().getPageSource(),
                    ContextoIteracion.actual().valoresSensibles());
            adjuntar(nombre + " (UI)", "text/xml", "xml", xml.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            aviso("codigo fuente", nombre, e);
        }
    }

    public static void texto(String nombre, String contenido) {
        adjuntar(nombre, "text/plain", "txt", contenido.getBytes(StandardCharsets.UTF_8));
    }

    public static void iniciarVideo() {
        try {
            DriverManager.driver().startRecordingScreen(new AndroidStartScreenRecordingOptions()
                    .withTimeLimit(Duration.ofMinutes(3)));
        } catch (RuntimeException e) {
            aviso("video", "inicio", e);
        }
    }

    public static void detenerVideo() {
        if (!DriverManager.activo()) {
            return;
        }
        try {
            String base64 = DriverManager.driver().stopRecordingScreen();
            if (base64 != null && !base64.isBlank()) {
                adjuntar("Video de la iteracion", "video/mp4", "mp4", Base64.getMimeDecoder().decode(base64));
            }
        } catch (RuntimeException e) {
            aviso("video", "fin", e);
        }
    }

    public static void limpiarLogcat(String udid) {
        adb(udid, "logcat", "-c");
    }

    public static void logcat(String udid) {
        String log = adb(udid, "logcat", "-d", "-v", "time");
        if (!log.isBlank()) {
            texto("Logcat del dispositivo", Enmascarador.texto(log, ContextoIteracion.actual().valoresSensibles()));
        }
    }

    public static String versionApp(String udid, String paquete) {
        return adb(udid, "shell", "dumpsys", "package", paquete).lines().map(String::trim)
                .filter(l -> l.startsWith("versionName=")).findFirst()
                .map(l -> l.substring("versionName=".length())).orElse("");
    }

    /** Requiere iniciar Appium con --allow-insecure "*:get_server_logs" (lo hace el menu .bat). */
    public static void logServidorAppium() {
        if (!DriverManager.activo()) {
            return;
        }
        try {
            String log = DriverManager.driver().manage().logs().get("server").getAll().stream()
                    .map(LogEntry::toString).collect(Collectors.joining("\n"));
            texto("Log del servidor Appium", Enmascarador.texto(log, ContextoIteracion.actual().valoresSensibles()));
        } catch (RuntimeException e) {
            aviso("log Appium", "servidor", e);
        }
    }

    private static void adjuntar(String nombre, String tipo, String extension, byte[] contenido) {
        Allure.addAttachment(nombre, tipo, new ByteArrayInputStream(contenido), extension);
        Path carpeta = ContextoIteracion.actual().carpetaEvidencias();
        if (carpeta == null) {
            return;
        }
        try {
            Files.createDirectories(carpeta);
            String archivo = String.format("%03d_%s.%s", ++consecutivo, nombreArchivo(nombre), extension);
            Files.write(carpeta.resolve(archivo), contenido);
        } catch (IOException e) {
            System.err.println("[EVIDENCIA] No se pudo guardar " + nombre + ": " + e.getMessage());
        }
    }

    private static String nombreArchivo(String nombre) {
        String limpio = nombre.replaceAll("[^\\p{L}\\p{N}]+", "_").replaceAll("^_|_$", "");
        return limpio.length() > 70 ? limpio.substring(0, 70) : limpio;
    }

    private static String adb(String udid, String... argumentos) {
        try {
            return Adb.ejecutar(udid, argumentos);
        } catch (IOException e) {
            aviso("adb", String.join(" ", argumentos), e);
            return "";
        }
    }

    private static void aviso(String tipo, String nombre, Exception e) {
        System.err.println("[EVIDENCIA] No se pudo tomar " + tipo + " '" + nombre + "': " + e.getMessage());
    }
}
