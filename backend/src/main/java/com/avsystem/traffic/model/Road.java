package com.avsystem.traffic.model;

import java.util.Queue;
import java.util.ArrayDeque;

/**
 * Wydajna reprezentacja drogi dojazdowej.
 * Wykorzystuje ArrayDeque dla operacji O(1) oraz sumaryczną telemetrię,
 * aby dostarczać statystyk dla AI bez kosztownych pętli.
 */
public class Road {

    private final Direction direction;
    private final Queue<Vehicle> vehicleQueue;

    /**
     * TELEMETRIA O(1): Suma kroków wejściowych (entryStep) wszystkich pojazdów.
     */
    private long totalEntryStepsSum = 0;

    /**
     * TELEMETRIA O(1): Suma wag priorytetów wszystkich pojazdów w kolejce.
     * Pozwala na obliczenie "ciśnienia" drogi bez iteracji po liście.
     */
    private double totalWeightSum = 0.0;

    public Road(Direction direction) {
        this.direction = direction;
        this.vehicleQueue = new ArrayDeque<>();
    }

    /**
     * Dodaje pojazd do kolejki. Aktualizuje telemetrię w czasie O(1).
     */
    public void addVehicle(Vehicle vehicle) {
        if (vehicle != null) {
            vehicleQueue.add(vehicle);
            totalEntryStepsSum += vehicle.getEntryStep();
            totalWeightSum += vehicle.getPriorityWeight();
        }
    }

    /**
     * Usuwa pojazd z przodu kolejki. Aktualizuje telemetrię w czasie O(1).
     */
    public Vehicle removeVehicle() {
        Vehicle vehicle = vehicleQueue.poll();
        if (vehicle != null) {
            totalEntryStepsSum -= vehicle.getEntryStep();
            totalWeightSum -= vehicle.getPriorityWeight();
        }
        return vehicle;
    }

    /**
     * Zwraca czas oczekiwania pojazdu, który stoi najdłużej (na początku kolejki).
     * Kluczowe dla mechanizmu Safety Override (Starvation Prevention).
     */
    public int getMaxWaitTime(int currentStep) {
        Vehicle oldest = vehicleQueue.peek();
        return (oldest != null) ? oldest.getWaitTime(currentStep) : 0;
    }

    /**
     * TELEMETRIA O(1): Oblicza średni czas oczekiwania aut na tej drodze.
     * Wzór wyprowadzony: Σ(currentStep - entryStep) / count
     */
    public double getAverageWaitTime(int currentStep) {
        int count = vehicleQueue.size();
        if (count == 0) return 0.0;

        long totalWaitTime = ((long) count * currentStep) - totalEntryStepsSum;
        return (double) totalWaitTime / count;
    }

    /**
     * Zwraca sumaryczne "ciśnienie" drogi uwzględniając priorytety pojazdów.
     * Dzięki temu AI "widzi" korek nie tylko jako liczbę aut, ale jako masę priorytetową.
     */
    public double getTotalPressure() {
        return totalWeightSum;
    }

    public double getTotalTimeCostInQueue() {
        double totalCost = 0;
        boolean isFirst = true;
        for (Vehicle v : vehicleQueue) {
            totalCost += isFirst ? 30.0 : 15.0;
            if (!v.isGoingStraight()) totalCost += 15.0;
            isFirst = false;
        }
        return totalCost;
    }


    public Vehicle peekVehicle() { return vehicleQueue.peek(); }
    public int getVehicleCount() { return vehicleQueue.size(); }
    public boolean isEmpty() { return vehicleQueue.isEmpty(); }
    public Direction getDirection() { return direction; }
}