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
        setConflict(Direction.NORTH, Direction.EAST);
        setConflict(Direction.NORTH, Direction.WEST);
        setConflict(Direction.SOUTH, Direction.EAST);
        setConflict(Direction.SOUTH, Direction.WEST);
    }


    /**
     * Weryfikuje bezpieczeństwo stanu skrzyżowania w oparciu o macierz konfliktów.
     * Sprawdza, czy żadne dwa kierunki uznane za kolizyjne nie mają jednocześnie zapalonego
     * światła zielonego lub żółtego (faza przejściowa).
     *
     * @return true, jeśli stan jest bezpieczny; false, jeśli występuje konflikt prostopadły.
     */
    public boolean isStateSafe() {
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            for (int j = i + 1; j < dirs.length; j++) {
                TrafficLight lightA = trafficLights.get(dirs[i]);
                TrafficLight lightB = trafficLights.get(dirs[j]);

                if (lightA.isBlocking() && lightB.isBlocking()) {

                    if (isConflicting(dirs[i], dirs[j])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Pomocnicza metoda sprawdzająca konflikt w statycznej macierzy.
     */
    public boolean isConflicting(Direction d1, Direction d2) {
        return CONFLICT_MATRIX[d1.ordinal()][d2.ordinal()];
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