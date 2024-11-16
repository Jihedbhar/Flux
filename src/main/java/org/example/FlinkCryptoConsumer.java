package org.example;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import java.util.Properties;

public class FlinkCryptoConsumer {
    public static void main(String[] args) throws Exception {
        // Set up the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Kafka consumer properties
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "crypto-group");
        properties.setProperty("auto.offset.reset", "latest"); // To consume from the latest offset

        // Create Kafka consumer for Flink
        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>(
                "crypto_prices",  // The Kafka topic name (same as in your producer)
                new SimpleStringSchema(),
                properties
        );

        // Add the Kafka consumer as a source
        DataStream<String> cryptoStream = env.addSource(kafkaConsumer);

        // Process the stream
        cryptoStream.print();  // Here, we just print the consumed messages (could be JSON data)

        env.execute("Flink Crypto Data Consumer");
    }
}