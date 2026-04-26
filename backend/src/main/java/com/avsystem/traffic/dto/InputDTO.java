package com.avsystem.traffic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Główny kontener danych wejściowych (Root DTO).
 * Reprezentuje strukturę całego pliku input.json.
 * Zapewnia integralność danych poprzez kaskadową walidację wszystkich zawartych komend.
 */
public class InputDTO {

    /**
     * Lista komend do wykonania. Dzięki polimorfizmowi w CommandDTO,
     * Jackson automatycznie wypełni tę listę odpowiednimi obiektami
     * (AddVehicleCommandDTO lub StepCommandDTO).
     */
    @JsonProperty("commands")
    private List<CommandDTO> commands = new ArrayList<>();

    // Pole przechowujące opis pierwszego napotkanego błędu (do celów diagnostycznych)
    private String validationErrorDetail = "";

    public InputDTO() {
    }

    /**
     * Przeprowadza kaskadową walidację całego zestawu danych.
     * Metoda "Fail-Fast" — symulacja nie powinna się rozpocząć,
     * jeśli choć jedna komenda jest niepoprawna.
     * * @return true, jeśli wszystkie komendy w pakiecie są poprawne.
     */

    public boolean isValid() {
        if (commands == null || commands.isEmpty()) {
            this.validationErrorDetail = "The 'commands' list is missing or empty.";
            return false;
        }

        // Kaskadowa walidacja - sprawdzamy każdą komendę z osobna
        for (int i = 0; i < commands.size(); i++) {
            CommandDTO command = commands.get(i);
            if (command == null) {
                this.validationErrorDetail = "Command at index " + i + " is null.";
                return false;
            }
            if (!command.isValid()) {
                // Agregujemy błąd z konkretnej komendy (np. AddVehicleCommand)
                this.validationErrorDetail = String.format("Error in command #%d [%s]: %s",
                        i, command.getCommandType(), command.getValidationErrorMessage());
                return false;
            }
        }

        return true;
    }

    /**
     * Zwraca niemodyfikowalną listę komend.
     * Chroni to integralność scenariusza symulacji przed zmianami w runtime.
     */
    public List<CommandDTO> getCommands() {
        return java.util.Collections.unmodifiableList(commands);
    }

    public void setCommands(List<CommandDTO> commands) {
        this.commands = commands;
    }

    public String getValidationErrorDetail() {
        return validationErrorDetail;
    }
}