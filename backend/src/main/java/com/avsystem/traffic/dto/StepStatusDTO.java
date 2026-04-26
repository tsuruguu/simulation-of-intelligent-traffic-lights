package com.avsystem.traffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO reprezentujący stan pojedynczego kroku symulacji na wyjściu.
 * Przechowuje listę identyfikatorów pojazdów, które pomyślnie przejechały
 * przez skrzyżowanie w danej jednostce czasu.
 */
public class StepStatusDTO {

    /**
     * Lista ID pojazdów, które opuściły skrzyżowanie.
     * Adnotacja @JsonProperty("leftVehicles") zapewnia pełną zgodność ze specyfikacją wejścia/wyjścia.
     */
    @JsonProperty("leftVehicles")
    private List<String> leftVehicles;

    /**
     * Konstruktor domyślny wymagany przez mechanizm deserializacji Jacksona.
     */
    public StepStatusDTO() {
        this.leftVehicles = new ArrayList<>();
    }

    /**
     * Konstruktor inicjalizujący status konkretną listą pojazdów.
     * Wykorzystuje "defensive copying", aby chronić integralność raportu przed zmianami w innych częściach systemu.
     * @param leftVehicles Lista identyfikatorów pojazdów, które opuściły skrzyżowanie.
     */
    public StepStatusDTO(List<String> leftVehicles) {
        this.leftVehicles = (leftVehicles != null) ? new ArrayList<>(leftVehicles) : new ArrayList<>();
    }

    /**
     * Zwraca niemodyfikowalną listę pojazdów, które opuściły skrzyżowanie.
     * Gwarantuje to, że raport raz wygenerowany nie zostanie zmieniony przez błąd programistyczny w innej warstwie.
     */
    public List<String> getLeftVehicles() {
        return Collections.unmodifiableList(leftVehicles);
    }

    /**
     * Ustawia listę pojazdów z zachowaniem bezpieczeństwa danych.
     */
    public void setLeftVehicles(List<String> leftVehicles) {
        this.leftVehicles = (leftVehicles != null) ? new ArrayList<>(leftVehicles) : new ArrayList<>();
    }

    /**
     * METRYKA DIAGNOSTYCZNA (Pro-Tip):
     * Pozwala szybko sprawdzić, ile aut opuściło skrzyżowanie w danym kroku bez liczenia elementów listy.
     */
    @JsonIgnore
    public int getVehicleCount() {
        return leftVehicles.size();
    }

    /**
     * Implementacja metody toString() ułatwia debugowanie logiki świateł w konsoli deweloperskiej.
     */
    @Override
    public String toString() {
        return String.format("StepResult[vehicles=%d: %s]", leftVehicles.size(), leftVehicles);
    }
}