package com.avsystem.traffic.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

class TrafficLightTest {
    private TrafficLight light;
    private Intersection intersection;

    @BeforeEach
    void setUp() {
        intersection = new Intersection();
        light = intersection.getTrafficLight(Direction.NORTH);
    }

    @Test
    void testYellowLightInterlock() {
        for(int i = 0; i < 4; i++) light.incrementTime();

        light.transitionTo(LightState.GREEN);
        assertEquals(LightState.GREEN, light.getCurrentState(), "Sygnalizator powinien być zielony");

        for(int i = 0; i < 6; i++) light.incrementTime();

        light.transitionTo(LightState.RED);

        assertEquals(LightState.YELLOW, light.getCurrentState(),
                "Sygnalizator musi wejść w fazę YELLOW przed RED!");
    }

    @Test
    void testGreenArrowLogic() {
        Vehicle rightTurner = new Vehicle("V1", Direction.NORTH, Direction.EAST, 0);
        Vehicle straightGoer = new Vehicle("V2", Direction.NORTH, Direction.SOUTH, 0);

        light.transitionTo(LightState.RED);
        light.setRightArrow(true);

        assertTrue(light.allowsPassage(rightTurner, intersection), "Auto skręcające w prawo powinno przejechać na zielonej strzałce.");
        assertFalse(light.allowsPassage(straightGoer, intersection), "Auto jadące prosto nie może jechać na samej strzałce.");
    }

    @Test
    @DisplayName("Should forbid U-turn on green arrow but allow it on full green")
    void testUturnOnGreenArrow() {
        Vehicle uturnVehicle = new Vehicle("V1", Direction.NORTH, Direction.NORTH, 0);
        Vehicle rightTurnVehicle = new Vehicle("V2", Direction.NORTH, Direction.EAST, 0);

        light.transitionTo(LightState.RED);
        light.setRightArrow(true);

        assertTrue(light.allowsPassage(rightTurnVehicle, intersection), "Skręt w prawo powinien być dozwolony na strzałce");
        assertFalse(light.allowsPassage(uturnVehicle, intersection), "Zawracanie powinno być ZABRONIONE na strzałce");

        for(int i = 0; i < 10; i++) light.incrementTime();
        light.transitionTo(LightState.GREEN);
        light.incrementTime();

        assertTrue(light.allowsPassage(uturnVehicle, intersection), "Zawracanie powinno być dozwolone na pełnym zielonym");
    }
}