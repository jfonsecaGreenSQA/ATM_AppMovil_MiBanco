package com.greensqa.mibanco.atm.control;

import com.greensqa.mibanco.atm.data.Enmascarador;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Deja en la carpeta de la ejecucion una copia del control con el resultado de cada fila. */
public final class ResultadoControlWriter {

    private static final String[] COLUMNAS = {"Resultado", "Detalle", "DocumentoUsado", "Inicio", "Duracion(s)", "Evidencias"};
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ResultadoControlWriter() {
    }

    public static Path escribir(Path control, Path carpetaEjecucion, List<Iteracion> iteraciones) {
        Path copia = carpetaEjecucion.resolve("Control_Resultado.xlsx");
        try (InputStream in = Files.newInputStream(control); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet hoja = wb.getSheet(ControlReader.HOJA);
            Row cabecera = hoja.getRow(0);
            int base = cabecera.getLastCellNum();
            CellStyle estiloCabecera = estiloCabecera(wb);
            for (int i = 0; i < COLUMNAS.length; i++) {
                cabecera.createCell(base + i).setCellValue(COLUMNAS[i]);
                cabecera.getCell(base + i).setCellStyle(estiloCabecera);
            }
            Map<Iteracion.Estado, CellStyle> estilos = estilosEstado(wb);
            for (Iteracion it : iteraciones) {
                Row row = hoja.getRow(it.fila().fila());
                row.createCell(base).setCellValue(it.estado().name());
                row.getCell(base).setCellStyle(estilos.get(it.estado()));
                row.createCell(base + 1).setCellValue(it.detalle());
                row.createCell(base + 2).setCellValue(it.numDocumento() == null ? "" : Enmascarador.documento(it.numDocumento()));
                row.createCell(base + 3).setCellValue(it.inicio() == null ? "" : it.inicio().format(FECHA));
                row.createCell(base + 4).setCellValue(it.duracion() == null ? "" : String.valueOf(it.duracion().toSeconds()));
                row.createCell(base + 5).setCellValue(it.carpetaEvidencias() == null ? ""
                        : carpetaEjecucion.relativize(it.carpetaEvidencias()).toString());
            }
            for (int i = 0; i < COLUMNAS.length; i++) {
                hoja.setColumnWidth(base + i, (i == 1 ? 70 : 18) * 256);
            }
            try (OutputStream out = Files.newOutputStream(copia)) {
                wb.write(out);
            }
            return copia;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir " + copia, e);
        }
    }

    private static CellStyle estiloCabecera(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static Map<Iteracion.Estado, CellStyle> estilosEstado(XSSFWorkbook wb) {
        Map<Iteracion.Estado, CellStyle> m = new EnumMap<>(Iteracion.Estado.class);
        m.put(Iteracion.Estado.EXITOSA, relleno(wb, IndexedColors.LIGHT_GREEN));
        m.put(Iteracion.Estado.FALLIDA, relleno(wb, IndexedColors.CORAL));
        m.put(Iteracion.Estado.NO_EJECUTADA, relleno(wb, IndexedColors.LIGHT_YELLOW));
        m.put(Iteracion.Estado.OMITIDA, relleno(wb, IndexedColors.GREY_25_PERCENT));
        m.put(Iteracion.Estado.CANCELADA, relleno(wb, IndexedColors.LIGHT_ORANGE));
        m.put(Iteracion.Estado.PENDIENTE, relleno(wb, IndexedColors.LIGHT_TURQUOISE));
        m.put(Iteracion.Estado.LISTA, relleno(wb, IndexedColors.WHITE));
        return m;
    }

    private static CellStyle relleno(XSSFWorkbook wb, IndexedColors color) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(color.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}
