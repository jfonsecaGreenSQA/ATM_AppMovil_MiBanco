package com.greensqa.mibanco.atm.screens;

import java.time.Duration;
import java.util.Optional;

/** Observacion en segundo plano de la carga del Home (skeleton y montos), iniciada antes de ingresar. */
public interface ObservacionCarga extends AutoCloseable {

    /** Espera a que el Home termine de cargar los montos o a que venza la espera. */
    void esperarFin(Duration espera);

    /** Maximo de indicadores "Cargando..." vistos a la vez; 0 = no se vio el skeleton. */
    int maximoIndicadores();

    /** Los montos aparecieron sin que se viera antes el skeleton. */
    boolean montosAntesDelSkeleton();

    /** Al terminar la observacion habia montos y ningun indicador de carga. */
    boolean terminoConMontos();

    /** Captura de pantalla tomada al ver el skeleton por primera vez. */
    Optional<byte[]> capturaSkeleton();

    @Override
    void close();
}
