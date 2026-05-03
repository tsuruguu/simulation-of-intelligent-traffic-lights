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
import com.avsystem.traffic.model.TrafficLight;
import java.util.logging.Logger;

public class NeuralNetworkStrategy implements TrafficStrategy {

    private static final Logger LOGGER = Logger.getLogger(NeuralNetworkStrategy.class.getName());

    private double[][] weightsInputHidden;
    private double[] biasHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasOutput;

    private int inputSize;
    private int hiddenSize;
    private int outputSize;

    private double learningRate = 0.01;
    private double discountFactor = 0.95; // Lambda dla przyszłych nagród
    private double lastGlobalFrustration = 0;
    private double[] lastState;
    private int lastAction;

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

            this.inputSize = ihNode.size();
            this.hiddenSize = ihNode.get(0).size();
            this.outputSize = hoNode.get(0).size();

            this.weightsInputHidden = new double[inputSize][hiddenSize];
            this.biasHidden = new double[hiddenSize];
            this.weightsHiddenOutput = new double[hiddenSize][outputSize];
            this.biasOutput = new double[outputSize];

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

    private void train(double[] state, int action, double reward, double[] nextState) {
        double[] currentQ = predict(state);
        double[] nextQ = predict(nextState);

        double maxNextQ = nextQ[0];
        for (double q : nextQ) if (q > maxNextQ) maxNextQ = q;

        double target = reward + (discountFactor * maxNextQ);
        double error = target - currentQ[action];

        for (int j = 0; j < hiddenSize; j++) {
            weightsHiddenOutput[j][action] += learningRate * error;

            for (int i = 0; i < inputSize; i++) {
                    weightsInputHidden[i][j] += learningRate * error * weightsHiddenOutput[j][action] * 0.1;
                }
            }
        }
    }

    private double epsilon = 0.1;

    @Override
    public void optimizeTraffic(Intersection intersection, int currentStep) {
        double currentFrustration = intersection.getGlobalFrustrationIndex(currentStep);
        double[] currentState = captureStateVector(intersection, currentStep);

        if (lastState != null) {
            double reward = lastGlobalFrustration - currentFrustration;

            if (!intersection.isStateSafe()) {
                reward -= 50.0;
                LOGGER.warning("AI PENALIZED: Proposed unsafe configuration.");
            }

            train(lastState, lastAction, reward, currentState);
        }

        double[] qValues;
        if (Math.random() < epsilon) {
            qValues = new double[] {Math.random(), Math.random(), Math.random(), Math.random()};
        } else {
            qValues = predict(currentState);
        }

        int action = 0;
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > qValues[action]) {
                action = i;
            }
        }

        applyIndividualDecisions(intersection, qValues);

        this.lastState = currentState;
        this.lastAction = action;
        this.lastGlobalFrustration = currentFrustration;
    }

    private double[] predict(double[] input) {
        if (!isInitialized) return new double[outputSize];

        double[] hiddenLayer = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            double sum = biasHidden[j];
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weightsInputHidden[i][j];
            }
            hiddenLayer[j] = Math.max(0, sum);
        }

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
        if (qValues.length < 4) return;

        Direction[] dirs = Direction.values();
        Integer[] indices = {0, 1, 2, 3};
        java.util.Arrays.sort(indices, (a, b) -> Double.compare(qValues[b], qValues[a]));

        boolean[] willBeGreen = new boolean[4];

        for (int idx : indices) {
            Direction targetDir = dirs[idx];
            boolean hasConflict = false;
            for (int i = 0; i < 4; i++) {
                if (willBeGreen[i] && intersection.isConflicting(targetDir, dirs[i])) {
                    hasConflict = true;
                    break;
                }
            }

            if (!hasConflict && qValues[idx] > -1.0) {
                willBeGreen[idx] = true;
            }
        }

        for (int i = 0; i < 4; i++) {
            TrafficLight light = intersection.getTrafficLight(dirs[i]);
            if (willBeGreen[i]) {
                boolean pathBlockedByYellow = false;
                for (Direction other : Direction.values()) {
                    if (intersection.isConflicting(dirs[i], other) && intersection.getTrafficLight(other).isBlocking()) {
                        pathBlockedByYellow = true;
                        break;
                    }
                }

                if (!pathBlockedByYellow) {
                    light.transitionTo(LightState.GREEN);
                } else {
                    light.transitionTo(LightState.RED);
                }
            } else {
                light.transitionTo(LightState.RED);
            }
        }
    }

    private double[] captureStateVector(Intersection intersection, int currentStep) {
        int featuresPerRoad = 5;
        Direction[] dirs = Direction.values();
        double[] state = new double[dirs.length * featuresPerRoad + 1];

        int i = 0;
        for (Direction dir : dirs) {
            Road road = intersection.getRoad(dir);
            int currentCount = road.getVehicleCount();
            double avgWait = road.getAverageWaitTime(currentStep);
            double light = intersection.getTrafficLight(dir).allowsPassage() ? 1.0 : 0.0;
            double delta = currentCount - lastVehicleCounts.get(dir);
            double totalRoadCost = road.getTotalTimeCostInQueue();

            state[i++] = Math.min(totalRoadCost / 120.0, 1.0);
            state[i++] = Math.min(currentCount / 10.0, 1.0);
            state[i++] = Math.min(avgWait / 25.0, 1.0);
            state[i++] = light;
            state[i++] = Math.tanh(delta);

            lastVehicleCounts.put(dir, currentCount);
        }

        int phaseDuration = intersection.getTrafficLight(Direction.NORTH).getDurationInCurrentState();
        state[state.length - 1] = Math.min(phaseDuration / 40.0, 1.0);

        return state;
    }

    @Override
    public String getStrategyName() {
        return "Generic-Neural-Phase-Control";
    }
}