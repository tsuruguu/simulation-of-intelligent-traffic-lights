package com.avsystem.traffic.core;

import com.avsystem.traffic.model.Intersection;
import com.avsystem.traffic.dto.*;
import com.avsystem.traffic.brain.TrafficStrategy;
import com.avsystem.traffic.model.Vehicle;
import java.util.List;
import java.util.logging.Logger;

/**
 * Główny silnik symulacji (Orkiestrator).
 * Zarządza stanem skrzyżowania, wykonuje komendy i koordynuje pracę
 * algorytmów sterujących ruchem oraz fizyki kroków.
 */
public class SimulationEngine {

    private static final Logger LOGGER = Logger.getLogger(SimulationEngine.class.getName());

    private final Intersection intersection;
    private final OutputDTO results;
    private final StepManager stepManager;
    private TrafficStrategy brain;
    private int currentStep;

    public SimulationEngine(TrafficStrategy strategy) {
        this.intersection = new Intersection();
        this.results = new OutputDTO();
        this.stepManager = new StepManager(intersection, 200.0);
        this.brain = strategy;
        this.currentStep = 0;
    }

    /**
     * Główna pętla wykonawcza symulacji.
     * Przetwarza listę komend wejściowych i buduje raport wyjściowy.
     */
    public OutputDTO simulate(InputDTO input) {
        LOGGER.info("Rozpoczynanie przetwarzania sekwencji komend...");
        for (CommandDTO command : input.getCommands()) {
            executeCommand(command);
        }
        return results;
    }

    /**
     * Egzekucja komendy przy użyciu Wzorca Command.
     * Brak 'instanceof' sprawia, że silnik jest odporny na zmiany w typach komend.
     */
    private void executeCommand(CommandDTO command) {
        // Polimorficzne wywołanie - komenda sama wie, co wywołać w silniku
        command.execute(this);
    }

    /**
     * Obsługuje logikę dodawania pojazdu.
     * Metoda publiczna wywoływana przez AddVehicleCommandDTO.
     */
    public void handleAddVehicle(AddVehicleCommandDTO dto) {
        Vehicle vehicle = new Vehicle(
                dto.getVehicleId(),
                dto.getStartDirection(),
                dto.getEndDirection(),
                currentStep
        );

        intersection.getRoad(dto.getStartDirection()).addVehicle(vehicle);
    }

    /**
     * Wykonuje krok symulacji.
     * Metoda publiczna wywoływana przez StepCommandDTO.
     * @return Status wykonanego kroku do dołączenia do raportu.
     */
    public StepStatusDTO handleStep() {
        currentStep++;

        // 1. Optymalizacja: Strategia (AI/Heurystyka) dostosowuje światła do stanu dróg
        brain.optimizeTraffic(intersection, currentStep);

        // 2. Fizyka: StepManager przesuwa pojazdy i sprawdza Interlock bezpieczeństwa
        List<String> vehiclesThatLeft = stepManager.performStep(currentStep);

        // 3. Raportowanie: Tworzymy DTO wyniku i rejestrujemy w wynikach globalnych
        StepStatusDTO stepStatus = new StepStatusDTO(vehiclesThatLeft);
        results.addStepStatus(stepStatus);

        return stepStatus;
    }

    // --- LOGIKA "NEXT-GEN" ---

    /**
     * Pozwala na dynamiczną zmianę strategii sterowania w locie.
     * Np. przełączenie z Heurystyki na AI w trakcie trwania symulacji.
     */
    public void setTrafficStrategy(TrafficStrategy newStrategy) {
        LOGGER.info("Zmiana strategii sterowania na: " + newStrategy.getStrategyName());
        this.brain = newStrategy;
    }

    public int getCurrentStep() { return currentStep; }
    public Intersection getIntersection() { return intersection; }
    public OutputDTO getResults() { return results; }
}