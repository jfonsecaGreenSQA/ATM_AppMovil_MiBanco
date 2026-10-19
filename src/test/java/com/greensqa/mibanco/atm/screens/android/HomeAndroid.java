package com.greensqa.mibanco.atm.screens.android;

import com.greensqa.mibanco.atm.driver.DriverManager;
import com.greensqa.mibanco.atm.screens.HomeScreen;
import com.greensqa.mibanco.atm.screens.ObservacionCarga;
import com.greensqa.mibanco.atm.screens.ProductoHome;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Element;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final By MONTO = uiSelector("new UiSelector().textStartsWith(\"$\")");
    private static final Pattern DIAPOSITIVA = Pattern.compile("^splide\\d+-slide\\d+$");
    /** content-desc de una tarjeta: "Préstamos $71.454.146,91 Cuota vencio 1 Jun" = antes del monto, monto, estado. */
    private static final Pattern TARJETA_DESC = Pattern.compile("^(.*?)\\s*(\\$[\\d.]+(?:,\\d+)?)\\s*(.*)$");
    private static final By CAMPANAS_TITULO = rid("campaigns-heading");
    /** Banner de cada campana: "Ver campaña (se abre en una nueva pestaña): Promo 2" (observado el 15/10/2026). */
    private static final String VER_CAMPANA = "Ver campaña";
    private static final By BANNER_CAMPANA = uiSelector("new UiSelector().descriptionStartsWith(\"" + VER_CAMPANA + "\")");
    /** Puntos de paginacion de un carrusel (cuentas o campanas); el de la diapositiva visible esta seleccionado. */
    private static final Pattern PAGINA_CARRUSEL = Pattern.compile("^Go to slide (\\d+)$");
    private static final String BANNER_PRE_APROBADO = "Tienes un crédito pre-aprobado";
    private static final String OCULTAR_MONTOS = "Ocultar montos";
    private static final int MAX_DESLIZAMIENTOS = 5;
    /** Pasos de un tercio de pantalla para recorrer el Home completo (varias cuentas, creditos y campanas). */
    private static final int MAX_PASOS_PRODUCTOS = 20;
    /** Tarjeta de una cuenta en el carrusel "Tus cuentas" (su titulo: "Cuentas de ahorro **** 4066"). */
    private static final By TARJETA_CUENTA = uiSelector("new UiSelector().descriptionStartsWith(\"Cuenta\").clickable(true)");
    /** Contenedor de un carrusel (splideNN); el de cuentas y el de campanas cambian de numero segun el orden de carga. */
    private static final Pattern CARRUSEL = Pattern.compile("^splide\\d+$");

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

    /**
     * Lee los productos del arbol de UI (una lectura por posicion de scroll) y desliza hasta que la pantalla no cambie.
     * Estructura observada: cada cuenta es una diapositiva del carrusel ("splideNN-slideNN") con el boton "Ocultar
     * montos"; cada credito y el banner de pre-aprobado son un elemento tocable cuyo content-desc resume la tarjeta.
     */
    @Override
    public List<ProductoHome> productos() {
        esperarProductosCargados();
        String inicial = driver().getPageSource();
        Set<ProductoHome> productos = new LinkedHashSet<>(leerProductos(inicial));
        // El carrusel "Tus cuentas" solo expone la diapositiva en pantalla (observado el 19/10/2026: 3 diapositivas, la
        // segunda cuenta en la tercera): se recorre por sus puntos de paginacion y se vuelve a la primera
        int diapositivas = puntosDeCuentas(inicial).size();
        for (int i = 2; i <= diapositivas; i++) {
            if (irADiapositivaDeCuentas(i)) {
                productos.addAll(leerProductos(driver().getPageSource()));
            }
        }
        if (diapositivas > 1) {
            irADiapositivaDeCuentas(1);
        }
        String anterior = "";
        esperarPantallaQuieta();
        for (int i = 0; i <= MAX_PASOS_PRODUCTOS; i++) {
            String fuente = driver().getPageSource();
            if (fuente.equals(anterior)) {
                break;
            }
            productos.addAll(leerProductos(fuente));
            anterior = fuente;
            deslizarHaciaAbajo();
        }
        return sinParciales(productos);
    }

    /**
     * Una tarjeta cortada en el borde de la pantalla se lee sin algunos elementos (p. ej. el ojo de la cuenta sale por
     * arriba antes que el titulo). Las lecturas de la misma tarjeta (seccion, titulo y monto) se combinan; una lectura
     * sin monto se descarta si la tarjeta se leyo con monto en otra posicion.
     */
    private static List<ProductoHome> sinParciales(Set<ProductoHome> productos) {
        Map<String, ProductoHome> unicos = new LinkedHashMap<>();
        for (ProductoHome p : productos) {
            boolean hayConMonto = productos.stream().anyMatch(q -> q.seccion() == p.seccion()
                    && q.titulo().equals(p.titulo()) && !q.monto().isEmpty());
            if (p.monto().isEmpty() && hayConMonto) {
                continue;
            }
            unicos.merge(p.seccion() + "|" + p.titulo() + "|" + p.monto(), p, HomeAndroid::combinar);
        }
        return new ArrayList<>(unicos.values());
    }

    private static ProductoHome combinar(ProductoHome a, ProductoHome b) {
        return new ProductoHome(a.seccion(), a.titulo(), masLargo(a.etiqueta(), b.etiqueta()), a.monto(),
                masLargo(a.estado(), b.estado()), a.navegable() || b.navegable(), a.ocultarMontos() || b.ocultarMontos());
    }

    private static String masLargo(String a, String b) {
        return b.length() > a.length() ? b : a;
    }

    /** Lectura por DevTools del WebView: con UiAutomator cada consulta tarda 1-5 s durante la carga. */
    @Override
    public ObservacionCarga observarCarga() {
        return new ObservacionCargaAndroid(DriverManager.udid(), DriverManager.paquete());
    }

    /** Espera a que el skeleton de "Tus cuentas" / "Tus creditos" sea reemplazado por al menos un monto. */
    private void esperarProductosCargados() {
        esperarVisible(DASHBOARD);
        visibleEn(MONTO, espera());
    }

    /**
     * Avanza un tercio de pantalla sin inercia y espera a que quede quieta: con un deslizamiento rapido se saltaban
     * tarjetas (observado el 19/10/2026: un cliente con 10 productos se leia con 7).
     */
    private void deslizarHaciaAbajo() {
        avanzarDespacio(0.33);
        esperarPantallaQuieta();
    }

    /**
     * El carrusel de campanas usa ids splideNN-slideNN igual que el de cuentas y su numero depende del orden de carga:
     * se lee a partir del titulo de la seccion.
     */
    @Override
    public Campanas campanas() {
        esperarProductosCargados();
        Dimension pantalla = driver().manage().window().getSize();
        for (int i = 0; i <= MAX_DESLIZAMIENTOS; i++) {
            List<WebElement> titulo = driver().findElements(CAMPANAS_TITULO);
            if (!titulo.isEmpty() && titulo.get(0).getRect().y < pantalla.height / 2) {
                break;
            }
            deslizar("up", 0.5);
        }
        return leerCampanas(driver().getPageSource());
    }

    @Override
    public Campanas siguienteCampana() {
        Campanas antes = leerCampanas(driver().getPageSource());
        Rectangle banner = esperarVisible(BANNER_CAMPANA).getRect();
        driver().executeScript("mobile: swipeGesture", Map.of("left", banner.x + banner.width / 8, "top", banner.y + banner.height / 4,
                "width", banner.width * 3 / 4, "height", banner.height / 2, "direction", "left", "percent", 0.75));
        try {
            new WebDriverWait(driver(), Duration.ofSeconds(5)).until(d -> leerCampanas(d.getPageSource()).actual() != antes.actual());
        } catch (TimeoutException e) {
            // Sin otra campana el carrusel no cambia: se devuelve lo que muestra
        }
        return leerCampanas(driver().getPageSource());
    }

    private static Campanas leerCampanas(String fuente) {
        String titulo = "";
        String campana = "";
        int total = 0;
        int actual = 0;
        for (Element e : descendientes(documento(fuente).getDocumentElement())) {
            if (titulo.isEmpty()) {
                if ("campaigns-heading".equals(e.getAttribute("resource-id"))) {
                    titulo = e.getAttribute("text").trim();
                }
                continue;
            }
            String desc = e.getAttribute("content-desc");
            if (campana.isEmpty() && desc.startsWith(VER_CAMPANA)) {
                campana = desc.substring(desc.lastIndexOf(':') + 1).trim();
            }
            Matcher pagina = PAGINA_CARRUSEL.matcher(desc);
            if (pagina.matches()) {
                total++;
                if ("true".equals(e.getAttribute("selected"))) {
                    actual = Integer.parseInt(pagina.group(1));
                }
            }
        }
        return new Campanas(titulo, campana, Math.max(total, 1), Math.max(actual, 1));
    }

    /** Vuelve al inicio del Home (la lectura de productos lo deja al final) y toca la tarjeta con ese titulo exacto. */
    @Override
    public void abrirCuenta(String titulo) {
        By cuenta = uiSelector("new UiSelector().description(\"" + titulo + "\").clickable(true)");
        deslizarAlInicio(MAX_DESLIZAMIENTOS + 1);
        esperarPantallaQuieta();
        // Al volver de un detalle el Home se recarga ("Cargando..."): el carrusel aparece cuando terminan los productos
        esperarProductosCargados();
        visibleEn(TARJETA_CUENTA, espera());
        int diapositivas = puntosDeCuentas(driver().getPageSource()).size();
        for (int i = 1; i <= diapositivas && !titulo.equals(cuentaVisible()); i++) {
            irADiapositivaDeCuentas(i);
        }
        if (!titulo.equals(cuentaVisible())) {
            throw new NoSuchElementException("No se encontro la cuenta '" + titulo + "' en el carrusel; en pantalla: '"
                    + cuentaVisible() + "'");
        }
        tocar(cuenta);
    }

    /**
     * Va a la diapositiva {@code numero} del carrusel "Tus cuentas" tocando su punto de paginacion y espera a que quede
     * seleccionado. Se usan los puntos y no un deslizamiento: un deslizamiento rapido salta diapositivas.
     *
     * @return true si la diapositiva quedo seleccionada
     */
    private boolean irADiapositivaDeCuentas(int numero) {
        List<Element> puntos = puntosDeCuentas(driver().getPageSource());
        if (numero > puntos.size()) {
            return false;
        }
        int[] centro = centro(puntos.get(numero - 1).getAttribute("bounds"));
        driver().executeScript("mobile: clickGesture", Map.of("x", centro[0], "y", centro[1]));
        try {
            new WebDriverWait(driver(), Duration.ofSeconds(5)).until(d -> {
                List<Element> p = puntosDeCuentas(d.getPageSource());
                return p.size() >= numero && "true".equals(p.get(numero - 1).getAttribute("selected"));
            });
            esperarPantallaQuieta();
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Puntos de paginacion ("Go to slide N") del carrusel que contiene las cuentas, en orden; vacio si no esta en
     * pantalla o tiene una sola diapositiva. El banner de campanas tiene sus propios puntos: se toman solo los del
     * carrusel de cuentas.
     */
    private static List<Element> puntosDeCuentas(String fuente) {
        for (Element e : descendientes(documento(fuente).getDocumentElement())) {
            if (!CARRUSEL.matcher(e.getAttribute("resource-id")).matches()) {
                continue;
            }
            List<Element> contenido = descendientes(e);
            if (contenido.stream().anyMatch(c -> c.getAttribute("content-desc").startsWith("Cuenta"))) {
                return contenido.stream().filter(c -> PAGINA_CARRUSEL.matcher(c.getAttribute("content-desc")).matches()).toList();
            }
        }
        return List.of();
    }

    /** Centro de unos bounds "[x1,y1][x2,y2]". */
    private static int[] centro(String bounds) {
        String[] n = bounds.replaceAll("[\\[\\]]", ",").replaceAll(",+", ",").replaceAll("^,|,$", "").split(",");
        return new int[]{(Integer.parseInt(n[0]) + Integer.parseInt(n[2])) / 2, (Integer.parseInt(n[1]) + Integer.parseInt(n[3])) / 2};
    }

    /** Titulo de la cuenta en pantalla en el carrusel; vacio si no hay. */
    private String cuentaVisible() {
        List<WebElement> tarjeta = driver().findElements(TARJETA_CUENTA);
        return tarjeta.isEmpty() ? "" : tarjeta.get(0).getAttribute("content-desc").trim();
    }

    private static List<ProductoHome> leerProductos(String fuente) {
        List<ProductoHome> productos = new ArrayList<>();
        recorrer(documento(fuente).getDocumentElement(), productos);
        return productos;
    }

    private static void recorrer(Element e, List<ProductoHome> productos) {
        String desc = e.getAttribute("content-desc");
        if (DIAPOSITIVA.matcher(e.getAttribute("resource-id")).matches()) {
            cuenta(e).ifPresent(productos::add);
            return;
        }
        if ("true".equals(e.getAttribute("clickable")) && desc.startsWith(BANNER_PRE_APROBADO)) {
            tarjeta(ProductoHome.Seccion.BANNER, e).ifPresent(productos::add);
            return;
        }
        if ("true".equals(e.getAttribute("clickable")) && desc.contains("$")) {
            tarjeta(ProductoHome.Seccion.CREDITOS, e).ifPresent(productos::add);
            return;
        }
        for (Element hijo : hijos(e)) {
            recorrer(hijo, productos);
        }
    }

    /** Diapositiva de "Tus cuentas": titulo en el content-desc, luego etiqueta y monto. */
    private static Optional<ProductoHome> cuenta(Element diapositiva) {
        List<Element> todos = descendientes(diapositiva);
        Optional<Element> titulo = todos.stream().filter(d -> d.getAttribute("content-desc").startsWith("Cuenta")).findFirst();
        if (titulo.isEmpty()) {
            return Optional.empty();
        }
        String textoTitulo = titulo.get().getAttribute("content-desc").trim();
        List<String> textos = textos(todos).stream().filter(t -> !t.equals(textoTitulo)).toList();
        String monto = textos.stream().filter(t -> t.startsWith("$")).findFirst().orElse("");
        String etiqueta = textos.stream().filter(t -> !t.startsWith("$")).findFirst().orElse("");
        boolean ojo = todos.stream().anyMatch(d -> OCULTAR_MONTOS.equals(d.getAttribute("content-desc")));
        return Optional.of(new ProductoHome(ProductoHome.Seccion.CUENTAS, textoTitulo, etiqueta, monto, "",
                "true".equals(titulo.get().getAttribute("clickable")), ojo));
    }

    /**
     * Tarjeta de credito o banner: textos en orden = titulo, [etiqueta], monto, [estado]. Vacio si la tarjeta esta
     * cortada en el borde de la pantalla y no trae textos (se lee completa en otra posicion de scroll).
     * Se lee primero del content-desc del contenedor, que el WebView entrega completo aunque la tarjeta este cortada
     * (observado el 14/10/2026: cortada abajo, los hijos traen titulo y monto pero no "Cuota vencio 1 Jun", y la
     * lectura incompleta aparecia como una tarjeta de mas sin estado).
     */
    private static Optional<ProductoHome> tarjeta(ProductoHome.Seccion seccion, Element tarjeta) {
        List<String> textos = textos(descendientes(tarjeta));
        if (textos.isEmpty()) {
            return Optional.empty();
        }
        Matcher desc = TARJETA_DESC.matcher(tarjeta.getAttribute("content-desc").replaceAll("\\s+", " ").trim());
        if (desc.matches()) {
            String antesDelMonto = desc.group(1).trim();
            // El titulo es el primer texto de la tarjeta, si no quedo cortado por arriba (entonces el primero es el monto)
            String titulo = antesDelMonto.startsWith(textos.get(0)) && !textos.get(0).startsWith("$")
                    ? textos.get(0) : antesDelMonto;
            return Optional.of(new ProductoHome(seccion, titulo, antesDelMonto.substring(titulo.length()).trim(),
                    desc.group(2), desc.group(3).trim(), true, false));
        }
        String titulo = textos.get(0);
        int iMonto = -1;
        for (int i = 1; i < textos.size(); i++) {
            if (textos.get(i).startsWith("$")) {
                iMonto = i;
                break;
            }
        }
        String etiqueta = iMonto < 0 ? "" : String.join(" ", textos.subList(1, iMonto));
        String monto = iMonto < 0 ? "" : textos.get(iMonto);
        String estado = String.join(" ", textos.subList(iMonto < 0 ? 1 : iMonto + 1, textos.size())).replaceAll("\\s+", " ");
        return Optional.of(new ProductoHome(seccion, titulo, etiqueta, monto, estado.trim(), true, false));
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
