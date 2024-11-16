package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class CryptoKafkaProducer {

    private static final String KAFKA_TOPIC = "crypto_prices";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String API_KEY = "CG-UT1RRAscqb37omG6kpf8JHyA"; // Your CoinGecko API key

    public static void main(String[] args) {
        // Configure Kafka producer properties
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Fetch data from CoinGecko API with API key in header
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin,ethereum,binancecoin"))
                            .header("Authorization", "Bearer " + API_KEY) // Modify as per API documentation
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // Parse JSON response
                    JsonNode coins = objectMapper.readTree(response.body());
                    for (JsonNode coin : coins) {
                        String coinName = coin.get("name").asText();
                        double coinPrice = coin.get("current_price").asDouble();

                        // Create CryptoPrice object
                        CryptoPrice cryptoPrice = new CryptoPrice(coinName, coinPrice);

                        // Serialize the CryptoPrice object to JSON
                        String message = objectMapper.writeValueAsString(cryptoPrice);

                        // Send data to Kafka
                        producer.send(new ProducerRecord<>(KAFKA_TOPIC, coinName, message));
                        System.out.println("Sent: " + message);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 60000); // Fetch data every 60 seconds
    }

    // CryptoPrice class to represent cryptocurrency data
    public static class CryptoPrice {
        private String cryptoName;
        private double price;

        public CryptoPrice(String cryptoName, double price) {
            this.cryptoName = cryptoName;
            this.price = price;
        }

        public String getCryptoName() {
            return cryptoName;
        }

        public double getPrice() {
            return price;
        }

        @Override
        public String toString() {
            return "CryptoPrice{" +
                    "cryptoName='" + cryptoName + '\'' +
                    ", price=" + price +
                    '}';
        }
    }
}
