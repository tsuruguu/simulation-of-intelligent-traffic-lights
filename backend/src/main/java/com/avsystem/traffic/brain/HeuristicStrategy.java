package com.avsystem.traffic.brain;

import com.avsystem.traffic.model.Intersection;
import com.avsystem.traffic.model.TrafficLight;
import com.avsystem.traffic.model.Direction;
import com.avsystem.traffic.model.LightState;
import java.util.Arrays;
import java.util.List;

/**
 * Zaawansowana strategia Max-Pressure z adaptacyjnym tłumikiem EMA.
 * Eliminuje "szum" komunikacyjny i zapobiega zbyt częstym zmianom świateł
 * poprzez analizę trendu obciążenia zamiast wartości chwilowych.
 */
public class HeuristicStrategy implements TrafficStrategy {

    // Parametry czasowe
    private static final int MIN_PHASE_DURATION = 1;
    private static final int MAX_PHASE_DURATION = 40;

    // Parametry algorytmu Max-Pressure
    private static final double PRESSURE_THRESHOLD = 2.5;

    // Parametry tłumika (EMA)
    // ALPHA = 0.3 oznacza, że 30% wagi ma obecny krok, a 70% historia.
    private static final double ALPHA = 0.6;

    // Stanowiska pamięci dla filtrów EMA (niezależne dla każdej fazy)
    private double smoothedPressurePhase1 = -1.0;
    private double smoothedPressurePhase2 = -1.0;

    @Override
    public void optimizeTraffic(Intersection intersection, int currentStep) {
        // Faza 1: Pionowa (N-S), Faza 2: Pozioma (E-W)
        List<Direction> phase1 = Arrays.asList(Direction.NORTH, Direction.SOUTH);
        List<Direction> phase2 = Arrays.asList(Direction.EAST, Direction.WEST);

        // 1. Oblicz surowe ciśnienie (Raw Input)
        double rawP1 = calculatePhasePressure(intersection, phase1, currentStep);
        double rawP2 = calculatePhasePressure(intersection, phase2, currentStep);

        // 2. Aktualizacja filtrów EMA (Adaptive Smoothing)
        // Jeśli to pierwszy krok (-1.0), zainicjalizuj wartością surową, by uniknąć "rozgrzewania" filtra
        smoothedPressurePhase1 = (smoothedPressurePhase1 < 0) ? rawP1 : (ALPHA * rawP1) + ((1 - ALPHA) * smoothedPressurePhase1);
        smoothedPressurePhase2 = (smoothedPressurePhase2 < 0) ? rawP2 : (ALPHA * rawP2) + ((1 - ALPHA) * smoothedPressurePhase2);

        // 3. Pobranie stanu fizycznego skrzyżowania
        TrafficLight referenceLight = intersection.getTrafficLight(Direction.NORTH);
        boolean phase1IsGreen = referenceLight.allowsPassage();
        int duration = referenceLight.getDurationInCurrentState();

        // 4. Logika decyzyjna oparta na wygładzonych danych
        if (phase1IsGreen) {
            // Czy ciśnienie fazy bocznej (2) jest wyższe niż obecnej (1) o próg?
            if (shouldSwitch(duration, smoothedPressurePhase2, smoothedPressurePhase1)) {
                transition(intersection, phase1, phase2);
            }
        } else {
            // Czy ciśnienie fazy pionowej (1) dominuje?
            if (shouldSwitch(duration, smoothedPressurePhase1, smoothedPressurePhase2)) {
                transition(intersection, phase2, phase1);
            }
        }

        for (Direction dir : Direction.values()) {
            TrafficLight light = intersection.getTrafficLight(dir);
            if (light.getCurrentState() == LightState.RED) {
                // Włącz strzałkę, jeśli na drodze są auta skręcające w prawo,
                // a ciśnienie na drogach kolizyjnych jest niskie.
                int rightTurners = (int) intersection.getRoad(dir).getVehicleCount(); // Uproszczenie
                double conflictPressure = intersection.getConflictZonePressure(dir);

                // Jeśli mały ruch na poprzecznej, pozwól na warunkowy skręt
                light.setRightArrow(rightTurners > 0 && conflictPressure < 3.0);
            }
        }
    }

    private double calculatePhasePressure(Intersection intersection, List<Direction> directions, int currentStep) {
        return directions.stream()
                .mapToDouble(dir -> {
                    double count = intersection.getRoad(dir).getVehicleCount();
                    double avgWait = intersection.getRoad(dir).getAverageWaitTime(currentStep);
                    // Nieliniowa funkcja kosztu: priorytetyzuje duże zatory, ale reaguje na długie czekanie
                    return (count * 1.8) + (Math.log1p(avgWait) * 2.2);
                })
                .sum();
    }

    private boolean shouldSwitch(int currentDuration, double targetPressure, double activePressure) {
        if (currentDuration < MIN_PHASE_DURATION) return false;
        if (currentDuration > MAX_PHASE_DURATION) return true;

        // Decyzja na podstawie wygładzonego ciśnienia
        return targetPressure > (activePressure + PRESSURE_THRESHOLD);
    }

    private void transition(Intersection intersection, List<Direction> closing, List<Direction> opening) {
        // 1. Najpierw prosimy o zamknięcie obecnej fazy
        closing.forEach(dir -> intersection.getTrafficLight(dir).transitionTo(LightState.RED));

        // 2. Otwieramy nową fazę TYLKO JEŚLI stara jest już bezpiecznie CZERWONA
        // To zapobiega konfliktom w trakcie fazy YELLOW.
        boolean isSafeToOpen = closing.stream()
                .allMatch(dir -> intersection.getTrafficLight(dir).getCurrentState() == LightState.RED);

        if (isSafeToOpen) {
            opening.forEach(dir -> intersection.getTrafficLight(dir).transitionTo(LightState.GREEN));
        }
    }

    @Override
    public String getStrategyName() {
        return "Adaptive-EMA-MaxPressure";
    }
}