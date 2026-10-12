package com.greensqa.mibanco.atm.business;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.evidence.Evidencias;
import com.greensqa.mibanco.atm.screens.IndisponibilidadScreen;
import com.greensqa.mibanco.atm.screens.Pantallas;
import io.qameta.allure.Allure;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Regla del proyecto para el aviso "Validacion fallida. No es posible atender tu solicitud por este canal", que el
 * backend devuelve de forma intermitente: se acepta el aviso y se repite la accion que lo provoco, como maximo
 * indisponibilidad.reintentos veces. Si persiste, el aviso queda en pantalla; el hook de cada paso lo detecta y la
 * iteracion queda FALLIDA por indisponibilidad.
 */
public class ReintentoIndisponibilidad {

    private final IndisponibilidadScreen aviso = Pantallas.indisponibilidad();

    /**
     * @param accion            nombre de la accion para la evidencia (p. ej. "ingresar")
     * @param ejecutar          accion que llama al backend (p. ej. tocar Ingresar)
     * @param antesDeReintentar datos que la app borra al mostrar el aviso y hay que volver a ingresar
     * @param respondio         true cuando la app ya respondio (siguiente pantalla u otro aviso)
     * @param espera            tiempo maximo de respuesta de cada intento
     */
    public void ejecutar(String accion, Runnable ejecutar, Runnable antesDeReintentar, BooleanSupplier respondio, Duration espera) {
        int maximo = Config.entero("indisponibilidad.reintentos");
        for (int reintento = 0; ; reintento++) {
            ejecutar.run();
            esperarRespuesta(respondio, espera);
            Optional<String> texto = aviso.avisoVisible();
            if (texto.isEmpty() || reintento >= maximo) {
                return;
            }
            Evidencias.captura("Indisponibilidad al " + accion + " - se reintenta (" + (reintento + 1) + " de " + maximo + ")");
            Allure.step("La app mostro '" + texto.get() + "' al " + accion + ": se acepta y se reintenta ("
                    + (reintento + 1) + " de " + maximo + ")");
            aviso.cerrarAviso();
            antesDeReintentar.run();
        }
    }

    private void esperarRespuesta(BooleanSupplier respondio, Duration espera) {
        try {
            new FluentWait<>(aviso)
                    .withTimeout(espera)
                    .pollingEvery(Duration.ofMillis(700))
                    .until(a -> respondio.getAsBoolean() || a.avisoVisible().isPresent() ? Boolean.TRUE : null);
        } catch (TimeoutException e) {
            // Sin respuesta: la verificacion del paso informa la falla
        }
    }
}
