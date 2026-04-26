package com.avsystem.traffic.model;

/**
 * Inteligenty sygnalizator świetlny działający jako Maszyna Stanów (FSM).
 * Obsługuje asymetryczne fazy, zielone strzałki oraz dostarcza zaawansowaną
 * telemetrię dla agentów AI.
 */
public class TrafficLight {

    private LightState currentState;
    private boolean rightArrowActive;
    private int durationInCurrentState;
    private final Direction direction;

    // Stałe konfiguracyjne - "Złote parametry" inżynierii ruchu
    private static final int YELLOW_DURATION = 2;   // Czas czyszczenia skrzyżowania
    // W TrafficLight.java zmień stałe:
    private static final int MIN_GREEN_DURATION = 1; // Skrócone z 5
    private static final int MIN_RED_DURATION = 1;   // Skrócone z 3

    public TrafficLight(Direction direction) {
        this.direction = direction;
        this.currentState = LightState.RED;
        this.rightArrowActive = false;
        this.durationInCurrentState = MIN_RED_DURATION;
    }

    // W TrafficLight.java popraw metodę incrementTime:
    public void incrementTime() {
        this.durationInCurrentState++;

        // AUTOMATYCZNE PRZEJŚCIE: Jeśli jesteśmy w YELLOW i czas minął, przejdź do RED
        if (this.currentState == LightState.YELLOW && durationInCurrentState >= YELLOW_DURATION) {
            this.currentState = LightState.RED;
            this.durationInCurrentState = 0;
        }
    }

    /**
     * Zmienia stan strzałki.
     * Dzięki asymetrii, strzałka może być aktywna niezależnie od innych sygnalizatorów.
     */
    public void setRightArrow(boolean active) {
        this.rightArrowActive = active;
    }

    /**
     * Realizuje bezpieczne przejście stanów (FSM).
     * Implementuje zasadę nieuchronności fazy żółtej (Interlock).
     */
    public void transitionTo(LightState requestedState) {
        if (this.currentState == requestedState) return;
        if (!canChangeState()) return;

        // 1. Zabezpieczenie: Z GREEN (lub YELLOW w trakcie zmiany) do RED zawsze przez YELLOW
        if (this.currentState == LightState.GREEN && requestedState == LightState.RED) {
            this.currentState = LightState.YELLOW;
            this.durationInCurrentState = 0;
            this.rightArrowActive = false;
            return;
        }

        // 2. Obsługa przejścia z YELLOW do RED, gdy czas minął
        if (this.currentState == LightState.YELLOW && requestedState == LightState.RED) {
            if (durationInCurrentState >= YELLOW_DURATION) {
                this.currentState = LightState.RED;
                this.durationInCurrentState = 0;
            }
            return; // Czekamy w YELLOW aż minie czas
        }

        // 3. Pozostałe bezpośrednie przejścia (np. RED -> GREEN)
        switch (this.currentState) {
            case RED:
                if (requestedState == LightState.GREEN) {
                    this.currentState = LightState.GREEN;
                    this.durationInCurrentState = 0;
                }
                break;

            case GREEN:
                // Jeśli ktoś poprosił o YELLOW bezpośrednio
                if (requestedState == LightState.YELLOW) {
                    this.currentState = LightState.YELLOW;
                    this.durationInCurrentState = 0;
                    this.rightArrowActive = false;
                }
                break;
        }
    }

    /**
     * Sprawdza, czy sygnalizator może w danej chwili zmienić stan.
     * Zapobiega sytuacjom, w których AI próbuje przełączać światła co sekundę.
     */
    public boolean canChangeState() {
        switch (currentState) {
            case GREEN: return durationInCurrentState >= MIN_GREEN_DURATION;
            case RED: return durationInCurrentState >= MIN_RED_DURATION;
            case YELLOW: return durationInCurrentState >= YELLOW_DURATION;
            default: return true;
        }
    }

    /**
     * Decyzja o przejeździe zależna od intencji pojazdu (Zielona Strzałka).
     */
    public boolean allowsPassage(Vehicle vehicle) {
        // Jeśli jest zielone, jedziemy!
        if (this.currentState == LightState.GREEN) return true;

        // Jeśli czerwone, sprawdzamy strzałkę (tylko dla skrętu w prawo)
        if (this.rightArrowActive && vehicle.isTurningRight()) return true;

        return false;
    }

    /**
     * Wersja bezargumentowa dla ogólnej weryfikacji bezpieczeństwa (Intersection).
     * Zwraca true tylko dla pełnego zielonego.
     */
    public boolean allowsPassage() {
        return this.currentState == LightState.GREEN;
    }

    /**
     * Oblicza matematyczną wartość obecnego stanu (Utility Function).
     * Wykorzystywane przez NeuralNetworkStrategy do oceny nagrody (Reward).
     */
    public double calculateStateUtility(double currentRoadPressure, double conflictPressure, int rightTurners) {
        double utility = 0.0;

        if (this.currentState == LightState.GREEN) {
            // Zysk z przepływu minus strata dla blokowanych kierunków
            utility = (currentRoadPressure * 2.5) - (conflictPressure * 1.5);
            // Bonus za utrzymanie ciągłości
            utility += Math.min(durationInCurrentState, 10) * 0.2;
        } else {
            // Strata z blokowania minus zysk dla innych
            utility = (conflictPressure * 2.0) - (currentRoadPressure * 1.8);
            // Bonus za strzałkę jeśli są chętni do skrętu
            if (this.rightArrowActive) utility += (rightTurners * 1.5);
        }

        return utility;
    }

    // --- Gettery ---

    public boolean isRightArrowActive() { return rightArrowActive; }
    public LightState getCurrentState() { return currentState; }
    public int getDurationInCurrentState() { return durationInCurrentState; }
    public boolean isInTransition() { return currentState == LightState.YELLOW; }
}