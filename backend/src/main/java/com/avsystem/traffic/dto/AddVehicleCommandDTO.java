package com.avsystem.traffic.dto;

import com.avsystem.traffic.model.Direction;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

/**
 * DTO dla komendy 'addVehicle'.
 * Odpowiada za bezpieczne mapowanie surowych danych JSON na typy domenowe.
 */
public class AddVehicleCommandDTO extends CommandDTO {

    @JsonProperty("vehicleId")
    private String vehicleId;

    @JsonProperty("startRoad")
    private String startRoad;

    @JsonProperty("endRoad")
    private String endRoad;

    /**
     * Konstruktor bezargumentowy wymagany przez Jacksona.
     */
    public AddVehicleCommandDTO() {}

    /**
     * Implementacja walidacji kontraktu.
     * Sprawdza, czy ID nie jest puste oraz czy drogi odpowiadają zdefiniowanym kierunkom.
     */
    @Override
    public boolean isValid() {
        if (vehicleId == null || vehicleId.trim().isEmpty()) {
            this.validationErrorMessage = "vehicleId cannot be null or empty";
            return false;
        }

        if (startRoad == null || endRoad == null) {
            this.validationErrorMessage = "Road directions (startRoad/endRoad) must be provided";
            return false;
        }

        if (!isValidDirection(startRoad)) {
            this.validationErrorMessage = "Invalid startRoad: '" + startRoad + "'. Valid directions: " + Arrays.toString(Direction.values());
            return false;
        }

        if (!isValidDirection(endRoad)) {
            this.validationErrorMessage = "Invalid endRoad: '" + endRoad + "'. Valid directions: " + Arrays.toString(Direction.values());
            return false;
        }

        return true;
    }

    /**
     * Pomocnicza metoda sprawdzająca, czy tekst z JSON pasuje do Enuma Direction.
     */
    private boolean isValidDirection(String direction) {
        try {
            Direction.valueOf(direction.toUpperCase());
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }


    public String getVehicleId() {
        return vehicleId;
    }

    /**
     * Zwraca kierunek startowy jako typ Enum.
     * Bezpieczne wywołanie dzięki wcześniejszej walidacji w isValid().
     */
    public Direction getStartDirection() {
        return Direction.valueOf(startRoad.toUpperCase());
    }

    /**
     * Zwraca kierunek docelowy jako typ Enum.
     */
    public Direction getEndDirection() {
        return Direction.valueOf(endRoad.toUpperCase());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(com.avsystem.traffic.core.SimulationEngine engine) {
        engine.handleAddVehicle(this);
        return null;
    }
}