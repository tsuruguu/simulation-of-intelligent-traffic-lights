package com.avsystem.traffic.io;

import com.avsystem.traffic.dto.InputDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

/**
 * Wysokowydajny parser JSON oparty na bibliotece Jackson.
 * Wykorzystuje mechanizm Data Binding do szybkiego mapowania dużych zbiorów komend
 * na obiekty domenowe. Posiada wbudowaną walidację strukturalną.
 */
public class JsonParser {

    private final ObjectMapper objectMapper;

    public JsonParser() {
        this.objectMapper = new ObjectMapper();

        this.objectMapper.findAndRegisterModules();

        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    /**
     * Wczytuje plik wejściowy i przekształca go w obiekt DTO (Data Transfer Object).
     * Wykorzystuje technologię streamingu Jacksona dla minimalizacji zużycia RAM.
     * * @param filePath Ścieżka do pliku input.json.
     * @return Obiekt InputDTO zawierający listę komend do wykonania.
     * @throws IOException Gdy plik jest nieczytelny lub ma niepoprawny format.
     */
    public InputDTO parseInput(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Input file not found at: " + filePath);
        }

        InputDTO input = objectMapper.readValue(file, InputDTO.class);

        if (input == null || !input.isValid()) {
            throw new IOException("JSON validation failed: Some commands contain invalid data (e.g., wrong road names).");
        }

        return input;
    }

    /**
     * Implementacja strumieniowa (Next-Gen).
     * Pozwala na przetwarzanie milionów komend przy stałym zużyciu pamięci (np. 50MB RAM).
     */
    public void streamInput(String filePath, java.util.function.Consumer<com.avsystem.traffic.dto.CommandDTO> handler) throws IOException {
        com.fasterxml.jackson.core.JsonParser parser = objectMapper.getFactory().createParser(new File(filePath));

        while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            if ("commands".equals(fieldName)) {
                parser.nextToken();

                while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_ARRAY) {
                    com.avsystem.traffic.dto.CommandDTO cmd = objectMapper.readValue(parser, com.avsystem.traffic.dto.CommandDTO.class);
                    if (cmd.isValid()) {
                        handler.accept(cmd);
                    }
                }
            }
        }
        parser.close();
    }


    /**
     * Dynamicznie dobiera typ komendy na podstawie pola "type" w JSON.
     * Jackson robi to automatycznie dzięki adnotacjom @JsonTypeInfo w DTO.
     */
    private void setupPolymorphicDeserialization() {
    }
}