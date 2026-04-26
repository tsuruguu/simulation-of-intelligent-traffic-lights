package com.avsystem.traffic.model;

/**
 * Klasa reprezentująca pojazd jako inteligentnego agenta symulacji.
 * Dostarcza precyzyjnych metryk (frustracja, ekologia), które pozwalają
 * na trenowanie modeli AI i zaawansowaną optymalizację ruchu.
 */
public class Vehicle {

    private final String id;
    private final Direction startRoad;
    private final Direction endRoad;
    private final int entryStep;

    /**
     * Licznik zatrzymań pojazdu.
     * Wykorzystywany do obliczania kary (penalty) w modelach Reinforcement Learning.
     */
    private int stopCount;

    public Vehicle(String id, Direction startRoad, Direction endRoad, int entryStep) {
        this.id = id;
        this.startRoad = startRoad;
        this.endRoad = endRoad;
        this.entryStep = entryStep;
        this.stopCount = 0;
    }

    /**
     * Oblicza czas oczekiwania w krokach symulacji.
     */
    public int getWaitTime(int currentStep) {
        return Math.max(0, currentStep - entryStep);
    }

    /**
     * Zwraca wagę priorytetu pojazdu.
     * Logika gotowa pod rozszerzenie o pojazdy uprzywilejowane (np. ID zaczynające się od 'BUS_').
     */
    public double getPriorityWeight() {
        if (id.startsWith("EMERGENCY")) return 5.0; // Priorytet dla karetek
        if (id.startsWith("BUS")) return 2.5;       // Priorytet dla transportu publicznego
        return 1.0;
    }

    /**
     * Oblicza złożony wskaźnik frustracji kierowcy.
     * Wykorzystuje nieliniową zależność: każde kolejne zatrzymanie
     * irytuje bardziej niż samo oczekiwanie.
     */
    public double getFrustrationIndex(int currentStep) {
        int waitTime = getWaitTime(currentStep);
        // Frustracja = Czas_Oczekiwania + (Liczba_Zatrzymań * mnożnik_irytacji)
        return waitTime + (stopCount * 3.5);
    }

    /**
     * Metoda wywoływana, gdy auto musi stać na czerwonym świetle.
     */
    public void incrementStopCount() {
        this.stopCount++;
    }

    /**
     * Sprawdza, czy pojazd wykonuje manewr skrętu w prawo.
     * Wykorzystuje algebrę modularną na indeksach kierunków (N=0, E=1, S=2, W=3).
     * W ruchu prawostronnym (Polska), skręt w prawo to przesunięcie o +1 w skali zegara.
     * * $$(End - Start + 4) \pmod 4 = 1$$
     */
    public boolean isTurningRight() {
        int start = startRoad.ordinal();
        int end = endRoad.ordinal();

        return (end - start + 4) % 4 == 1;
    }

    /**
     * Sprawdza, czy pojazd jedzie prosto przez skrzyżowanie.
     * $$(End - Start + 4) \pmod 4 = 2$$
     */
    public boolean isGoingStraight() {
        int start = startRoad.ordinal();
        int end = endRoad.ordinal();
        return (end - start + 4) % 4 == 2;
    }

    /**
     * Sprawdza, czy pojazd wykonuje manewr zawracania (U-turn).
     * (End - Start + 4) % 4 == 0
     */
    public boolean isTurningBack() {
        int start = startRoad.ordinal();
        int end = endRoad.ordinal();
        return (end - start + 4) % 4 == 0;
    }

    // --- Gettery ---

    public String getId() { return id; }
    public Direction getStartRoad() { return startRoad; }
    public Direction getEndRoad() { return endRoad; }
    public int getEntryStep() { return entryStep; }
    public int getStopCount() { return stopCount; }
}