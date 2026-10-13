package com.greensqa.mibanco.atm.driver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Lectura de la pagina del WebView de la app por Chrome DevTools. El APK de pruebas es debuggable: el WebView publica
 * el socket "webview_devtools_remote_<pid>". Una lectura tarda milisegundos, frente a 1-5 s de UiAutomator mientras
 * el Home carga; se usa para observar estados breves como el skeleton. Solo lectura (Runtime.evaluate).
 * El WebView rechaza conexiones con encabezado Origin: el WebSocket de Java no lo envia.
 */
public final class WebViewDevTools implements AutoCloseable {

    private static final Gson GSON = new Gson();
    private static final Duration ESPERA = Duration.ofSeconds(3);

    private final String udid;
    private final int puerto;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA).build();
    private WebSocket ws;
    private volatile CompletableFuture<String> respuesta;
    private final StringBuilder mensaje = new StringBuilder();
    private volatile int id;

    private WebViewDevTools(String udid, int puerto) {
        this.udid = udid;
        this.puerto = puerto;
    }

    /** Redirige un puerto local al socket de DevTools del WebView del proceso actual de la app. */
    public static WebViewDevTools conectar(String udid, String paquete) {
        try {
            String pid = Adb.ejecutar(udid, "shell", "pidof", paquete).trim();
            if (!pid.matches("\\d+")) {
                throw new IllegalStateException("La app " + paquete + " no esta en ejecucion");
            }
            int puerto;
            try (ServerSocket libre = new ServerSocket(0)) {
                puerto = libre.getLocalPort();
            }
            Adb.ejecutar(udid, "forward", "tcp:" + puerto, "localabstract:webview_devtools_remote_" + pid);
            return new WebViewDevTools(udid, puerto);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo conectar a DevTools del WebView (requiere APK debuggable)", e);
        }
    }

    /**
     * Evalua una expresion JavaScript en la pagina y devuelve su valor como texto. Vacio si la pagina no responde
     * (p. ej. mientras navega del login al Home); la siguiente llamada se reconecta.
     */
    public synchronized Optional<String> evaluar(String expresion) {
        try {
            if (ws == null) {
                ws = abrir();
            }
            respuesta = new CompletableFuture<>();
            JsonObject params = new JsonObject();
            params.addProperty("expression", expresion);
            params.addProperty("returnByValue", true);
            JsonObject pedido = new JsonObject();
            pedido.addProperty("id", ++id);
            pedido.addProperty("method", "Runtime.evaluate");
            pedido.add("params", params);
            ws.sendText(GSON.toJson(pedido), true).get(ESPERA.toMillis(), TimeUnit.MILLISECONDS);
            JsonObject r = GSON.fromJson(respuesta.get(ESPERA.toMillis(), TimeUnit.MILLISECONDS), JsonObject.class);
            JsonElement valor = r.getAsJsonObject("result").getAsJsonObject("result").get("value");
            return valor == null || valor.isJsonNull() ? Optional.empty() : Optional.of(valor.getAsString());
        } catch (Exception e) {
            cerrarSocket();
            return Optional.empty();
        }
    }

    private WebSocket abrir() throws Exception {
        HttpResponse<String> lista = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + "/json"))
                .timeout(ESPERA).build(), HttpResponse.BodyHandlers.ofString());
        String url = null;
        for (JsonElement destino : GSON.fromJson(lista.body(), JsonArray.class)) {
            JsonObject d = destino.getAsJsonObject();
            if ("page".equals(d.get("type").getAsString()) && d.has("webSocketDebuggerUrl")) {
                url = d.get("webSocketDebuggerUrl").getAsString();
                break;
            }
        }
        if (url == null) {
            throw new IllegalStateException("El WebView no publica ninguna pagina en DevTools");
        }
        return http.newWebSocketBuilder().connectTimeout(ESPERA).buildAsync(URI.create(url), new Oyente())
                .get(ESPERA.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Completa la respuesta pendiente cuando llega el mensaje con su id (los eventos sin id se ignoran). */
    private final class Oyente implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence datos, boolean ultimo) {
            mensaje.append(datos);
            if (ultimo) {
                String texto = mensaje.toString();
                mensaje.setLength(0);
                JsonObject json = GSON.fromJson(texto, JsonObject.class);
                if (json.has("id") && json.get("id").getAsInt() == id && respuesta != null) {
                    respuesta.complete(texto);
                }
            }
            socket.request(1);
            return null;
        }
    }

    private void cerrarSocket() {
        if (ws != null) {
            ws.abort();
            ws = null;
        }
    }

    @Override
    public synchronized void close() {
        cerrarSocket();
        try {
            Adb.ejecutar(udid, "forward", "--remove", "tcp:" + puerto);
        } catch (IOException ignorada) {
            // El reenvio desaparece igual al reiniciar adb
        }
    }
}
