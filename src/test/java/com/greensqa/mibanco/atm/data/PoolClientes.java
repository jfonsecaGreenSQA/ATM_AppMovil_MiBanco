package com.greensqa.mibanco.atm.data;

import com.greensqa.mibanco.atm.config.Config;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Listado unico de clientes (datos/pool_clientes.xlsx): estado de registro y clave de cada documento.
 * El registro es unico, por eso cada cambio de estado se guarda de inmediato.
 */
public final class PoolClientes {

    public enum Estado { DISPONIBLE, EN_USO, REGISTRADO, YA_REGISTRADO_PREVIO, FALLIDO }

    /** Cliente registrado con su clave, para los casos que ingresan a la app. */
    public record ClienteRegistrado(String numero, String clave) {
    }

    private static final int TIPO = 0, NUMERO = 1, ESTADO = 3, CLAVE = 4, FECHA = 5, CASO = 6, OBS = 7, LINEA_SMS = 8;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static PoolClientes instancia;

    private final Path archivo;
    private final DataFormatter fmt = new DataFormatter();
    private XSSFWorkbook wb;
    private Sheet hoja;

    private PoolClientes(Path archivo) {
        this.archivo = archivo;
    }

    public static synchronized PoolClientes get() {
        if (instancia == null) {
            instancia = new PoolClientes(Config.ruta("ruta.pool"));
        }
        return instancia;
    }

    /**
     * Toma el primer documento DISPONIBLE del tipo indicado y lo marca EN_USO. Los clientes con LINEA_SMS = SI solo
     * se entregan a los casos de SMS: son pocos y no deben gastarse en otros registros.
     */
    public synchronized Optional<String> reservarDisponible(TipoDocumento tipo, String caso, boolean lineaSms) {
        cargar();
        for (int r = 1; r <= hoja.getLastRowNum(); r++) {
            Row row = hoja.getRow(r);
            if (row != null && tipo.name().equals(texto(row, TIPO)) && Estado.DISPONIBLE.name().equals(texto(row, ESTADO))
                    && lineaSms == tieneLineaSms(row)) {
                String numero = texto(row, NUMERO);
                escribirEstado(row, Estado.EN_USO, null, caso, "Reservado por la automatizacion");
                guardar();
                return Optional.of(numero);
            }
        }
        return Optional.empty();
    }

    /**
     * Cliente REGISTRADO con clave del tipo indicado, para ingresar. El ingreso no consume clientes: se prefieren los
     * que no tienen LINEA_SMS y, si no hay, se usa uno con la linea. No cambia su estado.
     */
    public synchronized Optional<ClienteRegistrado> registradoConClave(TipoDocumento tipo) {
        cargar();
        ClienteRegistrado conLinea = null;
        for (int r = 1; r <= hoja.getLastRowNum(); r++) {
            Row row = hoja.getRow(r);
            if (row != null && tipo.name().equals(texto(row, TIPO)) && Estado.REGISTRADO.name().equals(texto(row, ESTADO))
                    && !texto(row, CLAVE).isEmpty()) {
                ClienteRegistrado cliente = new ClienteRegistrado(texto(row, NUMERO), texto(row, CLAVE));
                if (!tieneLineaSms(row)) {
                    return Optional.of(cliente);
                }
                if (conLinea == null) {
                    conLinea = cliente;
                }
            }
        }
        return Optional.ofNullable(conLinea);
    }

    /** Clave guardada de un documento REGISTRADO. */
    public synchronized Optional<String> claveDe(TipoDocumento tipo, String numero) {
        cargar();
        return buscar(tipo, numero)
                .filter(row -> Estado.REGISTRADO.name().equals(texto(row, ESTADO)))
                .map(row -> texto(row, CLAVE))
                .filter(c -> !c.isEmpty());
    }

