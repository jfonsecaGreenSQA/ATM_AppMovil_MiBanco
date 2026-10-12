package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.MenuMasScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Menu "Mas". Selectores observados en el S23 (Android 16). "Cerrar sesion" se ubica por su texto exacto: en el mismo
 * menu esta "Eliminacion Cuenta", que nunca debe tocarse.
 */
public class MenuMasAndroid extends PantallaAndroid implements MenuMasScreen {

    private static final By CERRAR_SESION = uiSelector(
            "new UiSelector().className(\"android.widget.Button\").text(\"Cerrar sesión\")");
    private static final By DIALOGO = rid("mba-modal");
    // El boton de confirmar del dialogo expone el texto como content-desc; el del menu no
    private static final By CONFIRMAR = AppiumBy.accessibilityId("Cerrar sesión");

    @Override
    public void cerrarSesion() {
        tocar(CERRAR_SESION);
        esperarVisible(DIALOGO);
        tocar(CONFIRMAR);
    }
}
