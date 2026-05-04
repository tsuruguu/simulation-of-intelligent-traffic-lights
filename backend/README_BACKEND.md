## Prerequisites
* **Java Development Kit (JDK) 21** or higher.
* **Apache Maven 3.9+**.
* (Optional) An IDE such as IntelliJ IDEA or VS Code with Java extensions.

## Project Structure

```text
.
├── README_BACKEND.md
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── avsystem
    │   │           └── traffic
    │   │               ├── Main.java
    │   │               ├── brain
    │   │               │   ├── HeuristicStrategy.java
    │   │               │   ├── NeuralNetworkStrategy.java
    │   │               │   ├── SafetyGuardedStrategy.java
    │   │               │   └── TrafficStrategy.java
    │   │               ├── core
    │   │               │   ├── SimulationEngine.java
    │   │               │   └── StepManager.java
    │   │               ├── dto
    │   │               │   ├── AddVehicleCommandDTO.java
    │   │               │   ├── CommandDTO.java
    │   │               │   ├── InputDTO.java
    │   │               │   ├── OutputDTO.java
    │   │               │   ├── StepCommandDTO.java
    │   │               │   └── StepStatusDTO.java
    │   │               ├── io
    │   │               │   ├── JsonParser.java
    │   │               │   └── OutputWriter.java
    │   │               └── model
    │   │                   ├── Intersection.java
    │   │                   ├── Road.java
    │   │                   ├── TrafficLight.java
    │   │                   └── Vehicle.java
    │   └── resources
    │       ├── logging.properties
    │       └── model_weights.json
    └── test
        ├── IntersectionTest.java
        ├── TrafficLightTest.java
        └── VehicleTest.java
```

# Traffic Simulation

A Java-based traffic intersection simulation focused on intelligent signal control, safety guarantees, and AI-ready telemetry. The project combines classic software architecture patterns with traffic-domain logic such as asymmetric signal phases, green arrows, wait-time pressure, and safety interlocks.

---

## Core Features & Innovations

### Asymmetric Phase Management

The AI can control each traffic light independently instead of relying only on rigid, predefined phase groups. This allows highly flexible and non-standard green waves, making the simulation suitable for experimenting with adaptive traffic control strategies.

### Green Arrow Support

Conditional right-turn logic is integrated with the physical vehicle intention model. Vehicle maneuvers are detected using modular algebra on direction ordinals, which makes right-turn, left-turn, and straight movement classification clean and extensible.

### Safety Interlock

A fail-safe safety layer prevents conflicting green states at the physical model level. Even if the AI or heuristic strategy proposes an unsafe configuration, the simulation model can reject or override it to prevent collisions.

### Wait Time Telemetry O(1)

Road congestion and average wait times are calculated in constant time using aggregate entry-step sums. This avoids repeatedly iterating over every vehicle on a road and keeps the simulation efficient even for larger scenarios.

---

## Detailed Module Descriptions

## 1. Brain Package (`com.avsystem.traffic.brain`)

This package contains the "intelligence" of the simulation. It follows the Strategy Pattern to allow dynamic switching between different control algorithms.

### `TrafficStrategy.java`

The core interface defining the contract for any traffic control algorithm. It includes methods for initialization (`init`) and optimization logic.

### `HeuristicStrategy.java`

Implements a "Max-Pressure" algorithm. It uses Exponential Moving Averages (EMA) to calculate road pressure and prioritizes directions with the highest congestion.

### `NeuralNetworkStrategy.java`

A Deep Q-Network (DQN) inference engine. It captures a 16-dimensional state vector, including temporal trends and delta counts, and predicts optimal light configurations using a ReLU-activated feed-forward neural network.

### `SafetyGuardedStrategy.java`

A decorator/wrapper strategy. It monitors the primary AI's decisions and overrides them if they violate safety constraints or if a specific road has reached a "starvation" threshold, meaning extreme wait times.

---

## 2. Core Package (`com.avsystem.traffic.core`)

The backbone of the simulation execution.

### `SimulationEngine.java`

The main orchestrator. It manages the simulation clock, processes polymorphic commands using the Command Pattern, and coordinates between the "Brain" and the physical model.

### `StepManager.java`

Handles the physics of each simulation step. It manages vehicle movement, updates traffic light timers, and enforces the Safety Interlock to prevent collisions.

---

## 3. DTO Package (`com.avsystem.traffic.dto`)

Data Transfer Objects that define the JSON communication contract.

