# Getting Started

To run the application with full backend connectivity, follow these steps:

1.  **Start the Bridge Server**:
    Navigate to the `frontend` directory and run:
    ```bash
    node bridge.js
    ```
2.  **Start the UI**:
    In a second terminal, within the `frontend` directory, run:
    ```bash
    npm run dev
    ```
3.  **Access the Dashboard**:
    Open [http://localhost:5173](http://localhost:5173) in your browser.
---

# Smart Traffic UI - Documentation

This frontend application is an interactive dashboard built to visualize the **Intelligent Traffic Light Simulation**. It serves as the graphical user interface for designing traffic scenarios, interacting with the Java-based simulation engine, and analyzing results through real-time animations.

## Tech Stack

*   **Framework**: React 18 with TypeScript for strong data typing and UI scalability.
*   **Build Tool**: Vite for ultra-fast development and optimized bundling.
*   **Styling**: Tailwind CSS for a modern, dark-themed responsive design.
*   **Icons**: Lucide React for consistent and intuitive interface symbols.
*   **Backend Integration**: Node.js Express server acting as a "Process Bridge".

## File Structure and Module Descriptions

### Core Files
*   **`App.tsx`**: The main entry point of the application. It wraps the `Dashboard` component and establishes the high-level layout and global background styles.
*   **`bridge.js`**: A vital architectural component. Since a browser cannot directly execute local `.jar` files for security reasons, this Node.js Express server acts as a proxy. It listens on port 3001, receives commands from the UI, writes them to `input_temp.json`, executes the Java backend via system calls, and streams the `output_temp.json` results back to the frontend.
*   **`types/simulation.ts`**: Contains all TypeScript interfaces and type definitions. It maps directly to the Java DTOs (`InputDTO`, `OutputDTO`, `CommandDTO`), ensuring that the frontend and backend share a strict data contract.

### UI Components (`/components`)
*   **`Dashboard.tsx`**: The orchestration layer of the application. It manages the simulation state, including the `currentStep` pointer, the event log, and the global vehicle route map. It also contains the business logic for calculating traffic light states based on upcoming vehicle directions and handles the complex logic for importing and exporting scenario files.
*   **`Intersection.tsx`**: Handles the complex visual representation of the four-way junction. It contains the positioning logic for waiting queues and manages the lifecycle of animated vehicles.
    *   **Internal Component: `LeavingCar`**: Specifically designed to handle "exit animations". It uses `requestAnimationFrame` to trigger CSS transitions that move a vehicle from the stop line to a point outside the viewport, preventing vehicles from simply disappearing when a step is completed.
*   **`TrafficLight.tsx`**: A purely visual component that renders the three-state signal head. It supports both vertical and horizontal orientations and uses dynamic shadow effects to simulate glowing light states (`RED`, `YELLOW`, `GREEN`).

## Key Features

### 1. Java Engine Integration
The UI is not just a mockup; it is a full-stack tool. Through the `bridge.js` server, it communicates with the `traffic-simulation-backend-1.0-SNAPSHOT.jar`. This allows users to run actual heuristic or neural network strategies written in Java directly from the web interface.

### 2. Intelligent Light Synchronization
The dashboard features an advanced `getLightStates` algorithm. It inspects the current and next steps of the simulation data to determine which axis (North-South or East-West) should be granted the green light. It also automatically calculates the yellow (warning) phase if vehicles are expected to stop in the following step.

### 3. Smooth Exit Animations
To provide a realistic User Experience (UX), vehicles utilize **CSS Transitions**. When a vehicle leaves the intersection according to the backend data, it is moved into a "crossing" state where it smoothly glides across the junction and fades out, rather than teleporting out of existence.

### 4. Comprehensive File Management
The application supports the full lifecycle of a simulation:
*   **Generate**: Create random traffic scenarios locally.
*   **Save/Load Input**: Export your scenario as an `input.json` or load an existing one.
*   **Save/Load Output**: Export the calculated results as an `output.json` for later analysis or re-watch a previous simulation without re-running the backend.

