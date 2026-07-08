package com.example.app.config;

import io.confluent.demo.Rating;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaStreamsTopologyTests {
    private static final String SCHEMA_REGISTRY_URL = "mock://topology-tests";
    private static final String RATING_TOPIC = "ratings";
    private static final String AVERAGE_RATING_TOPIC = "rating-averages";
    private static final String STATE_STORE = "average-ratings";

    @Test
    void computesRunningAverageRatingPerMovie() {
        KafkaStreamsProperties properties = new KafkaStreamsProperties("localhost:29092", SCHEMA_REGISTRY_URL,
                "topology-tests", "localhost:7001", RATING_TOPIC, AVERAGE_RATING_TOPIC, STATE_STORE);
        Topology topology = new KafkaStreamsConfig(properties).buildTopology(new StreamsBuilder());

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "topology-tests");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.LongSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.DoubleSerde.class);

        try (TopologyTestDriver driver = new TopologyTestDriver(topology, props)) {
            SpecificAvroSerde<Rating> ratingSerde = new SpecificAvroSerde<>();
            ratingSerde.configure(
                    Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL), false);

            TestInputTopic<Long, Rating> ratings = driver.createInputTopic(RATING_TOPIC,
                    Serdes.Long().serializer(), ratingSerde.serializer());
            TestOutputTopic<Long, Double> averages = driver.createOutputTopic(AVERAGE_RATING_TOPIC,
                    Serdes.Long().deserializer(), Serdes.Double().deserializer());

            ratings.pipeInput(362L, new Rating(362L, 9.6));
            ratings.pipeInput(362L, new Rating(362L, 8.4));

            assertEquals(9.0, averages.readKeyValuesToMap().get(362L));
            assertEquals(9.0, driver.<Long, Double>getKeyValueStore(STATE_STORE).get(362L));
        }
    }
}
