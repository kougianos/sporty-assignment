#!/bin/bash
set -e

echo "=== Starting Kafka infrastructure ==="
docker compose up -d

echo "=== Waiting for Kafka to be ready ==="
until docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; do
    echo "  Kafka not ready yet, retrying..."
    sleep 2
done
echo "  Kafka is ready."

echo "=== Starting external-rest-dummy-api on port 8081 ==="
mvn -B -pl external-rest-dummy-api spring-boot:run &
EXTERNAL_PID=$!

echo "=== Starting sport-event-tracker on port 8080 ==="
mvn -B -pl sport-event-tracker spring-boot:run &
TRACKER_PID=$!

echo ""
echo "=== Both services starting in parallel ==="
echo "  Sport Event Tracker:    http://localhost:8080"
echo "  External Dummy API:     http://localhost:8081"
echo "  Kafka:                  localhost:9092"
echo ""
echo "Press Ctrl+C to stop all services"

trap "echo 'Shutting down...'; kill $TRACKER_PID $EXTERNAL_PID 2>/dev/null; docker compose down; exit 0" SIGINT SIGTERM

wait
