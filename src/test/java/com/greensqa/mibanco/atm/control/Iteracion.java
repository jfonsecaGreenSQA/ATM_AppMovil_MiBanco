package com.greensqa.mibanco.atm.control;

import com.greensqa.mibanco.atm.data.Enmascarador;
import com.greensqa.mibanco.atm.data.TipoDocumento;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Fila del control ya evaluada por el motor de reglas, con sus datos resueltos y su resultado. */
public class Iteracion {

    /**
     * PENDIENTE: no se ejecuto porque el proceso se detuvo por indisponibilidad; reprogramar.
     */
    public enum Estado { LISTA, OMITIDA, NO_EJECUTADA, EXITOSA, FALLIDA, PENDIENTE }

    private final FilaControl fila;
    private CasoPrueba caso;
    private Estado estado;
    private String detalle = "";

    private TipoDocumento tipoDocumento;
    private boolean documentoAutomatico;
    private String numDocumento;
    private boolean documentoReservado;
    private boolean reservaDeRegistrado;
    private String clave;
    private String origenClave = "";
    private String otp;

    private LocalDateTime inicio;
    private Duration duracion;
    private Path carpetaEvidencias;
    private boolean porIndisponibilidad;

    public Iteracion(FilaControl fila) {
        this.fila = fila;
    }

    // --- decisiones del motor de reglas ---

    void omitir(String motivo) {
        estado = Estado.OMITIDA;
        detalle = motivo;
    }

    void rechazar(String motivo) {
        estado = Estado.NO_EJECUTADA;
        detalle = motivo;
    }

    void aprobar(CasoPrueba caso) {
        this.caso = caso;
        estado = Estado.LISTA;
    }

    void datosDocumento(TipoDocumento tipo, String numero) {
        this.tipoDocumento = tipo;
        this.documentoAutomatico = numero == null;
        this.numDocumento = numero;
    }

    /** @param clave null cuando se toma del listado de clientes al ejecutar (login) */
    void datosClave(String clave, String origen) {
        this.clave = clave;
        this.origenClave = origen;
    }

    void datosOtp(String otp) {
        this.otp = otp;
    }

    void identificarCaso(CasoPrueba caso) {
        this.caso = caso;
    }

    // --- ciclo de ejecucion ---

    public void iniciar(Path carpetaEvidencias) {
        this.carpetaEvidencias = carpetaEvidencias;
        this.inicio = LocalDateTime.now();
    }

    public void finalizar(boolean exitosa, String detalle) {
        this.estado = exitosa ? Estado.EXITOSA : Estado.FALLIDA;
        this.detalle = detalle == null ? "" : detalle;
        this.duracion = Duration.between(inicio, LocalDateTime.now());
    }

    /** El aviso de indisponibilidad persistio tras los reintentos: FALLIDA, distinguible para detener la ejecucion. */
    public void fallarPorIndisponibilidad(String aviso) {
        this.estado = Estado.FALLIDA;
        this.porIndisponibilidad = true;
        this.detalle = "Indisponibilidad del sistema persistente tras los reintentos: " + aviso;
        this.duracion = Duration.between(inicio, LocalDateTime.now());
    }

    public void pendiente(String motivo) {
        this.estado = Estado.PENDIENTE;
        this.detalle = motivo;
    }

    public void noEjecutada(String motivo) {
        this.estado = Estado.NO_EJECUTADA;
        this.detalle = motivo;
        if (inicio != null) {
            this.duracion = Duration.between(inicio, LocalDateTime.now());
        }
    }

    /** Documento tomado del listado de clientes y marcado EN_USO. */
    public void reservarDocumento(String numero) {
        this.numDocumento = numero;
        this.documentoReservado = true;
    }

    /** Cliente ya registrado tomado del listado para ingresar; el login no cambia su estado, por eso no se reserva. */
    public void usarClienteRegistrado(String numero, String clave) {
        this.numDocumento = numero;
        this.clave = clave;
    }

    /** Cliente registrado tomado del listado para recuperar su clave; su clave (la nueva) es la de la iteracion. */
    public void usarDocumento(String numero) {
        this.numDocumento = numero;
    }

    /** Clave nueva generada al ejecutar, cuando depende de la clave actual del cliente (recuperacion de clave). */
    public void asignarClave(String clave) {
        this.clave = clave;
    }

    /**
     * Cliente REGISTRADO marcado EN_USO mientras su clave puede estar cambiando (recuperacion de clave). Si la iteracion
     * falla, no vuelve a REGISTRADO ni a DISPONIBLE: no se sabe que clave quedo.
     */
    public void reservarClienteRegistrado(String numero) {
        this.numDocumento = numero;
        this.documentoReservado = true;
        this.reservaDeRegistrado = true;
    }

    /** El documento ya quedo con su estado final en el listado (se conserva como documento usado). */
    public void liberarDocumento() {
        this.documentoReservado = false;
        this.reservaDeRegistrado = false;
    }

    /** El documento no sirvio (p. ej. ya registrado); con AUTO se tomara otro del listado. */
    public void descartarDocumento() {
        this.documentoReservado = false;
        if (documentoAutomatico) {
            this.numDocumento = null;
        }
    }

    /** Valores que no deben aparecer en claro en adjuntos de texto. */
    public List<String> valoresSensibles() {
        List<String> v = new ArrayList<>();
        if (numDocumento != null) {
            v.add(numDocumento);
        }
        if (clave != null) {
            v.add(clave);
        }
        return v;
    }

    public String resumenEnmascarado() {
        return "Caso: " + (caso == null ? fila.caso() : caso.textoCatalogo())
                + "\nIdPrueba: " + fila.idPrueba()
                + "\nTipoDocumento: " + tipoDocumento
                + "\nNumDocumento: " + (numDocumento == null ? "AUTO (listado de clientes)" : Enmascarador.documento(numDocumento))
                + "\nClave: " + origenClave + " (****)"
                + "\nOTP: " + (otp == null ? "" : "*".repeat(otp.length()))
                + "\nObservaciones: " + fila.observaciones();
    }

    public FilaControl fila() { return fila; }
    public CasoPrueba caso() { return caso; }
    public Estado estado() { return estado; }
    public String detalle() { return detalle; }
    public String idPrueba() { return fila.idPrueba(); }
    public TipoDocumento tipoDocumento() { return tipoDocumento; }
    public boolean documentoAutomatico() { return documentoAutomatico; }
    public String numDocumento() { return numDocumento; }
    public boolean documentoReservado() { return documentoReservado; }
    public boolean reservaDeRegistrado() { return reservaDeRegistrado; }
    public String clave() { return clave; }
    public String otp() { return otp; }
    public LocalDateTime inicio() { return inicio; }
    public Duration duracion() { return duracion; }
    public Path carpetaEvidencias() { return carpetaEvidencias; }
    public boolean porIndisponibilidad() { return porIndisponibilidad; }
}
