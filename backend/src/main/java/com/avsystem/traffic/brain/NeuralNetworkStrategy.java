package com.avsystem.traffic.brain;

import com.avsystem.traffic.model.Direction;
import com.avsystem.traffic.model.Intersection;
import com.avsystem.traffic.model.LightState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avsystem.traffic.model.Road;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class NeuralNetworkStrategy implements TrafficStrategy {

    private double[][] weightsInputHidden;
    private double[] biasHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasOutput;

    private int inputSize;
    private int hiddenSize;
    private int outputSize;

    private boolean isInitialized = false;
    private final Map<Direction, Integer> lastVehicleCounts = new EnumMap<>(Direction.class);

    public NeuralNetworkStrategy() {
        for (Direction dir : Direction.values()) {
            lastVehicleCounts.put(dir, 0);
        }
    }

    @Override
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("model_weights.json")) {
            if (is == null) {
                initializeFallback(16, 16, 2);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode weightsNode = root.get("weights");

            JsonNode ihNode = weightsNode.get("input_hidden");
            JsonNode hoNode = weightsNode.get("hidden_output");
            JsonNode bNode = weightsNode.get("biases");

            // Dynamiczne ustalanie rozmiarów na podstawie JSON
            this.inputSize = ihNode.size();
            this.hiddenSize = ihNode.get(0).size();
            this.outputSize = hoNode.get(0).size();

            this.weightsInputHidden = new double[inputSize][hiddenSize];
            this.biasHidden = new double[hiddenSize];
            this.weightsHiddenOutput = new double[hiddenSize][outputSize];
            this.biasOutput = new double[outputSize];

            // Wczytywanie wag
            for (int i = 0; i < inputSize; i++) {
                for (int j = 0; j < hiddenSize; j++) {
                    weightsInputHidden[i][j] = ihNode.get(i).get(j).asDouble();
                }
            }

            for (int j = 0; j < hiddenSize; j++) {
                for (int k = 0; k < outputSize; k++) {
                    weightsHiddenOutput[j][k] = hoNode.get(j).get(k).asDouble();
                }
            }

            for (int k = 0; k < outputSize; k++) {
                biasOutput[k] = bNode.get(k).asDouble();
            }

            this.isInitialized = true;
        } catch (Exception e) {
            initializeFallback(16, 16, 2);
        }
    }

    private void initializeFallback(int iSize, int hSize, int oSize) {
        this.inputSize = iSize;
        this.hiddenSize = hSize;
        this.outputSize = oSize;

        this.weightsInputHidden = new double[inputSize][hiddenSize];
        this.weightsHiddenOutput = new double[hiddenSize][outputSize];
        this.biasHidden = new double[hiddenSize];
        this.biasOutput = new double[outputSize];

        for (double[] row : weightsInputHidden) Arrays.fill(row, 0.1);
        for (double[] row : weightsHiddenOutput) Arrays.fill(row, 0.2);
        this.isInitialized = true;
    }

    @Override
    public void optimizeTraffic(Intersection intersection, int currentStep) {
        double[] input = captureStateVector(intersection, currentStep);
        double[] qValues = predict(input);
        applyIndividualDecisions(intersection, qValues);
    }

    private double[] predict(double[] input) {
        if (!isInitialized) return new double[outputSize];

        // Warstwa ukryta (ReLU)
        double[] hiddenLayer = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            double sum = biasHidden[j];
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weightsInputHidden[i][j];
            }
            hiddenLayer[j] = Math.max(0, sum);
        }

        // Warstwa wyjściowa
        double[] output = new double[outputSize];
        for (int k = 0; k < outputSize; k++) {
            double sum = biasOutput[k];
            for (int j = 0; j < hiddenSize; j++) {
                sum += hiddenLayer[j] * weightsHiddenOutput[j][k];
            }
            output[k] = sum;
        }
        return output;
    }

    private void applyIndividualDecisions(Intersection intersection, double[] qValues) {
        if (qValues.length < 2) return;

        // Histereza: musimy mieć 20% "pewności" więcej, by zmienić obecną fazę
        double hysteresisThreshold = 1.0;

        // Sprawdzamy co się pali teraz
        boolean isCurrentlyVertical = intersection.getTrafficLight(Direction.NORTH).allowsPassage();

        boolean wantVertical;
        if (isCurrentlyVertical) {
            // Jeśli pali się pion, zmień na poziom tylko jeśli qValues[1] jest znacznie większe
            wantVertical = qValues[0] * hysteresisThreshold > qValues[1];
        } else {
            // Jeśli pali się poziom, zmień na pion tylko jeśli qValues[0] jest znacznie większe
            wantVertical = qValues[0] > qValues[1] * hysteresisThreshold;
        }

        // Kierunki
        Direction[] vertical = {Direction.NORTH, Direction.SOUTH};
        Direction[] horizontal = {Direction.EAST, Direction.WEST};

// Zastąp blok otwierania świateł w applyIndividualDecisions:
        if (wantVertical) {
            // 1. Rozkaż poziomym zgasnąć (jeśli są zielone, przejdą w YELLOW)
            for (Direction d : horizontal) intersection.getTrafficLight(d).transitionTo(LightState.RED);

            // 2. Otwórz pion TYLKO jeśli poziome są JUŻ czerwone
            boolean horizontalIsSafe = Arrays.stream(horizontal)
                    .allMatch(d -> intersection.getTrafficLight(d).getCurrentState() == LightState.RED);

            if (horizontalIsSafe) {
                for (Direction d : vertical) intersection.getTrafficLight(d).transitionTo(LightState.GREEN);
            }
        } else {
            // 1. Rozkaż pionowym zgasnąć
            for (Direction d : vertical) intersection.getTrafficLight(d).transitionTo(LightState.RED);

            // 2. Otwórz poziom TYLKO jeśli pionowe są JUŻ czerwone
            boolean verticalIsSafe = Arrays.stream(vertical)
                    .allMatch(d -> intersection.getTrafficLight(d).getCurrentState() == LightState.RED);

            if (verticalIsSafe) {
                for (Direction d : horizontal) intersection.getTrafficLight(d).transitionTo(LightState.GREEN);
            }
        }
    }

    private double[] captureStateVector(Intersection intersection, int currentStep) {
        int featuresPerRoad = 5;
        double[] state = new double[Direction.values().length * featuresPerRoad];
        int i = 0;
        for (Direction dir : Direction.values()) {
            Road road = intersection.getRoad(dir); // Tutaj potrzebny jest ten import
            int currentCount = road.getVehicleCount();
            double avgWait = road.getAverageWaitTime(currentStep);
            double light = intersection.getTrafficLight(dir).allowsPassage() ? 1.0 : 0.0;
            double delta = currentCount - lastVehicleCounts.get(dir);
            double totalRoadCost = road.getTotalTimeCostInQueue();

            // Agresywna czułość na czas (avgWait / 25.0 zamiast 200.0)
            state[i++] = Math.min(totalRoadCost / 120.0, 1.0);
            state[i++] = Math.min(currentCount / 10.0, 1.0);
            state[i++] = Math.min(avgWait / 25.0, 1.0);
            state[i++] = light;
            state[i++] = Math.tanh(delta);

            lastVehicleCounts.put(dir, currentCount);
        }
        return state;
    }

    @Override
    public String getStrategyName() {
        return "Generic-Neural-Phase-Control";
    }
}