package com.avsystem.traffic.dto;

import com.avsystem.traffic.core.SimulationEngine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Polimorficzna baza kontraktowa dla komend (Wzorzec Command).
 * Wykorzystuje mechanizm Jacksona do automatycznego mapowania typów na podstawie pola "type".
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AddVehicleCommandDTO.class, name = "addVehicle"),
        @JsonSubTypes.Type(value = StepCommandDTO.class, name = "step")
})
public abstract class CommandDTO {

    @JsonIgnore
    protected String validationErrorMessage = "";

    /**
     * Główny kontrakt walidacyjny.
     * Każda komenda musi wiedzieć, jak sprawdzić swoją poprawność logiczną przed egzekucją.
     */
    public abstract boolean isValid();

    /**
     * SERCE WZORCA COMMAND:
     * Zamiast sprawdzać typ komendy w silniku (instanceof), silnik po prostu wywołuje 'execute'.
     * Używamy generyków <T>, aby różne komendy mogły zwracać różne wyniki
     * (np. StepCommand zwraca StepStatusDTO, a AddVehicle zwraca Void).
     * * @param engine Referencja do silnika symulacji (odbiorca polecenia).
     * @return Wynik działania komendy (może być null).
     */
    public abstract <T> T execute(SimulationEngine engine);

    @JsonIgnore
    public String getValidationErrorMessage() {
        return validationErrorMessage;
    }

    /**
     * Metoda pomocnicza dla logowania.
     */
    @JsonIgnore
    public String getCommandType() {
        JsonSubTypes subTypes = this.getClass().getSuperclass().getAnnotation(JsonSubTypes.class);
        for (JsonSubTypes.Type type : subTypes.value()) {
            if (type.value().equals(this.getClass())) {
                return type.name();
            }
        }
        return "unknown";
    }
}