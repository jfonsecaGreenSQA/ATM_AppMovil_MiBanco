package com.greensqa.mibanco.atm.screens;

import com.greensqa.mibanco.atm.config.Config;
import com.greensqa.mibanco.atm.screens.android.BienvenidaAndroid;
import com.greensqa.mibanco.atm.screens.android.CrearClaveAndroid;
import com.greensqa.mibanco.atm.screens.android.DialogosSistemaAndroid;
import com.greensqa.mibanco.atm.screens.android.IndisponibilidadAndroid;
import com.greensqa.mibanco.atm.screens.android.OtpAndroid;
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

    private static <T> T android(Supplier<T> implementacion) {
        String plataforma = Config.get("plataforma");
        if (!"android".equalsIgnoreCase(plataforma)) {
            throw new UnsupportedOperationException("Plataforma sin Page Objects: " + plataforma);
        }
        return implementacion.get();
    }
}
