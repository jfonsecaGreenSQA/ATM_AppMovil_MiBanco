package com.greensqa.mibanco.atm.control;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.data.GeneradorClave;
import com.greensqa.mibanco.atm.data.Montos;
import com.greensqa.mibanco.atm.data.TipoDocumento;
import com.greensqa.mibanco.atm.data.TipoProducto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decide que filas del control se ejecutan y resuelve sus datos.
 * Reglas generales: Ejecutar=SI, caso construido, IdPrueba unico (si viene vacio se genera).
 * Reglas por caso: campos obligatorios y formato de cada dato.
 */
public class MotorReglas {

    static final String AUTO = "AUTO";
    /** Con un documento informado la automatizacion no conoce ni inventa la clave: debe venir en el control. */
    static final String CLAVE_OBLIGATORIA = "Con NumDocumento informado (no AUTO) la Clave es obligatoria: indique ";

    public List<Iteracion> evaluar(List<FilaControl> filas) {
        Set<String> ids = new HashSet<>();
        List<Iteracion> iteraciones = new ArrayList<>();
        List<String> idsPrueba = idsPrueba(filas);
        for (int i = 0; i < filas.size(); i++) {
            FilaControl fila = filas.get(i);
            Iteracion it = new Iteracion(fila, idsPrueba.get(i));
            iteraciones.add(it);
            if (!"SI".equalsIgnoreCase(fila.ejecutar())) {
                it.omitir("Ejecutar = " + (fila.ejecutar().isBlank() ? "(vacio)" : fila.ejecutar()));
                continue;
            }
            List<String> errores = new ArrayList<>();
            Optional<CasoPrueba> caso = CasoPrueba.desdeTexto(fila.caso());
            if (caso.isEmpty()) {
                errores.add("Caso no reconocido o aun no construido: '" + fila.caso() + "'");
            }
            if (!ids.add(it.idPrueba().toUpperCase())) {
                errores.add("IdPrueba duplicado: " + it.idPrueba());
            }
            caso.ifPresent(c -> {
                it.identificarCaso(c);
                switch (c) {
                    case REGISTRO_EXITOSO_CC, REGISTRO_EXITOSO_CE, SMS_REGISTRO_EXITOSO_CC, SMS_REGISTRO_EXITOSO_CE -> reglasRegistroExitoso(fila, it, errores);
                    case LOGIN_EXITOSO_CC, LOGIN_EXITOSO_CE, HOME_SALUDO, HOME_ULTIMO_INGRESO, HOME_ACCESO_BREB, HOME_ACCESO_CREDITO,
                            HOME_MENU_INFERIOR, HOME_SKELETON, HOME_BANNER -> reglasLogin(fila, it, errores);
                    case RECUPERACION_CLAVE, RECUPERACION_CLAVE_OTP -> reglasRecuperacionClave(fila, it, errores);
                    case HOME_PRODUCTO_AHORRO, HOME_PRODUCTO_CORRIENTE, HOME_PRODUCTO_PRESTAMO, HOME_PRODUCTO_ROTATIVO,
                            HOME_PRODUCTO_PRE_APROBADO, HOME_SALDOS, CUENTAS_PRODUCTO_AHORRO, CUENTAS_PRODUCTO_CORRIENTE,
                            CUENTAS_PRODUCTO_PRESTAMO, CUENTAS_PRODUCTO_ROTATIVO, CUENTAS_SALDOS -> reglasProducto(fila, it, errores);
                    case CUENTAS_DETALLE, CUENTAS_DETALLE_TRANSACCION, CUENTAS_DESCARGAR_COMPROBANTE, CUENTAS_COMPARTIR_COMPROBANTE,
                            CUENTAS_MOVIMIENTOS_MAXIMO_20, CUENTAS_MOVIMIENTOS_SIN_VER_MAS, CUENTAS_MOVIMIENTOS_SIN_PAGINACION,
                            CUENTAS_MOVIMIENTOS_ULTIMO_MES ->
                            reglasDetalleCuenta(fila, it, errores);
                }
                if (!c.deProductos() && !(fila.tipoProducto().isBlank() && fila.monto().isBlank())) {
                    errores.add("TipoProducto y Monto solo aplican a los casos de productos del Home; deje vacias las columnas");
                }
            });
            if (errores.isEmpty()) {
                it.aprobar(caso.get());
            } else {
                it.rechazar(String.join("; ", errores));
            }
        }
        return iteraciones;
    }

