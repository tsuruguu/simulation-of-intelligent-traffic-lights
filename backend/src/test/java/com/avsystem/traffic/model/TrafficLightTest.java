package com.avsystem.traffic.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

class TrafficLightTest {
    private TrafficLight light;

    @BeforeEach
    void setUp() {
        light = new TrafficLight(Direction.NORTH);
    }

    @Test
    void testYellowLightInterlock() {
        // 0. Najpierw musimy odczekać fazę RED, żeby móc przejść na GREEN
        // (MIN_RED_DURATION = 3)
        for(int i = 0; i < 4; i++) light.incrementTime();

        // 1. Teraz przejście na zielone zadziała
        light.transitionTo(LightState.GREEN);
        assertEquals(LightState.GREEN, light.getCurrentState(), "Sygnalizator powinien być zielony");

        // Daj mu "pożyć" w zielonym (MIN_GREEN_DURATION = 5)
        for(int i = 0; i < 6; i++) light.incrementTime();

        // 2. Poproś o czerwone - system POWINIEN wymusić żółte
        light.transitionTo(LightState.RED);

        assertEquals(LightState.YELLOW, light.getCurrentState(),
                "Sygnalizator musi wejść w fazę YELLOW przed RED!");
    }

    @Test
    void testGreenArrowLogic() {
        Vehicle rightTurner = new Vehicle("V1", Direction.NORTH, Direction.EAST, 0);
        Vehicle straightGoer = new Vehicle("V2", Direction.NORTH, Direction.SOUTH, 0); // Prosto

        light.transitionTo(LightState.RED);
        light.setRightArrow(true);

        assertTrue(light.allowsPassage(rightTurner), "Auto skręcające w prawo powinno przejechać na zielonej strzałce.");
        assertFalse(light.allowsPassage(straightGoer), "Auto jadące prosto nie może jechać na samej strzałce.");
    }

    @Test
    @DisplayName("Should forbid U-turn on green arrow but allow it on full green")
    void testUturnOnGreenArrow() {
        TrafficLight light = new TrafficLight(Direction.NORTH);
        Vehicle uturnVehicle = new Vehicle("V1", Direction.NORTH, Direction.NORTH, 0);
        Vehicle rightTurnVehicle = new Vehicle("V2", Direction.NORTH, Direction.EAST, 0);

        // 1. Czerwone + Strzałka
        light.transitionTo(LightState.RED);
        light.setRightArrow(true);

        assertTrue(light.allowsPassage(rightTurnVehicle), "Skręt w prawo powinien być dozwolony na strzałce");
        assertFalse(light.allowsPassage(uturnVehicle), "Zawracanie powinno być ZABRONIONE na strzałce");

        // 2. Pełne zielone
        for(int i = 0; i < 4; i++) light.incrementTime();

        light.transitionTo(LightState.GREEN);
        light.incrementTime(); // Aktywuj czas w zielonym
        assertTrue(light.allowsPassage(uturnVehicle), "Zawracanie powinno być dozwolone na pełnym zielonym");
    }
}
