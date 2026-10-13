package com.greensqa.mibanco.atm.control;

/** Una fila de la hoja Control tal como la escribio el usuario. {@code fila} es el indice 0-based en la hoja. */
public record FilaControl(int fila,
                          String caso,
                          String idPrueba,
                          String ejecutar,
                          String tipoDocumento,
                          String numDocumento,
                          String clave,
                          String otp,
                          String tipoProducto,
                          String monto,
                          String observaciones) {
}
