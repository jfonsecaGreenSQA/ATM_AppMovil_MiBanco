package com.greensqa.mibanco.atm.screens;

/**
 * Producto tal como lo muestra el Home, sin interpretar.
 *
 * @param seccion       CUENTAS ("Tus cuentas"), CREDITOS ("Tus creditos") o BANNER (credito pre-aprobado)
 * @param titulo        p. ej. "Cuentas de ahorro **** 1234", "Prestamos", "Credito Rotativo"
 * @param etiqueta      texto entre el titulo y el monto: nombre de la cuenta o "Cupo aprobado"; vacio si no hay
 * @param monto         monto como se muestra ("$22.746.864,51"); vacio si el producto no muestra monto
 * @param estado        texto despues del monto, p. ej. "Cuota vencio 1 Jun"; vacio si no hay
 * @param navegable     la tarjeta se puede tocar para ver el detalle
 * @param ocultarMontos la tarjeta tiene el boton para ocultar montos (ojo)
 */
public record ProductoHome(Seccion seccion, String titulo, String etiqueta, String monto, String estado,
                           boolean navegable, boolean ocultarMontos) {

    public enum Seccion { CUENTAS, CREDITOS, BANNER }
}
