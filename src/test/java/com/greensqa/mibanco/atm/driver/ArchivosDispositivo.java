package com.greensqa.mibanco.atm.driver;

import java.io.IOException;
import java.util.OptionalLong;

/**
 * Archivos que la app guarda en el almacenamiento compartido del dispositivo (p. ej. el comprobante descargado). Las
 * rutas van entre comillas simples para el shell del dispositivo.
 */
public final class ArchivosDispositivo {

    private ArchivosDispositivo() {
    }

    /** Fecha de modificacion (segundos epoch) del archivo; vacio si no existe. */
    public static OptionalLong modificado(String udid, String ruta) {
        try {
            String salida = Adb.ejecutar(udid, "shell", "stat -c %Y '" + ruta + "' 2>/dev/null").trim();
            return salida.matches("\\d+") ? OptionalLong.of(Long.parseLong(salida)) : OptionalLong.empty();
        } catch (IOException e) {
            return OptionalLong.empty();
        }
    }

    public static byte[] leer(String udid, String ruta) throws IOException {
        return Adb.binario(udid, "exec-out", "cat '" + ruta + "'");
    }

    /**
     * Borra el archivo y su registro en la galeria (MediaStore): al borrar el registro Android borra tambien el archivo;
     * rm cubre el caso de un archivo sin registro.
     *
     * @return true si el archivo ya no existe
     */
    public static boolean borrar(String udid, String ruta) {
        try {
            Adb.ejecutar(udid, "shell", "content delete --uri content://media/external/file --where \"_data='" + ruta + "'\"");
            Adb.ejecutar(udid, "shell", "rm -f '" + ruta + "'");
        } catch (IOException e) {
            System.err.println("[DISPOSITIVO] No se pudo borrar " + ruta + ": " + e.getMessage());
        }
        return modificado(udid, ruta).isEmpty();
    }
}
