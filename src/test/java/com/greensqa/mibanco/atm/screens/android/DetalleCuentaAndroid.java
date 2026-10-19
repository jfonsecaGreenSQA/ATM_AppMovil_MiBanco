package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.DetalleCuentaScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Detalle de cuenta (widget de Modyo accountDetailWidget), observado el 15/10/2026 en el S23. Salvo el widget, los
 * elementos no tienen id: los textos se leen en el orden del arbol de UI y los botones por su descripcion de
 * accesibilidad hasta que Desarrollo agregue los Test IDs solicitados (cuenta_detalle_*).
 */
public class DetalleCuentaAndroid extends PantallaAndroid implements DetalleCuentaScreen {

    private static final String WIDGET_ID = "accountDetailWidget";
    private static final By WIDGET = rid(WIDGET_ID);
    private static final By VOLVER = AppiumBy.accessibilityId("Volver");
    private static final By COMPARTIR = AppiumBy.accessibilityId("Compartir cuenta");
    private static final By SALDO_DISPONIBLE = uiSelector("new UiSelector().text(\"Saldo disponible\")");
    private static final String TITULO = "Cuenta";
    private static final String SALDO_DISPONIBLE_TEXTO = "Saldo disponible";
    private static final String SALDO_EN_CANJE_TEXTO = "Saldo en canje";
    private static final String MOVIMIENTOS_TEXTO = "Movimientos";
    private static final String PROXIMAMENTE = "¡Próximamente!";
    private static final String SIN_MOVIMIENTOS = "No tienes movimientos";
    /**
     * Aviso sobre la lista. Observado el 19/10/2026: "Los movimientos mostrados corresponden a las últimas 20
     * transacciones realizadas en tu producto"; el caso 39566 dice "listados ... en su producto". Se aceptan ambos.
     */
    private static final String ULTIMAS_20 = "Los movimientos";
    private static final String ULTIMAS_20_CLAVE = "últimas 20 transacciones";
    /** Pasos de un tercio de pantalla: alcanza para 20 movimientos (unos 350 px cada uno) mas el encabezado. */
    private static final int MAX_DESLIZAMIENTOS = 25;

    @Override
    public boolean estaVisible(Duration espera) {
        return visibleEn(WIDGET, espera) && visibleEn(SALDO_DISPONIBLE, Duration.ofSeconds(10));
    }

    @Override
    public boolean estaVisible() {
        return presente(WIDGET);
    }

    @Override
    public Encabezado encabezado() {
        esperarVisible(SALDO_DISPONIBLE);
        List<String> t = textosDelDetalle(driver().getPageSource());
        int canje = t.indexOf(SALDO_EN_CANJE_TEXTO);
        int movimientos = t.indexOf(MOVIMIENTOS_TEXTO);
        List<String> acciones = canje < 0 || movimientos < canje + 2 ? List.of()
                : t.subList(canje + 2, movimientos).stream().filter(x -> !x.equals(PROXIMAMENTE)).toList();
        return new Encabezado(despues(t, TITULO, 1), despues(t, TITULO, 2), despues(t, SALDO_DISPONIBLE_TEXTO, 1),
                despues(t, SALDO_EN_CANJE_TEXTO, 1), presente(COMPARTIR), acciones);
    }

    @Override
    public Movimientos movimientos() {
        Set<String> textos = new LinkedHashSet<>();
        Set<String> registros = new LinkedHashSet<>();
        String anterior = "";
        // Cada lectura se hace con la pantalla quieta: tras deslizar, el arbol de UI tarda en actualizarse y una lectura
        // igual a la anterior se tomaba como el final de la lista (observado el 19/10/2026: se leian 2 de 8 movimientos)
        esperarPantallaQuieta();
        for (int i = 0; i <= MAX_DESLIZAMIENTOS; i++) {
            String fuente = driver().getPageSource();
            List<String> t = textosDelDetalle(fuente);
            registros.addAll(registrosDe(fuente));
            int titulo = t.indexOf(MOVIMIENTOS_TEXTO);
            // Ya desplazado, el titulo sale por arriba: lo que sigue en el detalle es la seccion de movimientos
            // (el titulo "Cuenta" queda fijo arriba y no es de la seccion)
            textos.addAll(titulo >= 0 ? t.subList(titulo + 1, t.size())
                    : textos.isEmpty() ? List.of() : t.stream().filter(x -> !x.equals(TITULO)).toList());
            Optional<String> aviso = textos.stream().filter(x -> esAviso(x)).findFirst();
            if (aviso.isPresent() && aviso.get().startsWith(SIN_MOVIMIENTOS) || fuente.equals(anterior)) {
                return resultado(textos, registros);
            }
            anterior = fuente;
            avanzarDespacio(0.33);
            esperarPantallaQuieta();
        }
        return resultado(textos, registros);
    }

    /**
     * Vuelve al inicio del detalle y baja de a poco hasta que el movimiento quede completo en pantalla, debajo del
     * encabezado fijo; solo entonces lo toca (con la pantalla quieta, ver {@link #esperarPantallaQuieta()}).
     */
    @Override
    public void abrirMovimiento(String texto) {
        By movimiento = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"" + texto + "\")");
        deslizarAlInicio(MAX_DESLIZAMIENTOS);
        esperarPantallaQuieta();
        Dimension pantalla = driver().manage().window().getSize();
        for (int i = 0; i <= MAX_DESLIZAMIENTOS; i++) {
            List<WebElement> encontrado = driver().findElements(movimiento);
            if (!encontrado.isEmpty()) {
                Rectangle r = encontrado.get(0).getRect();
                if (r.y > pantalla.height * 0.13 && r.y + r.height < pantalla.height * 0.95) {
                    tocar(encontrado.get(0));
                    return;
                }
            }
            avanzarDespacio(0.25);
            esperarPantallaQuieta();
        }
        throw new NoSuchElementException("No se pudo dejar en pantalla el movimiento: " + texto);
    }

    @Override
    public void volver() {
        tocar(VOLVER);
    }

    private static boolean esAviso(String texto) {
        return texto.startsWith(SIN_MOVIMIENTOS) || texto.startsWith(ULTIMAS_20) && texto.contains(ULTIMAS_20_CLAVE);
    }

    private static Movimientos resultado(Set<String> textos, Set<String> registros) {
        String aviso = textos.stream().filter(x -> esAviso(x)).findFirst().orElse("");
        return new Movimientos(aviso.startsWith(SIN_MOVIMIENTOS), aviso, new ArrayList<>(textos), new ArrayList<>(registros));
    }

    /** Cada movimiento es un boton con descripcion, fecha y monto en su texto (observado el 19/10/2026). */
    private static List<String> registrosDe(String fuente) {
        return buscarPorId(documento(fuente).getDocumentElement(), WIDGET_ID).map(w -> descendientes(w).stream()
                .filter(e -> e.getAttribute("class").endsWith("Button") && e.getAttribute("text").contains("$"))
                .map(e -> e.getAttribute("text").trim()).toList()).orElse(List.of());
    }

    /** Textos del detalle en el orden del arbol de UI (solo lo que esta en pantalla). */
    private static List<String> textosDelDetalle(String fuente) {
        return textosDelWidget(fuente, WIDGET_ID);
    }
}
