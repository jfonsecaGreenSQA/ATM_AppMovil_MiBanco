package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.driver.Adb;
import com.greensqa.mibanco.atm.driver.WebViewDevTools;
import com.greensqa.mibanco.atm.screens.ObservacionCarga;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee la pagina del WebView por DevTools cada {@value #INTERVALO_MS} ms en un hilo propio. Durante la carga del Home,
 * "Tus cuentas" y "Tus creditos" muestran "Cargando..." unos 4 s (observado el 12/10/2026); con UiAutomator cada
 * consulta tarda 1-5 s y el estado se pierde. La captura usa adb screencap para no competir con la sesion de Appium.
 */
final class ObservacionCargaAndroid implements ObservacionCarga, Runnable {

    private static final long INTERVALO_MS = 200;
    /** Cuenta indicadores y montos en el texto visible de la pagina; responde "indicadores|montos". */
    private static final String CONTEO = "(function(){var t=document.body?document.body.innerText:'';"
            + "return (t.match(/Cargando\\.\\.\\./g)||[]).length+'|'+(t.match(/\\$[0-9.,]+/g)||[]).length;})()";
    private static final Pattern RESPUESTA = Pattern.compile("^(\\d+)\\|(\\d+)$");

    private final String udid;
    private final WebViewDevTools devTools;
    private final AtomicInteger maximo = new AtomicInteger();
    private final AtomicBoolean montosAntes = new AtomicBoolean();
    private final AtomicBoolean conMontos = new AtomicBoolean();
    private final AtomicReference<byte[]> captura = new AtomicReference<>();
    private final CountDownLatch fin = new CountDownLatch(1);
    private volatile boolean activa = true;

    ObservacionCargaAndroid(String udid, String paquete) {
        this.udid = udid;
        this.devTools = WebViewDevTools.conectar(udid, paquete);
        Thread hilo = new Thread(this, "observacion-carga-home");
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    public void run() {
        while (activa) {
            devTools.evaluar(CONTEO).map(RESPUESTA::matcher).filter(Matcher::matches).ifPresent(m -> {
                int indicadores = Integer.parseInt(m.group(1));
                int montos = Integer.parseInt(m.group(2));
                if (indicadores > 0 && maximo.get() == 0) {
                    captura.set(capturar());
                }
                maximo.accumulateAndGet(indicadores, Math::max);
                if (montos > 0 && maximo.get() == 0) {
                    montosAntes.set(true);
                }
                if (montos > 0 && indicadores == 0) {
                    conMontos.set(true);
                    activa = false;
                }
            });
            dormir();
        }
        fin.countDown();
    }

    private byte[] capturar() {
        try {
            return Adb.binario(udid, "exec-out", "screencap", "-p");
        } catch (Exception e) {
            return null;
        }
    }

    private static void dormir() {
        try {
            Thread.sleep(INTERVALO_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void esperarFin(Duration espera) {
        try {
            fin.await(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int maximoIndicadores() {
        return maximo.get();
    }

    @Override
    public boolean montosAntesDelSkeleton() {
        return montosAntes.get();
    }

    @Override
    public boolean terminoConMontos() {
        return conMontos.get();
    }

    @Override
    public Optional<byte[]> capturaSkeleton() {
        return Optional.ofNullable(captura.get());
    }

    @Override
    public void close() {
        activa = false;
        esperarFin(Duration.ofSeconds(5));
        devTools.close();
    }
}
