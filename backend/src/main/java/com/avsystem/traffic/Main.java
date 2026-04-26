package com.avsystem.traffic;

import com.avsystem.traffic.brain.*;
import com.avsystem.traffic.core.SimulationEngine;
import com.avsystem.traffic.dto.InputDTO;
import com.avsystem.traffic.dto.OutputDTO;
import com.avsystem.traffic.io.JsonParser;
import com.avsystem.traffic.io.OutputWriter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Główna klasa uruchomieniowa systemu Inteligentnej Symulacji Ruchu.
 * Odpowiada za orkiestrację komponentów, wstrzykiwanie zależności (Dependency Injection)
 * oraz zarządzanie cyklem życia aplikacji.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // 1. Walidacja argumentów linii poleceń
        if (args.length < 2) {
            System.err.println("Błąd: Niepoprawna liczba argumentów.");
            System.err.println("Użycie: java -jar traffic-sim.jar <input.json> <output.json>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        // Konfiguracja formatu logowania dla czytelności w konsoli
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");

        try {
            LOGGER.info("=== Inicjalizacja Inteligentnego Systemu Sterowania Ruchem ===");

            // 2. Inicjalizacja warstwy IO
            JsonParser parser = new JsonParser();
            OutputWriter writer = new OutputWriter();

            // 3. Parsowanie i walidacja danych wejściowych
            LOGGER.info("Wczytywanie scenariusza z: " + inputPath);
            InputDTO inputData = parser.parseInput(inputPath);

            if (inputData == null || !inputData.isValid()) {
                LOGGER.severe("Plik wejściowy zawiera błędy logiczne lub jest pusty.");
                return;
            }
// W Main.java upewnij się, że masz to:
            TrafficStrategy primaryAI = new NeuralNetworkStrategy();
            HeuristicStrategy safetyFallback = new HeuristicStrategy();

// To jest klucz: SafetyGuarded pilnuje AI
            TrafficStrategy hybridBrain = new SafetyGuardedStrategy(primaryAI, safetyFallback);

            // Inicjalizacja (np. ładowanie wag sieci, ustawienie filtrów EMA)
            hybridBrain.init();

            // 5. Uruchomienie silnika symulacji
            SimulationEngine engine = new SimulationEngine(hybridBrain);
            LOGGER.info("Uruchamianie symulacji. Strategia: " + hybridBrain.getStrategyName());

            long startTime = System.currentTimeMillis();
            OutputDTO outputResults = engine.simulate(inputData);
            long endTime = System.currentTimeMillis();

            // 6. Eksport wyników do pliku wyjściowego
            LOGGER.info(String.format("Symulacja zakończona sukcesem w %dms.", (endTime - startTime)));
            LOGGER.info("Zapisywanie raportu końcowego do: " + outputPath);

            writer.writeOutput(outputPath, outputResults);

            LOGGER.info("=== Przetwarzanie zakończone pomyślnie ===");
            System.out.println("\nGotowe! Wynik zapisano w: " + outputPath);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Błąd wejścia/wyjścia: " + e.getMessage(), e);
            System.exit(2);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Wystąpił nieoczekiwany błąd krytyczny: " + e.getMessage(), e);
            System.exit(3);
        }
    }
}