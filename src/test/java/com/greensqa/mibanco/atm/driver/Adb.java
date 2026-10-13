package com.greensqa.mibanco.atm.driver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ejecuta comandos adb contra un dispositivo; el adb del SDK tiene prioridad sobre el del PATH. */
public final class Adb {

    private Adb() {
    }

    /** @return salida estandar y de error del comando */
    public static String ejecutar(String udid, String... argumentos) throws IOException {
        String[] comando = new String[argumentos.length + 3];
        comando[0] = ruta();
        comando[1] = "-s";
        comando[2] = udid;
        System.arraycopy(argumentos, 0, comando, 3, argumentos.length);
        Process p = new ProcessBuilder(comando).redirectErrorStream(true).start();
        try (InputStream in = p.getInputStream()) {
            String salida = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor();
            return salida;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    /** Salida binaria del comando (p. ej. exec-out screencap -p); no mezcla la salida de error. */
    public static byte[] binario(String udid, String... argumentos) throws IOException {
        String[] comando = new String[argumentos.length + 3];
        comando[0] = ruta();
        comando[1] = "-s";
        comando[2] = udid;
        System.arraycopy(argumentos, 0, comando, 3, argumentos.length);
        Process p = new ProcessBuilder(comando).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try (InputStream in = p.getInputStream()) {
            byte[] salida = in.readAllBytes();
            p.waitFor();
            return salida;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new byte[0];
        }
    }

    /** adb del SDK (ANDROID_HOME, ANDROID_SDK_ROOT o la ruta por defecto de Android Studio); si no existe, el del PATH. */
    private static String ruta() {
        String ejecutable = System.getProperty("os.name").toLowerCase().contains("win") ? "adb.exe" : "adb";
        String localAppData = System.getenv("LOCALAPPDATA");
        String[] sdks = {System.getenv("ANDROID_HOME"), System.getenv("ANDROID_SDK_ROOT"),
                localAppData == null ? null : localAppData + "\\Android\\Sdk"};
        for (String sdk : sdks) {
            if (sdk != null && !sdk.isBlank()) {
                Path adb = Path.of(sdk, "platform-tools", ejecutable);
                if (Files.isRegularFile(adb)) {
                    return adb.toString();
                }
            }
        }
        return "adb";
    }
}
