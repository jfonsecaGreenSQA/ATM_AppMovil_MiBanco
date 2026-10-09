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
                    case REGISTRO_EXITOSO_CC -> reglasRegistroExitoso(fila, it, errores);
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
        Optional<TipoDocumento> tipo = TipoDocumento.desde(fila.tipoDocumento());
        if (fila.tipoDocumento().isBlank()) {
            errores.add("TipoDocumento es obligatorio");
        } else if (tipo.isEmpty()) {
            errores.add("TipoDocumento invalido: " + fila.tipoDocumento());
        } else if (tipo.get() == TipoDocumento.CE) {
            errores.add("Registro con CE aun no construido; solicitar habilitacion y datos al lider QA");
        }

        String num = fila.numDocumento();
        boolean auto = num.isBlank() || AUTO.equalsIgnoreCase(num);
        if (!auto && !num.matches("\\d{6,11}")) {
            errores.add("NumDocumento debe ser numerico de 6 a 11 digitos, o AUTO");
        }
        tipo.ifPresent(t -> it.datosDocumento(t, auto ? null : num));

        if (fila.clave().isBlank()) {
            it.datosClave(GeneradorClave.generar(), true);
        } else if (!GeneradorClave.cumpleRnR07(fila.clave())) {
            errores.add("Clave no cumple RN-R07 (4 digitos, sin 3 iguales consecutivos) para un registro exitoso");
        } else {
            it.datosClave(fila.clave(), false);
        }

        String otp = fila.otp().isBlank() ? Config.get("otp.defecto") : fila.otp();
        if (!otp.matches("\\d{6}")) {
            errores.add("OTP debe tener 6 digitos");
        }
        it.datosOtp(otp);
    }
}
