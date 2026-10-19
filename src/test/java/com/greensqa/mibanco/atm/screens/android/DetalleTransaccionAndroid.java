package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.DetalleTransaccionScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.List;

/**
 * Detalle de una transaccion, observado el 19/10/2026 en el S23. Se muestra dentro del mismo widget de Modyo que el
 * detalle de cuenta (accountDetailWidget), con el titulo "Detalle". Sin ids: los datos se leen por la etiqueta que
 * los precede y los botones por su texto, hasta que Desarrollo agregue los Test IDs solicitados (transaccion_*).
 */
public class DetalleTransaccionAndroid extends PantallaAndroid implements DetalleTransaccionScreen {

    private static final String WIDGET_ID = "accountDetailWidget";
    private static final String TITULO = "Detalle";
    private static final String COMPROBANTE = "Comprobante N°";
    private static final String DESCRIPCION = "Descripción";
    private static final By COMPROBANTE_ETIQUETA = uiSelector("new UiSelector().text(\"" + COMPROBANTE + "\")");
    private static final By VOLVER = AppiumBy.accessibilityId("Volver");
    private static final By COMPARTIR = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"Compartir\")");
    /** Aviso tras descargar, observado el 19/10/2026: "Tu comprobante se descargó correctamente." */
    private static final By AVISO_DESCARGA = uiSelector("new UiSelector().textStartsWith(\"Tu comprobante se descarg\")");
    private static final By DESCARGAR = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"Descargar\")");

    @Override
    public boolean estaVisible(Duration espera) {
        return visibleEn(COMPROBANTE_ETIQUETA, espera);
    }

    @Override
    public boolean estaVisible() {
        return presente(COMPROBANTE_ETIQUETA);
    }

    /** Orden observado: Detalle, etiqueta del monto, monto, fecha, Comprobante N°, numero, Descripción, texto. */
    @Override
    public Datos datos() {
        esperarVisible(COMPROBANTE_ETIQUETA);
        List<String> t = textosDelWidget(driver().getPageSource(), WIDGET_ID);
        return new Datos(despues(t, TITULO, 1), despues(t, TITULO, 2), despues(t, TITULO, 3), despues(t, COMPROBANTE, 1),
                despues(t, DESCRIPCION, 1), presente(COMPARTIR), presente(DESCARGAR));
    }

    @Override
    public void compartir() {
        tocar(COMPARTIR);
    }

    @Override
    public void descargar() {
        tocar(DESCARGAR);
    }

    @Override
    public java.util.Optional<String> avisoDescarga(Duration espera) {
        return visibleEn(AVISO_DESCARGA, espera) ? java.util.Optional.of(esperarVisible(AVISO_DESCARGA).getText().trim())
                : java.util.Optional.empty();
    }

    @Override
    public void volver() {
        tocar(VOLVER);
    }
}
