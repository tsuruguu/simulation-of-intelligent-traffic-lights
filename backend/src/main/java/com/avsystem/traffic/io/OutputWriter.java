package com.avsystem.traffic.io;

import com.avsystem.traffic.dto.OutputDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;

/**
 * Komponent odpowiedzialny za trwałe zapisywanie wyników symulacji.
 * Wykorzystuje zaawansowaną serializację Jackson do generowania
 * zoptymalizowanych plików wyjściowych JSON.
 */
public class OutputWriter {

    private final ObjectMapper objectMapper;

    public OutputWriter() {
        this.objectMapper = new ObjectMapper();

        // Rejestracja modułów dla nowoczesnych typów danych (np. Java 8 Time)
        this.objectMapper.findAndRegisterModules();

        // Zapewnienie czytelności pliku (Pretty Print) zgodnie z Twoją sugestią
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Formatowanie dat w standardzie ISO-8601 (zamiast timestampów)
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Zapisuje finalny raport z symulacji do pliku JSON.
     * Operacja jest atomowa i bezpieczna wątkowo.
     * * @param filePath Ścieżka do pliku wyjściowego (np. output.json).
     * @param outputData Obiekt DTO zawierający historię wszystkich kroków symulacji.
     * @throws IOException Gdy wystąpi błąd zapisu do pliku.
     */
    public void writeOutput(String filePath, OutputDTO outputData) throws IOException {
        if (outputData == null) {
            throw new IllegalArgumentException("Output data cannot be null");
        }

        // Zapis do pliku z automatycznym formatowaniem (Pretty Print jest włączony w konstruktorze)
        objectMapper.writeValue(new File(filePath), outputData);
    }

    /**
     * Opcjonalna metoda do zapisu logów diagnostycznych (np. dla frontendu).
     * Zapisuje pełny stan skrzyżowania w każdym kroku.
     */
    public void writeDebugHistory(String filePath, Object detailedHistory) throws IOException {
        if (detailedHistory == null) return;

        // Zapisujemy dodatkowy plik z pełną telemetrią (stany świateł, Q-values sieci, frustracja aut)
        objectMapper.writeValue(new File(filePath), detailedHistory);
    }

    // --- LOGIKA "NEXT-GEN" ---

    /**
     * Serializacja asynchroniczna przy użyciu CompletableFuture.
     * Zapobiega blokowaniu wątku obliczeniowego AI przez operacje dyskowe.
     */
    public void writeOutputAsync(String filePath, OutputDTO outputData) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                writeOutput(filePath, outputData);
            } catch (IOException e) {
                java.util.logging.Logger.getLogger(OutputWriter.class.getName())
                        .log(java.util.logging.Level.SEVERE, "Błąd podczas asynchronicznego zapisu wyników", e);
            }
        });
    }
}