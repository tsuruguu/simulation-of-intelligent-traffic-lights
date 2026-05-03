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

    private static final int YELLOW_DURATION = 1;
    private static final int MIN_GREEN_DURATION = 3;
    private static final int MIN_RED_DURATION = 3;

    public TrafficLight(Direction direction) {
        this.direction = direction;
        this.currentState = LightState.RED;
        this.rightArrowActive = false;
        this.durationInCurrentState = MIN_RED_DURATION;
    }

    public void incrementTime() {
        this.durationInCurrentState++;

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

    public boolean isBlocking() {
        return this.currentState == LightState.GREEN || this.currentState == LightState.YELLOW;
    }

    /**
     * Realizuje bezpieczne przejście stanów (FSM).
     * Implementuje zasadę nieuchronności fazy żółtej (Interlock).
     */
    public void transitionTo(LightState requestedState) {
        if (this.currentState == requestedState) return;
        if (!canChangeState()) return;

        if (this.currentState == LightState.GREEN && requestedState == LightState.RED) {
            this.currentState = LightState.YELLOW;
            this.durationInCurrentState = 0;
            this.rightArrowActive = false;
            return;
        }

        if (this.currentState == LightState.YELLOW && requestedState == LightState.RED) {
            if (durationInCurrentState >= YELLOW_DURATION) {
                this.currentState = LightState.RED;
                this.durationInCurrentState = 0;
            }
            return;
        }

        switch (this.currentState) {
            case RED:
                if (requestedState == LightState.GREEN) {
                    this.currentState = LightState.GREEN;
                    this.durationInCurrentState = 0;
                }
                break;

            case GREEN:
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
     * Rozszerzona logika wjazdu uwzględniająca ustępowanie pierwszeństwa.
     */
    public boolean allowsPassage(Vehicle vehicle, Intersection intersection) {
        if (this.currentState == LightState.RED && !this.rightArrowActive) return false;

        if (this.currentState == LightState.GREEN && vehicle.isTurningLeft()) {
            Direction oppositeDir = getOpposite(this.direction);
            Road oppositeRoad = intersection.getRoad(oppositeDir);
            TrafficLight oppositeLight = intersection.getTrafficLight(oppositeDir);

            if (oppositeLight.allowsPassage() && !oppositeRoad.isEmpty()) {
                Vehicle oncoming = oppositeRoad.peekVehicle();
                if (oncoming.isGoingStraight() || oncoming.isTurningRight()) {
                    return false;
                }
            }
        }

        if (this.currentState == LightState.RED && this.rightArrowActive) {
            return vehicle.isTurningRight();
        }

        return this.currentState == LightState.GREEN;
    }

    private Direction getOpposite(Direction dir) {
        return Direction.values()[(dir.ordinal() + 2) % 4];
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
            utility = (currentRoadPressure * 2.5) - (conflictPressure * 1.5);
            utility += Math.min(durationInCurrentState, 10) * 0.2;
        } else {
            utility = (conflictPressure * 2.0) - (currentRoadPressure * 1.8);
            if (this.rightArrowActive) utility += (rightTurners * 1.5);
        }

        return utility;
    }


    public boolean isRightArrowActive() { return rightArrowActive; }
    public LightState getCurrentState() { return currentState; }
    public int getDurationInCurrentState() { return durationInCurrentState; }
    public boolean isInTransition() { return currentState == LightState.YELLOW; }
}