package com.greensqa.mibanco.atm.control;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lee la hoja Control. Las columnas se ubican por nombre de encabezado, no por posicion. */
public final class ControlReader {

    public static final String HOJA = "Control";
    static final List<String> COLUMNAS = List.of(
            "Caso", "IdPrueba", "Ejecutar", "TipoDocumento", "NumDocumento", "Clave", "OTP", "Observaciones");

    private ControlReader() {
    }

    public static List<FilaControl> leer(Path archivo) {
        try (InputStream in = Files.newInputStream(archivo); Workbook wb = WorkbookFactory.create(in)) {
            Sheet hoja = wb.getSheet(HOJA);
            if (hoja == null) {
                throw new IllegalStateException("El archivo de control no tiene la hoja '" + HOJA + "'");
            }
            Map<String, Integer> col = columnas(hoja.getRow(0));
            DataFormatter fmt = new DataFormatter();
            List<FilaControl> filas = new ArrayList<>();
            for (int r = 1; r <= hoja.getLastRowNum(); r++) {
                Row row = hoja.getRow(r);
                if (row == null) {
                    continue;
                }
                String[] v = COLUMNAS.stream().map(c -> valor(row, col.get(normalizar(c)), fmt)).toArray(String[]::new);
                if (String.join("", v).isBlank()) {
                    continue;
                }
                filas.add(new FilaControl(r, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]));
            }
            return filas;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo de control " + archivo, e);
        }
    }

    private static Map<String, Integer> columnas(Row cabecera) {
        if (cabecera == null) {
            throw new IllegalStateException("La hoja Control no tiene encabezados en la fila 1");
        }
        Map<String, Integer> col = new HashMap<>();
        for (Cell c : cabecera) {
            col.put(normalizar(c.getStringCellValue()), c.getColumnIndex());
        }
        for (String requerida : COLUMNAS) {
            if (!col.containsKey(normalizar(requerida))) {
                throw new IllegalStateException("Falta la columna '" + requerida + "' en la hoja Control");
            }
        }
        return col;
    }

    private static String valor(Row row, int indice, DataFormatter fmt) {
        Cell c = row.getCell(indice);
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }

    static String normalizar(String s) {
        return s == null ? "" : s.replace(" ", "").toUpperCase(Locale.ROOT);
    }
}
