package com.avsystem.traffic.brain;

import com.avsystem.traffic.model.Intersection;
import com.avsystem.traffic.model.Direction;
import java.util.logging.Logger;

/**
 * Meta-algorytm (Decorator) nadzorujący pracę sieci neuronowej.
 * Implementuje koncepcję Explainable AI (XAI) oraz Safety Interlock.
 * Jeśli sieć neuronowa podejmie decyzję ryzykowną lub zignoruje krytyczne zatory,
 * system automatycznie przełącza się na tryb awaryjny (Override).
 */

public class SafetyGuardedStrategy implements TrafficStrategy {

    private static final int MAX_INTERVENTIONS = 5;

    private int interventionCount = 0;
    private boolean permanentFailSafeActive = false;

    private static final Logger LOGGER = Logger.getLogger(SafetyGuardedStrategy.class.getName());

    private static final int CRITICAL_WAIT_THRESHOLD = 12;

    private final TrafficStrategy primaryAI;
    private final HeuristicStrategy safetyHeuristic;

    /**
     * @param primaryAI Główny mózg (np. NeuralNetworkStrategy).
     * @param safetyHeuristic Strażnik (np. HeuristicStrategy).
     */
    public SafetyGuardedStrategy(TrafficStrategy primaryAI, HeuristicStrategy safetyHeuristic) {
        this.primaryAI = primaryAI;
        this.safetyHeuristic = safetyHeuristic;
    }

    @Override
    public void init() {
        primaryAI.init();
        safetyHeuristic.init();
    }

    @Override
    public void optimizeTraffic(Intersection intersection, int currentStep) {
        if (permanentFailSafeActive) {
            safetyHeuristic.optimizeTraffic(intersection, currentStep);
            return;
        }

        Direction starvingRoad = findStarvingRoad(intersection, currentStep);

        if (starvingRoad != null) {
            interventionCount++;

            if (interventionCount >= MAX_INTERVENTIONS) {
                permanentFailSafeActive = true;
                LOGGER.severe("PERMANENT FAIL-SAFE ACTIVATED: Neural Network disabled due to excessive safety overrides. " +
                        "System switched to stable Heuristic control.");
            }

            LOGGER.warning(String.format(
                    "SAFETY OVERRIDE [%d/%d] at step %d: Road %s exceeded critical wait time. " +
                            "Neural Network control suspended.",
                    interventionCount, MAX_INTERVENTIONS, currentStep, starvingRoad));

            safetyHeuristic.optimizeTraffic(intersection, currentStep);
        } else {
            primaryAI.optimizeTraffic(intersection, currentStep);
        }
    }

    /**
     * Analizuje wszystkie wloty w poszukiwaniu pojazdów, które czekają zbyt długo.
     */
    private Direction findStarvingRoad(Intersection intersection, int currentStep) {
        for (Direction dir : Direction.values()) {
            double avgWait = intersection.getRoad(dir).getAverageWaitTime(currentStep);
            if (avgWait > CRITICAL_WAIT_THRESHOLD) {
                return dir;
            }
        }
        return null;
    }

    @Override
    public String getStrategyName() {
        return "SafetyGuarded-" + primaryAI.getStrategyName();
    }
}