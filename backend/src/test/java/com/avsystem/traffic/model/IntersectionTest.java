package com.avsystem.traffic.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

class IntersectionTest {

    @Test
    void testSafetyInterlock() {
        Intersection intersection = new Intersection();
        TrafficLight north = intersection.getTrafficLight(Direction.NORTH);
        TrafficLight east = intersection.getTrafficLight(Direction.EAST);

        // 1. "Odczekaj", aż czerwone światło pozwoli na zmianę (MIN_RED_DURATION = 3)
        for(int i = 0; i < 4; i++) intersection.getAllTrafficLights().values().forEach(TrafficLight::incrementTime);

        // 2. Teraz przejście na GREEN zadziała
        north.transitionTo(LightState.GREEN);
        east.transitionTo(LightState.GREEN);

        // 3. Inkrymentuj czas, aby stan GREEN został w pełni uznany
        intersection.getAllTrafficLights().values().forEach(TrafficLight::incrementTime);

        assertFalse(intersection.isStateSafe(), "Wykryto konflikt N i E!");
    }

    @Test
    @DisplayName("Safety Interlock should detect N-S and E-W simultaneous green as unsafe")
    void testExtremeSafetyViolation() {
        Intersection intersection = new Intersection();

        // Odczekaj na odblokowanie RED -> GREEN
        for(int i = 0; i < 4; i++) intersection.getAllTrafficLights().values().forEach(TrafficLight::incrementTime);

        intersection.getTrafficLight(Direction.NORTH).transitionTo(LightState.GREEN);
        intersection.getTrafficLight(Direction.EAST).transitionTo(LightState.GREEN);

        intersection.getAllTrafficLights().values().forEach(TrafficLight::incrementTime);

        assertFalse(intersection.isStateSafe(), "Skrzyżowanie musi uznać za niebezpieczne zielone dla kierunków prostopadłych!");
    }
}