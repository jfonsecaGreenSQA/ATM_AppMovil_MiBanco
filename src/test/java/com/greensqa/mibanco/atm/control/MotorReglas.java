package com.greensqa.mibanco.atm.control;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.data.GeneradorClave;
import com.greensqa.mibanco.atm.data.TipoDocumento;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decide que filas del control se ejecutan y resuelve sus datos.
 * Reglas generales: Ejecutar=SI, caso construido, IdPrueba obligatorio y unico.
 * Reglas por caso: campos obligatorios y formato de cada dato.
 */
public class MotorReglas {

    static final String AUTO = "AUTO";

    public List<Iteracion> evaluar(List<FilaControl> filas) {
        Set<String> ids = new HashSet<>();
        List<Iteracion> iteraciones = new ArrayList<>();
        for (FilaControl fila : filas) {
            Iteracion it = new Iteracion(fila);
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
            if (fila.idPrueba().isBlank()) {
                errores.add("IdPrueba es obligatorio");
            } else if (!ids.add(fila.idPrueba().toUpperCase())) {
                errores.add("IdPrueba duplicado: " + fila.idPrueba());
            }
            caso.ifPresent(c -> {
                it.identificarCaso(c);
                switch (c) {
                    case REGISTRO_EXITOSO_CC, REGISTRO_EXITOSO_CE, SMS_REGISTRO_EXITOSO_CC, SMS_REGISTRO_EXITOSO_CE -> reglasRegistroExitoso(fila, it, errores);
                    case LOGIN_EXITOSO_CC, LOGIN_EXITOSO_CE, HOME_SALUDO, HOME_ULTIMO_INGRESO, HOME_ACCESO_BREB, HOME_ACCESO_CREDITO,
                            HOME_MENU_INFERIOR -> reglasLogin(fila, it, errores);
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

    private void reglasRegistroExitoso(FilaControl fila, Iteracion it, List<String> errores) {
        reglasDocumento(fila, it, errores);

        if (fila.clave().isBlank()) {
            it.datosClave(GeneradorClave.generar(), "generada segun RN-R07");
        } else if (!GeneradorClave.cumpleRnR07(fila.clave())) {
            errores.add("Clave no cumple RN-R07 (4 digitos, sin 3 iguales consecutivos) para un registro exitoso");
        } else {
            it.datosClave(fila.clave(), "tomada del control");
        }

        String otp = fila.otp().isBlank() ? Config.get("otp.defecto") : fila.otp();
        if (!otp.matches("\\d{6}")) {
            errores.add("OTP debe tener 6 digitos");
        }
        it.datosOtp(otp);
    }

    /**
     * Login con un cliente ya registrado. Con NumDocumento AUTO (o vacio) se toma un cliente REGISTRADO del listado
     * con su clave; con un documento informado, la clave vacia se toma del listado. El login no consume clientes.
     */
    private void reglasLogin(FilaControl fila, Iteracion it, List<String> errores) {
        boolean auto = reglasDocumento(fila, it, errores);
        if (fila.clave().isBlank()) {
            it.datosClave(null, "tomada del listado de clientes");
        } else if (auto) {
            errores.add("Con NumDocumento AUTO la clave se toma del listado de clientes: deje Clave vacia");
        } else if (!fila.clave().matches("\\d{4}")) {
            errores.add("Clave debe tener 4 digitos");
        } else {
            it.datosClave(fila.clave(), "tomada del control");
        }
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
