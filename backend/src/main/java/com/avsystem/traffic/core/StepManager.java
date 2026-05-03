package com.avsystem.traffic.core;

import com.avsystem.traffic.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Komponent odpowiedzialny za fizykę kroku symulacji.
 * Zarządza ruchem pojazdów, aktualizacją sygnalizacji oraz
 * weryfikacją bezpieczeństwa wewnątrz pojedynczej jednostki czasu.
 */
public class StepManager {

    private static final Logger LOGGER = Logger.getLogger(StepManager.class.getName());
    private final Intersection intersection;
    private final double stepTimeBudget;

    public StepManager(Intersection intersection, double stepTimeBudget) {
        this.intersection = intersection;
        this.stepTimeBudget = stepTimeBudget;
    }

    /**
     * Wykonuje pełną logikę jednego kroku symulacji.
     */
    public List<String> performStep(int currentStep) {
        List<String> vehiclesThatLeft = new ArrayList<>();

        updateTrafficLights();

        if (!intersection.isStateSafe()) {
            handleSafetyBreach(currentStep);
        }

        for (Direction dir : Direction.values()) {
            processRoadTraffic(dir, currentStep, vehiclesThatLeft);
        }

        return vehiclesThatLeft;
    }

    /**
     * Aktualizuje liczniki czasu dla wszystkich sygnalizatorów.
     */
    private void updateTrafficLights() {
        intersection.getAllTrafficLights().values().forEach(TrafficLight::incrementTime);
    }

    /**
     * Reaguje na wykrycie niebezpiecznego stanu skrzyżowania.
     * Zamiast tylko rzucać wyjątek, system wymusza "wszystko czerwone".
     */
    private void handleSafetyBreach(int currentStep) {
        LOGGER.severe("CRITICAL SAFETY BREACH at step " + currentStep + ": Emergency All-Red forced.");

        for (TrafficLight light : intersection.getAllTrafficLights().values()) {
            light.transitionTo(LightState.RED);
        }
    }

    /**
     * Zarządza ruchem pojazdów na konkretnej drodze.
     * Implementuje logikę opuszczania skrzyżowania oraz zbierania metryk frustracji.
     */
    private void processRoadTraffic(Direction dir, int currentStep, List<String> results) {
        Road road = intersection.getRoad(dir);
        TrafficLight light = intersection.getTrafficLight(dir);

        if (road.isEmpty()) return;

        double timeConsumed = 0.0;
        int movedInThisStep = 0;

        while (!road.isEmpty()) {
            Vehicle vehicle = road.peekVehicle();
            double travelCost = (timeConsumed == 0) ? 30.0 : 15.0;
            if (!vehicle.isGoingStraight()) travelCost += 15.0;

            if (light.allowsPassage(vehicle, intersection) && (movedInThisStep == 0 || timeConsumed + travelCost <= stepTimeBudget)){
                Vehicle departingVehicle = road.removeVehicle();
                results.add(departingVehicle.getId());
                timeConsumed += travelCost;
                movedInThisStep++;
            } else {
                if (movedInThisStep == 0) vehicle.incrementStopCount();
                break;
            }
        }
    }
}