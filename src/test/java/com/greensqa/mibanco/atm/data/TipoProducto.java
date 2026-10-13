package com.greensqa.mibanco.atm.data;

import com.greensqa.mibanco.atm.screens.ProductoHome;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Productos que muestra el Home. Los nombres siguen lo observado en la app (control de cambios del 12/10/2026):
 * el caso 38707 habla de "Libre inversion" pero la app muestra esos creditos como "Prestamos".
 */
public enum TipoProducto {

    AHORRO("Cuenta de ahorro", ProductoHome.Seccion.CUENTAS, "cuenta de ahorro", "cuentas de ahorro", "cuenta ahorros"),
    CORRIENTE("Cuenta corriente", ProductoHome.Seccion.CUENTAS, "cuenta corriente", "cuentas corriente"),
    PRESTAMO("Prestamo (libre inversion)", ProductoHome.Seccion.CREDITOS, "prestamos", "prestamo", "libre inversion"),
    ROTATIVO("Credito rotativo", ProductoHome.Seccion.CREDITOS, "credito rotativo"),
    PRE_APROBADO("Credito pre-aprobado", ProductoHome.Seccion.BANNER, "tienes un credito pre-aprobado");

    private final String descripcion;
    private final ProductoHome.Seccion seccion;
    private final String[] titulos;

    TipoProducto(String descripcion, ProductoHome.Seccion seccion, String... titulos) {
        this.descripcion = descripcion;
        this.seccion = seccion;
        this.titulos = titulos;
    }

    public String descripcion() {
        return descripcion;
    }

    public ProductoHome.Seccion seccion() {
        return seccion;
    }

    /** Tipo de un producto del Home segun su seccion y el inicio de su titulo (sin tildes ni mayusculas). */
    public static Optional<TipoProducto> de(ProductoHome producto) {
        String titulo = normalizar(producto.titulo());
        return Arrays.stream(values())
                .filter(t -> t.seccion == producto.seccion())
                .filter(t -> Arrays.stream(t.titulos).anyMatch(titulo::startsWith))
                .findFirst();
    }

    public static Optional<TipoProducto> desde(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(t -> t.name().equalsIgnoreCase(valor.trim())).findFirst();
    }

    private static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}
