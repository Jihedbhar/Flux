# Flink-Kafka Crypto Price Watch/Monitoring Project

This project demonstrates the use of **Apache Flink** to ingest, process, and analyze cryptocurrency data from the **CoinGecko API** via **Kafka**. The project includes multiple filters (price spike detection, volume threshold, and moving average) and processes data in sliding windows for real-time analysis. The results are aggregated and visualized for better understanding.

---

## Key Features

- **Kafka Integration**: Data is ingested in real-time from the CoinGecko API and sent to Kafka.
- **Sliding Window**: Data is processed using a sliding window of 10 minutes with a slide interval of 1 minute.
- **Filters**:
  - **Price Spike Detection**: Filters out any price changes greater than 10% within the window.
  - **Volume Threshold**: Filters out low volume data points.
  - **Moving Average**: Applies a simple moving average to smooth out fluctuations in price data.
- **Aggregation**: Computes the average price for each cryptocurrency in each window.
- **Visualization**: Results are printed for real-time monitoring. (Optional: Use a tool like **Grafana** for visualization).

---

## Prerequisites

- **Kafka** running on localhost:9092 (or adjust as necessary).
- **Apache Flink** set up and running.
- **CoinGecko API** key (if needed).

---

## Setup Instructions

### 1. Install Kafka (if not installed already):

- Download and extract Kafka from [Apache Kafka Downloads](https://kafka.apache.org/downloads).
- Start Zookeeper and Kafka brokers by running the following commands:
  ```bash
  bin/zookeeper-server-start.sh config/zookeeper.properties
  bin/kafka-server-start.sh config/server.properties
