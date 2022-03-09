echo "Waiting for Kafka to come online..."

cub kafka-ready -b kafka:9092 1 20

# create the notifications topic
kafka-topics \
  --bootstrap-server kafka:9092 \
  --topic notifications \
  --replication-factor 1 \
  --partitions 1 \
  --create

sleep infinity