package com.example.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaStreamsProperties(String bootstrapServers,
                                     String schemaRegistryUrl,
                                     String applicationId,
                                     String applicationServer,
                                     String ratingTopic,
                                     String averageRatingTopic,
                                     String stateStore) {
}
