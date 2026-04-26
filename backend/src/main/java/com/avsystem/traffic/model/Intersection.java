package com.avsystem.traffic.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Serce systemu symulacji. Zarządza infrastrukturą skrzyżowania.
 * Wykorzystuje Macierz Konfliktów do zapewnienia bezpieczeństwa w czasie O(1),
 * co jest kluczowe przy szybkich symulacjach i trenowaniu modeli AI.
 */
public class Intersection {

    private final Map<Direction, Road> roads;
    private final Map<Direction, TrafficLight> trafficLights;

    /**
     * Macierz Konfliktów (Adjacency Matrix).
     * conflictMatrix[A][B] == true oznacza, że kierunki A i B są kolizyjne.
     */
    private static final boolean[][] CONFLICT_MATRIX = new boolean[Direction.values().length][Direction.values().length];

    static {
        // Te pary NIE MOGĄ mieć zielonego jednocześnie
        setConflict(Direction.NORTH, Direction.EAST);
        setConflict(Direction.NORTH, Direction.WEST);
        setConflict(Direction.SOUTH, Direction.EAST);
        setConflict(Direction.SOUTH, Direction.WEST);
        // Jeśli chcesz być super-dokładna: zawracanie technicznie koliduje
        // z jazdą prosto z naprzeciwka, ale przy Twoim podziale na fazy (N-S razem)
        // wystarczy pilnować blokady osi prostopadłej.
    }

    public Intersection() {
        this.roads = new EnumMap<>(Direction.class);
        this.trafficLights = new EnumMap<>(Direction.class);

        for (Direction dir : Direction.values()) {
            roads.put(dir, new Road(dir));
            trafficLights.put(dir, new TrafficLight(dir));
        }
    }

    private static void setConflict(Direction d1, Direction d2) {
        CONFLICT_MATRIX[d1.ordinal()][d2.ordinal()] = true;
        CONFLICT_MATRIX[d2.ordinal()][d1.ordinal()] = true;
    }

    /**
     * Kluczowa metoda bezpieczeństwa (Safety Interlock).
     * Sprawdza, czy obecna konfiguracja świateł nie doprowadzi do katastrofy.
     * Uwzględnia asymetrię - sprawdza każdą parę sygnalizatorów z osobna.
     */
    public boolean isStateSafe() {
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            for (int j = i + 1; j < dirs.length; j++) {
                if (CONFLICT_MATRIX[i][j]) {
                    // ZMIANA: Sprawdzamy czy którykolwiek sygnalizator NIE JEST czerwony
                    // Jeśli jeden ma GREEN/YELLOW, a drugi też chce GREEN/YELLOW -> Konflikt
                    TrafficLight lightA = trafficLights.get(dirs[i]);
                    TrafficLight lightB = trafficLights.get(dirs[j]);

                    boolean lightANotRed = lightA.getCurrentState() != LightState.RED;
                    boolean lightBNotRed = lightB.getCurrentState() != LightState.RED;

                    if (lightANotRed && lightBNotRed) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * METRYKA GLOBALNA (Reward Function Basis):
     * Oblicza sumaryczny wskaźnik "frustracji" na skrzyżowaniu.
     * Wykorzystywane przez AI do oceny jakości podjętych decyzji.
     */
    public double getGlobalFrustrationIndex(int currentStep) {
        return roads.values().stream()
                .mapToDouble(r -> r.getAverageWaitTime(currentStep) * r.getVehicleCount())
                .sum();
    }

    /**
     * Zwraca sumaryczne ciśnienie (wagowe) dla kierunków kolizyjnych.
     * Wykorzystuje metodę getTotalPressure() z klasy Road, co pozwala
     * uwzględniać priorytety pojazdów (np. autobusów).
     */
    public double getConflictZonePressure(Direction direction) {
        double pressure = 0;
        for (Direction other : Direction.values()) {
            if (CONFLICT_MATRIX[direction.ordinal()][other.ordinal()]) {
                pressure += roads.get(other).getTotalPressure();
            }
        }
        return pressure;
    }

    /**
     * Helper dla NeuralNetworkStrategy: Zwraca znormalizowany (0.0 - 1.0)
     * poziom zatłoczenia konkretnej drogi.
     */
    public double getNormalizedCongestion(Direction direction, int maxExpectedCars) {
        return Math.min((double) getWaitingCountOnRoad(direction) / maxExpectedCars, 1.0);
    }

    public void setPhase(boolean vertical) {
        if (vertical) {
            getTrafficLight(Direction.NORTH).transitionTo(LightState.GREEN);
            getTrafficLight(Direction.SOUTH).transitionTo(LightState.GREEN);
            getTrafficLight(Direction.EAST).transitionTo(LightState.RED);
            getTrafficLight(Direction.WEST).transitionTo(LightState.RED);
        } else {
            getTrafficLight(Direction.NORTH).transitionTo(LightState.RED);
            getTrafficLight(Direction.SOUTH).transitionTo(LightState.RED);
            getTrafficLight(Direction.EAST).transitionTo(LightState.GREEN);
            getTrafficLight(Direction.WEST).transitionTo(LightState.GREEN);
        }
    }

    // --- Pozostałe metody bez zmian ---

    public int getTotalWaitingVehicles() {
        return roads.values().stream().mapToInt(Road::getVehicleCount).sum();
    }

    public int getWaitingCountOnRoad(Direction direction) {
        return roads.get(direction).getVehicleCount();
    }

    public Road getRoad(Direction direction) {
        return roads.get(direction);
    }

    public TrafficLight getTrafficLight(Direction direction) {
        return trafficLights.get(direction);
    }

    public Map<Direction, Road> getAllRoads() { return roads; }
    public Map<Direction, TrafficLight> getAllTrafficLights() { return trafficLights; }
}