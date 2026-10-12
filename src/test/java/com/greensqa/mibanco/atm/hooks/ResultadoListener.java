package com.greensqa.mibanco.atm.hooks;

import com.greensqa.mibanco.atm.context.ContextoIteracion;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;

/** Plugin de Cucumber: informa al orquestador el paso en curso y el resultado del escenario. */
public class ResultadoListener implements ConcurrentEventListener {

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepStarted.class, e -> {
            if (e.getTestStep() instanceof PickleStepTestStep paso) {
                ContextoIteracion.pasoActual(paso.getStep().getKeyword() + paso.getStep().getText());
            }
        });
        publisher.registerHandlerFor(TestStepFinished.class, e -> {
            if (e.getTestStep() instanceof PickleStepTestStep paso && e.getResult().getStatus() == Status.FAILED) {
                ContextoIteracion.pasoFallido(paso.getStep().getKeyword() + paso.getStep().getText());
            }
        });
        publisher.registerHandlerFor(TestCaseFinished.class, e -> {
            Result r = e.getResult();
            String error = r.getError() == null ? "" : primeraLinea(r.getError());
            ContextoIteracion.registrarResultado(r.getStatus() == Status.PASSED, error);
        });
    }

    private static String primeraLinea(Throwable t) {
        String m = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        return m.lines().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse(m).trim();
    }
}
