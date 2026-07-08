package com.example.app.config;

import io.confluent.demo.CountAndSum;
import io.confluent.demo.Rating;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.util.Optional.ofNullable;
import static org.apache.kafka.common.serialization.Serdes.Double;
import static org.apache.kafka.common.serialization.Serdes.Long;
import static org.apache.kafka.streams.StreamsConfig.DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG;
import static org.apache.kafka.streams.kstream.Grouped.with;

@Configuration
public class KafkaStreamsConfig {
    private final KafkaStreamsProperties properties;

    public KafkaStreamsConfig(KafkaStreamsProperties properties) {
        this.properties = properties;
    }

    @Bean(destroyMethod = "close")
    public KafkaStreams kafkaStreams() {
        final Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, properties.applicationId());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, properties.schemaRegistryUrl());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Long().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Double().getClass());
        props.put(StreamsConfig.STATE_DIR_CONFIG, "data");
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, properties.applicationServer());
        props.put(DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final KafkaStreams kafkaStreams = new KafkaStreams(buildTopology(new StreamsBuilder()), props);
        kafkaStreams.start();

        return kafkaStreams;
    }

    Topology buildTopology(StreamsBuilder bldr) {
        KStream<Long, Rating> ratingStream = bldr.stream(properties.ratingTopic(),
                Consumed.with(Serdes.Long(), getRatingSerde()));

        SpecificAvroSerde<CountAndSum> countAndSumSerde = getCountAndSumSerde();

        KGroupedStream<Long, Double> ratingsById = ratingStream
                .map((key, rating) -> new KeyValue<>(rating.getMovieId(), rating.getRating()))
                .groupByKey(with(Long(), Double()));

        final KTable<Long, CountAndSum> ratingCountAndSum =
                ratingsById.aggregate(() -> new CountAndSum(0L, 0.0),
                        (key, value, aggregate) -> {
                            aggregate.setCount(aggregate.getCount() + 1);
                            aggregate.setSum(aggregate.getSum() + value);
                            return aggregate;
                        },
                        Materialized.with(Long(), countAndSumSerde));

        final KTable<Long, Double> ratingAverage =
                ratingCountAndSum.mapValues(value -> value.getSum() / value.getCount(),
                        Materialized.<Long, Double, KeyValueStore<Bytes, byte[]>>as(properties.stateStore())
                                .withKeySerde(Long())
                                .withValueSerde(Double()));

        ratingAverage
                .toStream()
                .to(properties.averageRatingTopic(), Produced.with(Long(), Double()));

        return bldr.build();
    }

    private SpecificAvroSerde<CountAndSum> getCountAndSumSerde() {
        SpecificAvroSerde<CountAndSum> serde = new SpecificAvroSerde<>();
        serde.configure(getSerdeConfig(), false);
        return serde;
    }

    private SpecificAvroSerde<Rating> getRatingSerde() {
        SpecificAvroSerde<Rating> serde = new SpecificAvroSerde<>();
        serde.configure(getSerdeConfig(), false);
        return serde;
    }

    private Map<String, String> getSerdeConfig() {
        final HashMap<String, String> map = new HashMap<>();
        map.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                ofNullable(properties.schemaRegistryUrl()).orElse(""));
        return map;
    }
}