    /** Actualiza el estado del documento; si no existe en el listado (dato tomado del control) lo agrega. */
    public synchronized void actualizar(TipoDocumento tipo, String numero, Estado estado, String clave, String caso, String observacion) {
        cargar();
        Row row = buscar(tipo, numero).orElseGet(() -> {
            Row nueva = hoja.createRow(hoja.getLastRowNum() + 1);
            escribir(nueva, TIPO, tipo.name());
            escribir(nueva, NUMERO, numero);
            return nueva;
        });
        escribirEstado(row, estado, clave, caso, observacion);
        guardar();
    }

    /** Elimina los YA_REGISTRADO_PREVIO: no se conoce su clave y no sirven para ningun caso. */
    public synchronized int depurarYaRegistradosPrevios() {
        if (wb == null) {
            return 0;
        }
        // Compacta en una sola pasada: shiftRows por cada fila eliminada tarda minutos con un listado grande
        int ultima = hoja.getLastRowNum();
        int destino = 1;
        int eliminados = 0;
        for (int r = 1; r <= ultima; r++) {
            Row row = hoja.getRow(r);
            if (row == null) {
                continue;
            }
            if (Estado.YA_REGISTRADO_PREVIO.name().equals(texto(row, ESTADO))) {
                eliminados++;
                continue;
            }
            if (r != destino) {
                copiarFila(row, hoja.getRow(destino) == null ? hoja.createRow(destino) : hoja.getRow(destino));
            }
            destino++;
        }
        if (eliminados == 0) {
            return 0;
        }
        for (int r = ultima; r >= destino; r--) {
            Row row = hoja.getRow(r);
            if (row != null) {
                hoja.removeRow(row);
            }
        }
        guardar();
        return eliminados;
    }

    private void copiarFila(Row origen, Row destino) {
        int columnas = Math.max(origen.getLastCellNum(), destino.getLastCellNum());
        for (int c = 0; c < columnas; c++) {
            escribir(destino, c, texto(origen, c));
        }
    }

    private Optional<Row> buscar(TipoDocumento tipo, String numero) {
        for (int r = 1; r <= hoja.getLastRowNum(); r++) {
            Row row = hoja.getRow(r);
            if (row != null && numero.equals(texto(row, NUMERO)) && tipo.name().equals(texto(row, TIPO))) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    private void escribirEstado(Row row, Estado estado, String clave, String caso, String observacion) {
        escribir(row, ESTADO, estado.name());
        if (clave != null) {
            escribir(row, CLAVE, clave);
        }
        escribir(row, FECHA, LocalDateTime.now().format(FMT));
        escribir(row, CASO, caso == null ? "" : caso);
        escribir(row, OBS, observacion == null ? "" : observacion);
    }

    private void cargar() {
        if (wb != null) {
            return;
        }
        try (InputStream in = Files.newInputStream(archivo)) {
            wb = new XSSFWorkbook(in);
            hoja = wb.getSheet("Clientes");
            if (hoja == null) {
                throw new IllegalStateException("El listado de clientes no tiene la hoja 'Clientes'");
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el listado de clientes " + archivo, e);
        }
    }

    private void guardar() {
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporal)) {
                wb.write(out);
            }
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar " + archivo.getFileName()
                    + ". Verifique que el archivo no este abierto en Excel.", e);
        }
    }

    private boolean tieneLineaSms(Row row) {
        return "SI".equalsIgnoreCase(texto(row, LINEA_SMS));
    }

    private String texto(Row row, int col) {
        Cell c = row.getCell(col);
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }

    /**
     * Recrea la celda como texto: el listado viene con celdas inlineStr (openpyxl) y POI, al asignar un valor
     * sobre ellas, conserva el texto anterior en el XML y otros lectores siguen viendo el valor viejo.
     */
    private static void escribir(Row row, int col, String valor) {
        Cell anterior = row.getCell(col);
        CellStyle estilo = anterior == null ? null : anterior.getCellStyle();
        if (anterior != null) {
            row.removeCell(anterior);
        }
        Cell nueva = row.createCell(col, CellType.STRING);
        if (estilo != null) {
            nueva.setCellStyle(estilo);
        }
        nueva.setCellValue(valor);
    }
}
