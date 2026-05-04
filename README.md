# Smart Traffic Analyzer & Simulation Engine

[![Java](https://img.shields.io/badge/Backend-Java_17-orange?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/Frontend-React_18-blue?style=flat-square&logo=react)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/Language-TypeScript-blue?style=flat-square&logo=typescript)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/Styling-Tailwind_CSS-38B2AC?style=flat-square&logo=tailwind-css)](https://tailwindcss.com/)

An advanced, full-stack simulation system designed to optimize traffic light cycles at a standard four-way intersection. This project combines a high-performance Java simulation engine with a modern, interactive React dashboard to visualize real-time traffic flow and AI-driven decision-making.

## Overview

The **Smart Traffic Analyzer** was developed as a recruitment task for **AVSystem**. The core objective is to design a system that dynamically adjusts traffic light phases based on current road occupancy, ensuring maximum throughput while maintaining strict safety standards (avoiding conflicting green lights).

### Key Features
- **Dual-Engine Strategy**: Choose between a rule-based Heuristic approach and an experimental Neural Network "brain".
- **Full-Stack Visualization**: Real-time animation of traffic steps using a React-based GUI.
- **Process Bridge**: A Node.js middleware that enables the browser to execute and communicate with the Java CLI engine seamlessly.
- **Smooth UX**: Custom animation logic for vehicles entering and leaving the intersection, preventing "teleportation" effects.
- **Robust Data Contract**: Strict JSON-based input/output handling as per system requirements.

---

## System Architecture

The project follows a decoupled architecture, separating the simulation logic from the presentation layer.

### Components

1. **Simulation Engine (Java)**:
    - Built with **Java 17**.
    - Uses the **Strategy Pattern** to swap traffic management algorithms at runtime.
    - Processes `input.json` and generates `output.json` with vehicle throughput data.

2. **Interactive Dashboard (React + TS)**:
    - High-fidelity visualization of the 4-way intersection (North, South, East, West).
    - State-driven traffic lights and vehicle queues.
    - Built-in traffic generator and file management tools.

3. **The Bridge (Node.js)**:
    - An Express.js proxy server that manages the lifecycle of the Java process.
    - Orchestrates file I/O operations between the browser and the local file system.

---

## Detailed Module Documentation

To better understand the internal logic, setup, and specific technical decisions of each module, please refer to the dedicated documentation files below:

*   **[⚙️ Backend Engine Details](./backend/README_BACKEND.md)**: A deep dive into the Java simulation core, including the Strategy Pattern, the Neural Network implementation, and safety guard logic.
*   **[💻 Frontend UI Details](./frontend/README_FRONTEND.md)**: Detailed information regarding the React dashboard, the Node.js bridge architecture, and the custom CSS animation system for vehicles.

---

## Project Structure

```text
.
├── backend                 # Java Simulation Engine (Core)
│   ├── README_BACKEND.md   # <--- Detailed backend docs
│   ├── src/main/java       # Simulation logic, DTOs, and Brains
│   └── pom.xml             # Maven configuration
├── frontend                # React + Vite Application
│   ├── README_FRONTEND.md  # <--- Detailed frontend docs
│   ├── src/components      # UI Components (Intersection, Dashboard)
│   └── bridge.js           # The Proxy Bridge (Node.js)
├── examples                # Sample input/output JSON files
└── run_simulation.sh       # Utility script
```

---

## Brains & Strategies

The backend supports multiple strategies for light management:

### 1. Heuristic Strategy
Evaluates the ratio of waiting vehicles between the North-South axis and the East-West axis. It implements a "Pressure-Based" transition, where the green light duration is proportional to the backlog on a given road.

### 2. Neural Network Strategy (AI)
An experimental implementation using pre-trained weights (`model_weights.json`). It analyzes road occupancy patterns to predict the next optimal state configuration.

### 3. Safety Guard Layer
A mandatory wrapper around all strategies. It acts as a "Hardware Interlock" equivalent, verifying that no two conflicting directions ever receive a green light simultaneously, regardless of what the AI suggests.

---

## Getting Started

### Prerequisites
- Java 17 or higher
- Node.js 18 or higher
- Maven 3.8+

### 1. Build the Backend

Navigate to the backend folder and package the application:
```bash
cd backend
mvn clean package
```
This will generate `traffic-simulation-backend-1.0-SNAPSHOT.jar` in the `target` directory.

### 2. Prepare the Frontend

Navigate to the frontend folder and install dependencies:
```bash
cd frontend
npm install
```

### 3. Run the Full Stack

To experience the full interactive simulation, open two terminal windows:

#### Terminal 1 (The Bridge):
```bash
cd frontend
node bridge.js
```

The bridge will start on port 3001, ready to talk to the Java engine

#### Terminal 2 (The UI):

```bash
cd frontend
npm run dev
```

Open http://localhost:5173 in your browser.

---

## Data Format Specification

### Input JSON (`input.json`)

The system accepts a list of commands to populate the simulation

```json
{
  "commands": [
    { "type": "addVehicle", "vehicleId": "V1", "startRoad": "SOUTH", "endRoad": "NORTH" },
    { "type": "step" }
  ]
}
```

### Output JSON (`output.json`)

The engine reports which vehicles cleared the intersection at each step

```json
{
  "stepStatuses": [
    { "leftVehicles": ["V1"] },
    { "leftVehicles": [] }
  ]
}
```

---

## Quality Assurance

The backend includes comprehensive unit tests to ensure simulation stability:

- **Model Tests**: Validating FIFO behavior in roads and light state transitions.
- **Engine Tests**: Ensuring correct command processing and result generation.

[//]: # (Run tests via Maven:)

[//]: # (```bash)

[//]: # (cd backend)

[//]: # (mvn test)

[//]: # (```)
