#!/bin/bash
if [ "$#" -ne 2 ]; then
    echo "Użycie: ./run_simulation.sh <input.json> <output.json>"
    exit 1
fi

INPUT_FILE=$1
OUTPUT_FILE=$2

cd backend && mvn clean package && cd ..

java -jar backend/target/traffic-simulation-1.0-SNAPSHOT.jar "$INPUT_FILE" "$OUTPUT_FILE"

echo "Simulation ended. Output saved in: $OUTPUT_FILE"