    /**
     * IdPrueba de cada fila: el escrito en el control o, si esta vacio, el prefijo del caso y su consecutivo entre las
     * filas de ese caso (p. ej. REG-CC-001, CTA-MOV20-002). Un generado nunca repite uno escrito a mano.
     */
    static List<String> idsPrueba(List<FilaControl> filas) {
        Set<String> escritos = new HashSet<>();
        filas.stream().map(FilaControl::idPrueba).filter(s -> !s.isBlank()).forEach(s -> escritos.add(s.toUpperCase()));
        Map<String, Integer> contador = new HashMap<>();
        List<String> ids = new ArrayList<>();
        for (FilaControl fila : filas) {
            if (!fila.idPrueba().isBlank()) {
                ids.add(fila.idPrueba());
                continue;
            }
            String base = CasoPrueba.desdeTexto(fila.caso()).map(CasoPrueba::prefijoIdPrueba).orElse("FILA");
            String id;
            do {
                id = String.format("%s-%03d", base, contador.merge(base, 1, Integer::sum));
            } while (escritos.contains(id.toUpperCase()));
            ids.add(id);
        }
        return ids;
    }

    private void reglasRegistroExitoso(FilaControl fila, Iteracion it, List<String> errores) {
        boolean auto = reglasDocumento(fila, it, errores);

        if (fila.clave().isBlank() && !auto) {
            errores.add(CLAVE_OBLIGATORIA + "la clave que se crea en el registro");
        } else if (fila.clave().isBlank()) {
            it.datosClave(GeneradorClave.generar(), "generada segun RN-R07");
        } else if (!GeneradorClave.cumpleRnR07(fila.clave())) {
            errores.add("Clave no cumple RN-R07 (4 digitos, sin 3 iguales consecutivos) para un registro exitoso");
        } else {
            it.datosClave(fila.clave(), "tomada del control");
        }

        reglasOtp(fila, it, errores);
    }

    /**
     * Recuperacion de clave de un cliente registrado. NumDocumento AUTO (o vacio) toma un cliente REGISTRADO con clave
     * del listado. Clave es la nueva: con AUTO, vacia se genera al ejecutar (RN-R07 y distinta de la actual); con un
     * documento informado es obligatoria. Si se informa debe cumplir RN-R07.
     */
    private void reglasRecuperacionClave(FilaControl fila, Iteracion it, List<String> errores) {
        boolean auto = reglasDocumento(fila, it, errores);

        if (fila.clave().isBlank() && !auto) {
            errores.add(CLAVE_OBLIGATORIA + "la clave nueva que queda tras la recuperacion");
        } else if (fila.clave().isBlank()) {
            it.datosClave(null, "nueva, generada al ejecutar segun RN-R07 y distinta de la actual");
        } else if (!GeneradorClave.cumpleRnR07(fila.clave())) {
            errores.add("Clave nueva no cumple RN-R07 (4 digitos, sin 3 iguales consecutivos)");
        } else {
            it.datosClave(fila.clave(), "nueva, tomada del control");
        }

        reglasOtp(fila, it, errores);
    }

    /**
     * Detalle de cuenta: ingresa como en el login con un cliente que muestre la cuenta de ahorros (con AUTO se busca en
     * el listado como en los productos del Home). TipoProducto y Monto no aplican.
     */
    private void reglasDetalleCuenta(FilaControl fila, Iteracion it, List<String> errores) {
        reglasLogin(fila, it, errores);
        it.datosProducto(TipoProducto.AHORRO, null);
    }

    /** OTP vacio = OTP de certificacion configurado (otp.defecto). */
    private void reglasOtp(FilaControl fila, Iteracion it, List<String> errores) {
        String otp = fila.otp().isBlank() ? Config.get("otp.defecto") : fila.otp();
        if (!otp.matches("\\d{6}")) {
            errores.add("OTP debe tener 6 digitos");
        }
        it.datosOtp(otp);
    }

