export type Direction = 'NORTH' | 'EAST' | 'SOUTH' | 'WEST';
export type LightState = 'RED' | 'YELLOW' | 'GREEN';


/**
 * Bazowy interfejs dla komend przesyłanych do symulacji.
 * Odpowiada strukturze CommandDTO w Javie.
 */
export interface BaseCommand {
    type: 'addVehicle' | 'step';
}

/**
 * Komenda dodania pojazdu.
 * Używamy nazw 'startRoad' i 'endRoad', aby były zgodne z przykładem z zadania.
 */
export interface AddVehicleCommand extends BaseCommand {
    type: 'addVehicle';
    vehicleId: string;
    startRoad: Direction;
    endRoad: Direction;
}

export interface Vehicle {
    id: string;
    startRoad: 'north' | 'south' | 'east' | 'west';
    endRoad: 'north' | 'south' | 'east' | 'west';
    isExiting?: boolean; // Dodajemy to
}

/**
 * Komenda wykonania kroku symulacji.
 */
export interface StepCommand extends BaseCommand {
    type: 'step';
}

/**
 * Unia typów dla wszystkich dostępnych komend.
 */
export type Command = AddVehicleCommand | StepCommand;

/**
 * Pełny obiekt wejściowy dla symulacji (InputDTO).
 */
export interface SimulationInput {
    commands: Command[];
}



/**
 * Reprezentuje status pojedynczego kroku symulacji (StepStatusDTO)[cite: 5, 6].
 */
export interface StepStatus {
    leftVehicles: string[];
}

/**
 * Pełny raport z symulacji (OutputDTO).
 */
export interface SimulationResult {
    stepStatuses: StepStatus[];
}



export interface IntersectionState {
    lights: Record<Direction, LightState>;
    waitingVehicles: Record<Direction, string[]>;
}

/**
 * Typ dla pojazdu przechowywanego w pamięci UI przed eksportem.
 */
export interface VehicleEntry {
    id: string;
    from: Direction;
    to: Direction;
    arrivalStep: number;
}