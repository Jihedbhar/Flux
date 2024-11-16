package org.example;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FlinkSlidingWindow {
    // To store the last known prices for filtering price spikes
    private static final Map<String, Double> lastPriceMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Initialize the environment and add the Kafka consumer as a source
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "crypto-group");
        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>(
                "crypto_prices", // Kafka topic name
                new SimpleStringSchema(),
                properties
        );

        DataStream<String> cryptoStream = env.addSource(kafkaConsumer)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(10))  // Handling out-of-order events
                                .withTimestampAssigner((event, timestamp) -> {
                                    return System.currentTimeMillis();
                                })
                );

        // Process data: Parse JSON, apply filters and compute aggregates
        SingleOutputStreamOperator<Double> cryptoPrices = cryptoStream
                .map(FlinkSlidingWindow::parseJson) // Parse JSON to create CryptoPrice objects
                .keyBy(CryptoPrice::getCryptoName)
                .filter(FlinkSlidingWindow::filterPriceSpikes) // Price Spikes Filter
                .filter(FlinkSlidingWindow::filterVolume) // Volume Threshold Filter
                .window(SlidingEventTimeWindows.of(Time.minutes(10), Time.minutes(1))) // Sliding window of 10 minutes
                .aggregate(new AveragePriceAggregator()); // Aggregation of price data

        cryptoPrices.print(); // Output the result

        env.execute("Flink Sliding Window with Filters");
    }

    // JSON parsing using Jackson to convert String to CryptoPrice object
    private static CryptoPrice parseJson(String record) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(record, CryptoPrice.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new CryptoPrice("unknown", 0.0, 0.0); // Return a default value if parsing fails
        }
    }

    // Filter out price spikes (10% increase or decrease)
    private static boolean filterPriceSpikes(CryptoPrice price) {
        String cryptoName = price.getCryptoName();
        double lastPrice = lastPriceMap.getOrDefault(cryptoName, price.getPrice()); // Fetch the last price from the map

        // If there's a significant price spike (more than 10%), filter it out
        if (Math.abs(price.getPrice() - lastPrice) / lastPrice > 0.1) {
            return false; // Filter out if price spikes more than 10%
        }

        // Update the last known price for this cryptocurrency
        lastPriceMap.put(cryptoName, price.getPrice());
        return true;
    }

    // Filter out small volume data points
    private static boolean filterVolume(CryptoPrice price) {
        return price.getVolume() >= 1000000; // Example threshold: filter out if volume is less than 1,000,000
    }

    // Aggregator for average price calculation
    public static class AveragePriceAggregator implements AggregateFunction<CryptoPrice, Tuple2<Double, Integer>, Double> {
        @Override
        public Tuple2<Double, Integer> createAccumulator() {
            return new Tuple2<>(0.0, 0); // Initialize accumulator
        }

        @Override
        public Tuple2<Double, Integer> add(CryptoPrice value, Tuple2<Double, Integer> accumulator) {
            return new Tuple2<>(accumulator.f0 + value.getPrice(), accumulator.f1 + 1); // Add current price to the accumulator
        }

        @Override
        public Double getResult(Tuple2<Double, Integer> accumulator) {
            return accumulator.f0 / accumulator.f1; // Calculate average price
        }

        @Override
        public Tuple2<Double, Integer> merge(Tuple2<Double, Integer> a, Tuple2<Double, Integer> b) {
            return new Tuple2<>(a.f0 + b.f0, a.f1 + b.f1); // Merge two accumulators
        }
    }

    // CryptoPrice class with crypto name, price, and volume
    public static class CryptoPrice {
        private String cryptoName;
        private double price;
        private double volume;

        public CryptoPrice(String cryptoName, double price, double volume) {
            this.cryptoName = cryptoName;
            this.price = price;
            this.volume = volume;
        }

        public String getCryptoName() {
            return cryptoName;
        }

        public double getPrice() {
            return price;
        }

        public double getVolume() {
            return volume;
        }
    }
}