    /**
     * Login con un cliente ya registrado. Con NumDocumento AUTO (o vacio) se toma un cliente REGISTRADO del listado
     * con su clave; con un documento informado la clave es obligatoria en el control. El login no consume clientes.
     */
    private void reglasLogin(FilaControl fila, Iteracion it, List<String> errores) {
        boolean auto = reglasDocumento(fila, it, errores);
        if (fila.clave().isBlank() && !auto) {
            errores.add(CLAVE_OBLIGATORIA + "la clave actual del cliente");
        } else if (fila.clave().isBlank()) {
            it.datosClave(null, "tomada del listado de clientes");
        } else if (auto) {
            errores.add("Con NumDocumento AUTO la clave se toma del listado de clientes: deje Clave vacia");
        } else if (!fila.clave().matches("\\d{4}")) {
            errores.add("Clave debe tener 4 digitos");
        } else {
            it.datosClave(fila.clave(), "tomada del control");
        }
    }

    /**
     * Productos del Home: ingresa como en el login y valida un producto. TipoProducto vacio = el del caso; si se
     * informa debe ser el del caso (en 38709 es obligatorio: el producto cuyo saldo se compara). Monto vacio = solo se
     * valida el formato; en 38709 es obligatorio (saldo esperado segun el backend).
     */
    private void reglasProducto(FilaControl fila, Iteracion it, List<String> errores) {
        reglasLogin(fila, it, errores);
        CasoPrueba caso = it.caso();
        Optional<TipoProducto> informado = TipoProducto.desde(fila.tipoProducto());
        if (!fila.tipoProducto().isBlank() && informado.isEmpty()) {
            errores.add("TipoProducto invalido: " + fila.tipoProducto() + " (AHORRO, CORRIENTE, PRESTAMO, ROTATIVO, PRE_APROBADO)");
        }
        Optional<TipoProducto> producto = caso.producto().or(() -> informado);
        if (caso.producto().isPresent() && informado.isPresent() && informado.get() != caso.producto().get()) {
            errores.add("El caso " + caso.idAzure() + " valida " + caso.producto().get() + "; TipoProducto es "
                    + informado.get() + " (deje TipoProducto vacio o use el caso de ese producto)");
        }
        if (producto.isEmpty() && fila.tipoProducto().isBlank()) {
            errores.add("TipoProducto es obligatorio en " + caso.idAzure() + ": indique el producto cuyo saldo se compara");
        }
        // 39565 abre el detalle de la cuenta: los creditos y el banner no tienen ese detalle
        if (caso == CasoPrueba.CUENTAS_SALDOS && informado.isPresent()
                && informado.get() != TipoProducto.AHORRO && informado.get() != TipoProducto.CORRIENTE) {
            errores.add("En " + caso.idAzure() + " TipoProducto debe ser AHORRO o CORRIENTE: el caso abre el detalle de la cuenta");
        }

        Optional<BigDecimal> monto = Montos.desdeControl(fila.monto());
        if (!fila.monto().isBlank() && monto.isEmpty()) {
            errores.add("Monto invalido: " + fila.monto() + " (numero, p. ej. 22746864.51)");
        } else if (caso.validaSaldos() && monto.isEmpty()) {
            errores.add("Monto es obligatorio en " + caso.idAzure() + ": saldo esperado del producto segun el backend");
        }
        producto.ifPresent(p -> it.datosProducto(p, monto.orElse(null)));
    }

    /** TipoDocumento (el del caso) y NumDocumento (6 a 11 digitos o AUTO). @return true si es AUTO */
    private boolean reglasDocumento(FilaControl fila, Iteracion it, List<String> errores) {
        Optional<TipoDocumento> tipo = TipoDocumento.desde(fila.tipoDocumento());
        if (fila.tipoDocumento().isBlank()) {
            errores.add("TipoDocumento es obligatorio");
        } else if (tipo.isEmpty()) {
            errores.add("TipoDocumento invalido: " + fila.tipoDocumento());
        } else if (tipo.get() != it.caso().tipoDocumento()) {
            errores.add("El caso " + it.caso().idAzure() + " es para " + it.caso().tipoDocumento() + "; TipoDocumento es "
                    + tipo.get() + " (use el caso de " + tipo.get() + " si esta construido)");
        }

        String num = fila.numDocumento();
        boolean auto = num.isBlank() || AUTO.equalsIgnoreCase(num);
        if (!auto && !num.matches("\\d{6,11}")) {
            errores.add("NumDocumento debe ser numerico de 6 a 11 digitos, o AUTO");
        }
        tipo.ifPresent(t -> it.datosDocumento(t, auto ? null : num));
        return auto;
    }
}