### `CommandDTO.java`

Abstract base class for polymorphic commands. Uses Jackson annotations for automated type discovery based on the `type` field.

### `AddVehicleCommandDTO.java`

DTO for vehicle entry. Includes strict validation for road names and prevents invalid maneuvers, such as U-turns.

### `StepCommandDTO.java`

Triggers a simulation step. Encapsulates the execution logic for the `SimulationEngine`.

### `InputDTO.java`

The root container for input files. Performs cascaded "Fail-Fast" validation of all commands.

### `OutputDTO.java`

The root container for results. Generates the final chronologically ordered report of the simulation.

### `StepStatusDTO.java`

Represents the outcome of a single step, listing all vehicles that successfully crossed the intersection.

---

## 4. IO Package (`com.avsystem.traffic.io`)

Hardware/File system abstraction layer.

### `JsonParser.java`

High-performance parser utilizing Jackson. Supports both full-file data binding and a "Next-Gen" streaming mode for processing massive simulation scenarios with low memory overhead.

### `OutputWriter.java`

Responsible for generating the final JSON report. Features asynchronous writing capabilities and "Pretty Print" formatting for human readability.

---

## 5. Model Package (`com.avsystem.traffic.model`)

The physical representation of the domain.

### `Intersection.java`

Manages the four-way intersection. It contains the Conflict Matrix, also known as an adjacency matrix, used to verify safety in O(1) time.

### `Road.java`

A high-performance road model using `ArrayDeque`. It maintains real-time telemetry, such as average wait time and total pressure, in O(1) without iterating over vehicles.

### `TrafficLight.java`

A Finite State Machine (FSM) implementation. It handles transitions from Green to Yellow to Red and manages Green Arrows for conditional right turns.

### `Vehicle.java`

Represents a vehicle as an intelligent agent. It uses modular algebra on direction ordinals to detect maneuvers, such as left, right, and straight, and tracks frustration/stop counts for AI reward functions.

---

## Design Patterns Used

### Strategy Pattern

Used for traffic control algorithms through `TrafficStrategy`. This enables seamless switching between heuristic control, neural-network-based control, and any future algorithm without rewriting the simulation engine.

### Command Pattern

Simulation actions such as adding vehicles and advancing the simulation step are encapsulated into polymorphic DTO commands. This keeps input processing extensible and clean.

### Decorator / Wrapper

`SafetyGuardedStrategy` wraps the core AI or heuristic strategy and adds a safety fallback layer without changing the original algorithm implementation.

### Finite State Machine

Implemented in `TrafficLight` to manage legal traffic light transitions, including Green, Yellow, Red, and conditional green-arrow states.

---

## Telemetry & AI Reward Metrics

### Stop Count Tracking

The simulation monitors how many times each vehicle had to stop. This can be used as a metric for fuel efficiency, emission estimation, and comfort of traffic flow.

### Frustration Index

A composite metric based on total wait time and stop frequency. It provides a more human-centered view of traffic quality than raw throughput alone.

### Road Pressure

Real-time calculation of vehicle density and congestion on each road approach. This metric is used by heuristic control and can also be included in AI reward functions.

---

## Resources & Configuration

### `logging.properties`

Configuration for `java.util.logging`. Defines a professional log format and severity levels.

### `model_weights.json`

Pre-trained weights and biases for the `NeuralNetworkStrategy`. This allows the AI to be updated without modifying the source code.

---
## JSON Interface

### Input Example (`input.json`)
```json
{
  "commands": [
    { "type": "addVehicle", "vehicleId": "V1", "startRoad": "NORTH", "endRoad": "SOUTH" },
    { "type": "step" },
    { "type": "step" }
  ]
}
```

### Output Example (`output.json`)

```json
{
  "stepStatuses": [
    { "leftVehicles": ["V1"] },
    { "leftVehicles": [] }
  ]
}
```

---

## Testing Suite

### `IntersectionTest.java`

Validates that the Conflict Matrix correctly identifies and blocks conflicting green light configurations.

### `TrafficLightTest.java`

Ensures the FSM transition logic, including enforcing Yellow lights, and green arrow conditions work as intended.

### `VehicleTest.java`

Rigorous testing of the modular math used to determine vehicle directionality across all compass points.

---

## Execution & Build

### Build

```bash
mvn clean package
```

Generates an Uber-JAR in the `/target` directory.

### Run

```bash
java -jar target/traffic-simulation.jar input.json output.json
```
