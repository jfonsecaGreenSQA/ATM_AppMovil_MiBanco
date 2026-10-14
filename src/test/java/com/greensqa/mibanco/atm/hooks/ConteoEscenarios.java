package com.greensqa.mibanco.atm.hooks;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCaseStarted;

import java.util.HashMap;
import java.util.Map;

/**
 * Plugin de Cucumber para la verificacion previa (dry-run): cuenta cuantos escenarios lleva cada tag, incluidos los
 * heredados del Feature y de los Examples. Cada fila del control debe corresponder a un solo escenario.
 */
public class ConteoEscenarios implements ConcurrentEventListener {

    private static final Map<String, Integer> POR_TAG = new HashMap<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class,
                e -> e.getTestCase().getTags().forEach(t -> POR_TAG.merge(t, 1, Integer::sum)));
    }

    public static void reiniciar() {
        POR_TAG.clear();
    }

    public static int escenarios(String tag) {
        return POR_TAG.getOrDefault(tag, 0);
    }
}
