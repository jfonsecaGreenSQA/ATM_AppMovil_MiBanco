package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.screens.HomeScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Home (widget de Modyo). El saludo, la pregunta, el ultimo ingreso y el menu inferior no tienen id: se ubican por
 * texto hasta que Desarrollo agregue los Test IDs solicitados (home_saludo_text, home_pregunta_text,
 * home_ultimo_ingreso_text, home_menu_inicio_button, home_menu_mas_button).
 */
public class HomeAndroid extends PantallaAndroid implements HomeScreen {

    private static final By DASHBOARD = rid("dashboardWidget");
    private static final By SALUDO = uiSelector("new UiSelector().textStartsWith(\"Hola,\")");
    private static final By PREGUNTA = uiSelector("new UiSelector().textStartsWith(\"¿Qué quieres\")");
    private static final By ULTIMO_INGRESO = uiSelector("new UiSelector().textStartsWith(\"Último ingreso\")");
    private static final By MENU_INICIO = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"Inicio\")");
    private static final By MENU_MAS = uiSelector("new UiSelector().className(\"android.widget.Button\").text(\"Más\")");
    private static final By AVISO_TITULO = rid("mb-message-modal-title");
    private static final By BOTON = By.className("android.widget.Button");

    @Override
    public boolean estaVisible(Duration espera) {
        return visibleEn(DASHBOARD, espera) && visibleEn(SALUDO, Duration.ofSeconds(5));
    }

    @Override
    public boolean estaVisible() {
        return presente(DASHBOARD);
    }

    @Override
    public String saludo() {
        return esperarVisible(SALUDO).getText().trim();
    }

    @Override
    public String preguntaActividad() {
        return esperarVisible(PREGUNTA).getText().trim();
    }

    @Override
    public Optional<String> ultimoIngreso() {
        // Se muestra junto con el saludo: si no esta en unos segundos, el cliente no tenia ingresos previos
        if (!visibleEn(ULTIMO_INGRESO, Duration.ofSeconds(5))) {
            return Optional.empty();
        }
        List<WebElement> e = driver().findElements(ULTIMO_INGRESO);
        return e.isEmpty() ? Optional.empty() : Optional.of(e.get(0).getText().trim());
    }

    @Override
    public boolean menuInferiorVisible() {
        return visibleEn(MENU_INICIO, Duration.ofSeconds(5));
    }

    /**
     * Los accesos exponen su nombre en content-desc ("Bre-B", "Pedir Credito (se abre en una nueva pestana)").
     * Debe quedar en la mitad superior de la pantalla, por encima del menu inferior.
     */
    @Override
    public boolean accesoSuperiorVisible(String nombre) {
        By acceso = uiSelector("new UiSelector().descriptionStartsWith(\"" + nombre + "\")");
        if (!visibleEn(acceso, Duration.ofSeconds(10))) {
            return false;
        }
        int altoPantalla = driver().manage().window().getSize().height;
        return driver().findElement(acceso).getRect().y < altoPantalla / 2;
    }

    /** Botones de la franja inferior de la pantalla (ultimo 15 %), ordenados por su posicion horizontal. */
    @Override
    public List<String> accesosMenuInferior() {
        esperarVisible(MENU_INICIO);
        int franja = (int) (driver().manage().window().getSize().height * 0.85);
        return driver().findElements(BOTON).stream()
                .filter(b -> b.getRect().y >= franja)
                .sorted(Comparator.comparingInt(b -> b.getRect().x))
                .map(b -> b.getText().trim())
                .toList();
    }

    @Override
    public void abrirMenuMas() {
        tocar(MENU_MAS);
    }

    @Override
    public boolean hayAviso() {
        return presente(AVISO_TITULO);
    }
}
