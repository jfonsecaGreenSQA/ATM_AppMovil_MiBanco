package com.greensqa.mibanco.atm.hooks;

import com.greensqa.mibanco.atm.context.ContextoIteracion;
import com.greensqa.mibanco.atm.control.Iteracion;
import com.greensqa.mibanco.atm.data.PoolClientes;
import com.greensqa.mibanco.atm.driver.DriverManager;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.Pantallas;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import java.util.Map;
import java.util.Optional;

/**
 * Ciclo de vida de cada iteracion: sesion de Appium, video, evidencia por paso y logs.
 * Vigila en cada paso el aviso de indisponibilidad del sistema, que puede aparecer en cualquier flujo:
 * si aparece, guarda evidencia y cancela la iteracion (los pasos siguientes no se ejecutan).
 */
public class Hooks {

    @Before
    public void antesDeLaIteracion() {
        Iteracion it = ContextoIteracion.actual();
        Evidencias.reiniciarConsecutivo();
        DriverManager.iniciar();
        registrarInfoDispositivo();
        Evidencias.limpiarLogcat(DriverManager.udid());
        Evidencias.iniciarVideo();
        Evidencias.texto("Datos de la iteracion", it.resumenEnmascarado());
        Evidencias.captura("Estado inicial del dispositivo");
    }

    @AfterStep
    public void despuesDeCadaPaso() {
        String paso = ContextoIteracion.pasoActual();
        Evidencias.captura(paso);
        Evidencias.codigoFuente(paso);
        Optional<String> aviso = detectarIndisponibilidad();
        if (aviso.isPresent()) {
            throw new IndisponibilidadSistemaException(aviso.get());
        }
    }

    @After
    public void despuesDeLaIteracion(Scenario escenario) {
        Iteracion it = ContextoIteracion.actual();
        try {
            if (DriverManager.activo()) {
                // Un paso pudo fallar por el aviso (p. ej. una espera que no se cumplio): se reclasifica
                if (escenario.isFailed() && ContextoIteracion.indisponibilidad() == null) {
                    detectarIndisponibilidad();
                }
                Evidencias.captura(escenario.isFailed() ? "Estado final - FALLA" : "Estado final");
                Evidencias.codigoFuente("Estado final");
                Evidencias.detenerVideo();
                Evidencias.logcat(DriverManager.udid());
                Evidencias.logServidorAppium();
            }
        } finally {
            liberarDocumentoReservado(it);
            DriverManager.cerrar();
        }
    }

    private Optional<String> detectarIndisponibilidad() {
        if (!DriverManager.activo()) {
            return Optional.empty();
        }
        Optional<String> aviso = Pantallas.indisponibilidad().avisoVisible();
        aviso.ifPresent(texto -> {
            ContextoIteracion.indisponibilidad(texto);
            Evidencias.captura("Indisponibilidad del sistema");
            Evidencias.texto("Indisponibilidad del sistema", "Paso: " + ContextoIteracion.pasoActual() + "\nAviso: " + texto);
        });
        return aviso;
    }

    private void liberarDocumentoReservado(Iteracion it) {
        if (!it.documentoReservado()) {
            return;
        }
        if (ContextoIteracion.indisponibilidad() != null) {
            // El cliente no tuvo problema: vuelve a estar disponible para otra ejecucion
            PoolClientes.get().actualizar(it.tipoDocumento(), it.numDocumento(), PoolClientes.Estado.DISPONIBLE, null,
                    "", "Liberado: iteracion " + it.idPrueba() + " cancelada por indisponibilidad del sistema");
        } else {
            // EN_USO sin confirmar el registro: no se reutiliza automaticamente
            PoolClientes.get().actualizar(it.tipoDocumento(), it.numDocumento(), PoolClientes.Estado.FALLIDO, null,
                    it.caso().idAzure(), "Iteracion " + it.idPrueba() + " fallo en: " + ContextoIteracion.pasoFallido());
        }
        it.liberarDocumento();
    }

    private void registrarInfoDispositivo() {
        Map<String, String> info = ContextoIteracion.infoEjecucion();
        if (info.isEmpty()) {
            info.put("Dispositivo", DriverManager.capacidad("deviceManufacturer") + " " + DriverManager.capacidad("deviceModel"));
            info.put("UDID", DriverManager.udid());
            info.put("Version.Android", DriverManager.capacidad("platformVersion"));
            info.put("Version.App", Evidencias.versionApp(DriverManager.udid(), DriverManager.paquete()));
        }
    }
}
