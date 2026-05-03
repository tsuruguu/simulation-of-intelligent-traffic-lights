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

        this.objectMapper.findAndRegisterModules();

        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
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

        objectMapper.writeValue(new File(filePath), outputData);
    }

    /**
     * Opcjonalna metoda do zapisu logów diagnostycznych (np. dla frontendu).
     * Zapisuje pełny stan skrzyżowania w każdym kroku.
     */
    public void writeDebugHistory(String filePath, Object detailedHistory) throws IOException {
        if (detailedHistory == null) return;

        objectMapper.writeValue(new File(filePath), detailedHistory);
    }


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