package com.avsystem.traffic.brain;

import com.avsystem.traffic.model.Intersection;

/**
 * Interfejs definiujący kontrakt dla algorytmów sterowania ruchem.
 * Umożliwia implementację różnych podejść: od prostej rotacji (Round Robin),
 * przez zaawansowane heurystyki wagowe, aż po modele Deep Reinforcement Learning.
 */
public interface TrafficStrategy {

    /**
     * Główna metoda decyzyjna wywoływana w każdym kroku symulacji przed ruchem pojazdów.
     * Na podstawie aktualnego stanu skrzyżowania, strategia decyduje o zmianie
     * lub utrzymaniu stanów sygnalizatorów.
     * * @param intersection Obiekt skrzyżowania dostarczający pełnej telemetrii (liczba aut, czasy oczekiwania).
     * @param currentStep Aktualny czas symulacji, niezbędny do obliczania priorytetów czasowych.
     */
    void optimizeTraffic(Intersection intersection, int currentStep);

    /**
     * Metoda informacyjna zwracająca nazwę używanej strategii.
     * Przydatna do logowania i porównywania wydajności różnych algorytmów w raporcie końcowym.
     * * @return Nazwa strategii (np. "Heuristic-Weight-Based" lub "Neural-Network-V1").
     */
    String getStrategyName();

    default void init() {
    }
}