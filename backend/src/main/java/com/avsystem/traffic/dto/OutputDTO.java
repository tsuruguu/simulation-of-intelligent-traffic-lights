package com.avsystem.traffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Główny kontener danych wyjściowych (Root Output DTO).
 * Reprezentuje strukturę finalnego raportu wymagany przez specyfikację zadania.
 * Skupia się na dostarczeniu chronologicznej listy zdarzeń dla każdego kroku symulacji.
 */
public class OutputDTO {

    /**
     * Lista statusów dla każdego wykonanego kroku symulacji.
     * Adnotacja @JsonProperty zapewnia zgodność z kluczem "stepStatuses" w JSON.
     */
    @JsonProperty("stepStatuses")
    private List<StepStatusDTO> stepStatuses = new ArrayList<>();

    public OutputDTO() {
    }

    /**
     * Dodaje status pojedynczego kroku do raportu.
     * Metoda wykorzystywana przez SimulationEngine po każdym wykonaniu komendy 'step'.
     * @param status Obiekt zawierający listę pojazdów, które opuściły skrzyżowanie.
     */
    public void addStepStatus(StepStatusDTO status) {
        if (status != null) {
            this.stepStatuses.add(status);
        }
    }

    /**
     * Zwraca niemodyfikowalną listę statusów kroków.
     * Dzięki temu żaden komponent zewnętrzny nie zmieni wyników symulacji po jej zakończeniu.
     */
    public List<StepStatusDTO> getStepStatuses() {
        return Collections.unmodifiableList(stepStatuses);
    }

    public void setStepStatuses(List<StepStatusDTO> stepStatuses) {
        this.stepStatuses = (stepStatuses != null) ? new ArrayList<>(stepStatuses) : new ArrayList<>();
    }

    /**
     * Oblicza całkowitą liczbę kroków zarejestrowanych w raporcie.
     */
    @JsonIgnore
    public int getStepCount() {
        return stepStatuses.size();
    }

    /**
     * LOGIKA "NEXT-GEN": Podsumowanie przepustowości.
     * Oblicza sumaryczną liczbę wszystkich pojazdów, które opuściły skrzyżowanie.
     * To pole nie jest wymagane w JSON, ale jest kluczowe dla analityki i logowania.
     */
    @JsonIgnore
    public int getTotalVehiclesProcessed() {
        return stepStatuses.stream()
                .mapToInt(step -> step.getLeftVehicles().size())
                .sum();
    }

    /**
     * Czyści raport. Przydatne przy wielokrotnym uruchamianiu symulacji na tym samym obiekcie.
     */
    public void clear() {
        this.stepStatuses.clear();
    }
}