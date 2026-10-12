package com.greensqa.mibanco.atm.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

/** Lee config/ambiente.properties; una propiedad de sistema (-Dclave=valor) tiene prioridad. */
public final class Config {

    private static final Properties PROPIEDADES = cargar();

    private Config() {
    }

    private static Properties cargar() {
        Properties p = new Properties();
        try (InputStream in = Config.class.getResourceAsStream("/config/ambiente.properties")) {
            if (in == null) {
                throw new IllegalStateException("No se encontro config/ambiente.properties en el classpath");
            }
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer config/ambiente.properties", e);
        }
        return p;
    }

    public static String get(String clave) {
        String valor = System.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            valor = PROPIEDADES.getProperty(clave, "");
        }
        return valor.trim();
    }

    public static int entero(String clave) {
        return Integer.parseInt(get(clave));
    }

    public static Duration segundos(String clave) {
        return Duration.ofSeconds(entero(clave));
    }

    public static Path ruta(String clave) {
        return Paths.get(get(clave)).toAbsolutePath().normalize();
    }
}
