package com.avsystem.traffic.dto;

import com.avsystem.traffic.core.SimulationEngine;

/**
 * DTO dla komendy 'step'.
 * Reprezentuje pojedynczy impuls zegarowy symulacji, wyzwalający logikę
 * ruchu pojazdów i zmiany stanów inteligentnych świateł.
 */
public class StepCommandDTO extends CommandDTO {

    /**
     * Konstruktor domyślny wymagany przez mechanizm refleksji Jacksona.
     */
    public StepCommandDTO() {
        super();
    }

    /**
     * Walidacja komendy step.
     * Komenda 'step' bez parametrów jest zawsze semantycznie poprawna.
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * Implementacja wzorca Command.
     * Przekazuje sterowanie do silnika symulacji w celu wykonania kroku.
     * * @param engine Referencja do orkiestratora symulacji.
     * @return Wynik kroku (StepStatusDTO), rzutowany generycznie.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(SimulationEngine engine) {
        // Wykonujemy krok i zwracamy status (wymaga, by engine.handleStep() zwracał StepStatusDTO)
        return (T) engine.handleStep();
    }
}