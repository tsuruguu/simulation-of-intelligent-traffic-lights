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

    private static final int MIN_PHASE_DURATION = 5;
    private static final int MAX_PHASE_DURATION = 40;

    private static final double PRESSURE_THRESHOLD = 2.5;

    private static final double ALPHA = 0.6;

    private double smoothedPressurePhase1 = -1.0;
    private double smoothedPressurePhase2 = -1.0;

    @Override
    public void optimizeTraffic(Intersection intersection, int currentStep) {
        List<Direction> phase1 = Arrays.asList(Direction.NORTH, Direction.SOUTH);
        List<Direction> phase2 = Arrays.asList(Direction.EAST, Direction.WEST);

        double rawP1 = calculatePhasePressure(intersection, phase1, currentStep);
        double rawP2 = calculatePhasePressure(intersection, phase2, currentStep);

        smoothedPressurePhase1 = (smoothedPressurePhase1 < 0) ? rawP1 : (ALPHA * rawP1) + ((1 - ALPHA) * smoothedPressurePhase1);
        smoothedPressurePhase2 = (smoothedPressurePhase2 < 0) ? rawP2 : (ALPHA * rawP2) + ((1 - ALPHA) * smoothedPressurePhase2);

        TrafficLight referenceLight = intersection.getTrafficLight(Direction.NORTH);
        boolean phase1IsGreen = referenceLight.allowsPassage();
        int duration = referenceLight.getDurationInCurrentState();

        if (phase1IsGreen) {
            if (shouldSwitch(duration, smoothedPressurePhase2, smoothedPressurePhase1)) {
                transition(intersection, phase1, phase2);
            }
        } else {
            if (shouldSwitch(duration, smoothedPressurePhase1, smoothedPressurePhase2)) {
                transition(intersection, phase2, phase1);
            }
        }

        for (Direction dir : Direction.values()) {
            TrafficLight light = intersection.getTrafficLight(dir);
            if (light.getCurrentState() == LightState.RED) {
                int rightTurners = (int) intersection.getRoad(dir).getVehicleCount();
                double conflictPressure = intersection.getConflictZonePressure(dir);

                light.setRightArrow(rightTurners > 0 && conflictPressure < 3.0);
            }
        }
    }

    private double calculatePhasePressure(Intersection intersection, List<Direction> directions, int currentStep) {
        return directions.stream()
                .mapToDouble(dir -> {
                    double count = intersection.getRoad(dir).getVehicleCount();
                    double avgWait = intersection.getRoad(dir).getAverageWaitTime(currentStep);
                    return (count * 1.8) + (Math.log1p(avgWait) * 2.2);
                })
                .sum();
    }

    private boolean shouldSwitch(int currentDuration, double targetPressure, double activePressure) {
        if (currentDuration < MIN_PHASE_DURATION) return false;
        if (currentDuration > MAX_PHASE_DURATION) return true;

        return targetPressure > (activePressure + PRESSURE_THRESHOLD);
    }

    private void transition(Intersection intersection, List<Direction> closing, List<Direction> opening) {
        closing.forEach(dir -> intersection.getTrafficLight(dir).transitionTo(LightState.RED));

        boolean isSafeToOpen = closing.stream()
                .noneMatch(dir -> intersection.getTrafficLight(dir).isBlocking());

        if (isSafeToOpen) {
            opening.forEach(dir -> intersection.getTrafficLight(dir).transitionTo(LightState.GREEN));
        } else {
        }
    }

    @Override
    public String getStrategyName() {
        return "Adaptive-EMA-MaxPressure";
    }
}