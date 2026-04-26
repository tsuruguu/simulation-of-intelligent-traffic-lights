package com.avsystem.traffic.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

class VehicleTest {

    @Test
    void testRightTurnAlgebra() {
        // Test dla każdego kierunku (Ruch prawostronny: N->E, E->S, S->W, W->N)
        Vehicle v1 = new Vehicle("V1", Direction.NORTH, Direction.EAST, 0);
        Vehicle v2 = new Vehicle("V2", Direction.EAST, Direction.SOUTH, 0);
        Vehicle v3 = new Vehicle("V3", Direction.SOUTH, Direction.WEST, 0);
        Vehicle v4 = new Vehicle("V4", Direction.WEST, Direction.NORTH, 0);

        assertTrue(v1.isTurningRight());
        assertTrue(v2.isTurningRight());
        assertTrue(v3.isTurningRight());
        assertTrue(v4.isTurningRight());
    }

    @Test
    void testStraightAndLeft() {
        Vehicle straight = new Vehicle("V1", Direction.NORTH, Direction.SOUTH, 0);
        Vehicle left = new Vehicle("V2", Direction.NORTH, Direction.WEST, 0);

        assertFalse(straight.isTurningRight());
        assertFalse(left.isTurningRight());
    }

    @Test
    void testUturnLogic() {
        // Start i End to samo = zawracanie
        Vehicle uturn = new Vehicle("V_UTURN", Direction.NORTH, Direction.NORTH, 0);

        assertTrue(uturn.isTurningBack(), "Pojazd powinien zostać rozpoznany jako zawracający");
        assertFalse(uturn.isTurningRight(), "Zawracanie to nie skręt w prawo");
        assertFalse(uturn.isGoingStraight(), "Zawracanie to nie jazda prosto");
    }

    @Test
    @DisplayName("Modular algebra should correctly identify maneuvers for all directions")
    void testAllManeuvers() {
        // North -> South (Straight)
        assertTrue(new Vehicle("V", Direction.NORTH, Direction.SOUTH, 0).isGoingStraight());
        // West -> East (Straight)
        assertTrue(new Vehicle("V", Direction.WEST, Direction.EAST, 0).isGoingStraight());

        // North -> West (Right turn - Polish traffic)
        // Uwaga: Zależy od Twojej definicji Direction. Jeśli North=0, East=1, South=2, West=3:
        // Skręt w prawo z North (0) to East (1).
        assertTrue(new Vehicle("V", Direction.NORTH, Direction.EAST, 0).isTurningRight());

        // Zawracanie (U-turn)
        assertTrue(new Vehicle("V", Direction.SOUTH, Direction.SOUTH, 0).isTurningBack());
    }

    @Test
    @DisplayName("Telemetry O(1) should match manual calculations after multiple operations")
    void testTelemetryAccuracy() {
        Road road = new Road(Direction.NORTH);
        road.addVehicle(new Vehicle("V1", Direction.NORTH, Direction.SOUTH, 10)); // entry at 10
        road.addVehicle(new Vehicle("V2", Direction.NORTH, Direction.SOUTH, 20)); // entry at 20

        int currentStep = 30;
        // V1 czeka 20, V2 czeka 10. Średnia = 15.
        assertEquals(15.0, road.getAverageWaitTime(currentStep), 0.001);

        road.removeVehicle(); // Usuwamy V1
        // Zostaje V2 (czeka 10). Średnia = 10.
        assertEquals(10.0, road.getAverageWaitTime(currentStep), 0.001);
    }
}