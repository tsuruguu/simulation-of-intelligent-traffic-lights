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

    public StepManager(Intersection intersection) {
        this.intersection = intersection;
    }

    /**
     * Wykonuje pełną logikę jednego kroku symulacji.
     */
    public List<String> performStep(int currentStep) {
        List<String> vehiclesThatLeft = new ArrayList<>();

        // 1. Aktualizacja stanu infrastruktury (inkrementacja czasu trwania faz)
        updateTrafficLights();

        // 2. Weryfikacja bezpieczeństwa (Safety Interlock Check)
        if (!intersection.isStateSafe()) {
            handleSafetyBreach(currentStep);
        }

        // 3. Procesowanie ruchu na każdej drodze
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

        // Wymuszamy przejście wszystkich świateł na RED (poprzez YELLOW jeśli trzeba)
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

        double stepTimeBudget = 180.0; // Zwiększony lekko budżet
        double timeConsumed = 0.0;
        int movedInThisStep = 0;

        while (!road.isEmpty()) {
            Vehicle vehicle = road.peekVehicle();
            double travelCost = (timeConsumed == 0) ? 30.0 : 15.0;
            if (!vehicle.isGoingStraight()) travelCost += 15.0;

            // ZMIANA: Pozwól jechać jeśli (to pierwsze auto) LUB (mieści się w budżecie)
            if (light.allowsPassage(vehicle) && (movedInThisStep == 0 || timeConsumed + travelCost <= stepTimeBudget)) {
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