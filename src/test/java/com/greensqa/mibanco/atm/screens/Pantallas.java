package com.greensqa.mibanco.atm.screens;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.screens.android.BandejaSmsAndroid;
import com.greensqa.mibanco.atm.screens.android.BienvenidaAndroid;
import com.greensqa.mibanco.atm.screens.android.ClaveCambiadaAndroid;
import com.greensqa.mibanco.atm.screens.android.CrearClaveAndroid;
import com.greensqa.mibanco.atm.screens.android.DetalleCuentaAndroid;
import com.greensqa.mibanco.atm.screens.android.DetalleTransaccionAndroid;
import com.greensqa.mibanco.atm.screens.android.DialogosSistemaAndroid;
import com.greensqa.mibanco.atm.screens.android.HomeAndroid;
import com.greensqa.mibanco.atm.screens.android.IndisponibilidadAndroid;
import com.greensqa.mibanco.atm.screens.android.LoginAndroid;
import com.greensqa.mibanco.atm.screens.android.MenuCompartirAndroid;
import com.greensqa.mibanco.atm.screens.android.MenuMasAndroid;
import com.greensqa.mibanco.atm.screens.android.OtpAndroid;
import com.greensqa.mibanco.atm.screens.android.RecuperarClaveAndroid;
import com.greensqa.mibanco.atm.screens.android.RegistroDocumentoAndroid;
import com.greensqa.mibanco.atm.screens.android.RegistroExitosoAndroid;

import java.util.function.Supplier;

/** Unico punto donde se elige la implementacion por plataforma (propiedad "plataforma"). */
public final class Pantallas {

    private Pantallas() {
    }

    public static BienvenidaScreen bienvenida() {
        return android(BienvenidaAndroid::new);
    }

    public static RegistroDocumentoScreen registroDocumento() {
        return android(RegistroDocumentoAndroid::new);
    }

    public static OtpScreen otp() {
        return android(OtpAndroid::new);
    }

    public static CrearClaveScreen crearClave() {
        return android(CrearClaveAndroid::new);
    }

    public static RegistroExitosoScreen registroExitoso() {
        return android(RegistroExitosoAndroid::new);
    }

    public static DialogosSistemaScreen dialogosSistema() {
        return android(DialogosSistemaAndroid::new);
    }

    public static IndisponibilidadScreen indisponibilidad() {
        return android(IndisponibilidadAndroid::new);
    }

    public static LoginScreen login() {
        return android(LoginAndroid::new);
    }

    public static HomeScreen home() {
        return android(HomeAndroid::new);
    }

    public static DetalleCuentaScreen detalleCuenta() {
        return android(DetalleCuentaAndroid::new);
    }

    public static DetalleTransaccionScreen detalleTransaccion() {
        return android(DetalleTransaccionAndroid::new);
    }

    public static MenuCompartirScreen menuCompartir() {
        return android(MenuCompartirAndroid::new);
    }

    public static MenuMasScreen menuMas() {
        return android(MenuMasAndroid::new);
    }

    public static BandejaSmsScreen bandejaSms() {
        return android(BandejaSmsAndroid::new);
    }

    public static RecuperarClaveScreen recuperarClave() {
        return android(RecuperarClaveAndroid::new);
    }

    public static ClaveCambiadaScreen claveCambiada() {
        return android(ClaveCambiadaAndroid::new);
    }

    private static <T> T android(Supplier<T> implementacion) {
        String plataforma = Config.get("plataforma");
        if (!"android".equalsIgnoreCase(plataforma)) {
            throw new UnsupportedOperationException("Plataforma sin Page Objects: " + plataforma);
        }
        return implementacion.get();
    }
}
