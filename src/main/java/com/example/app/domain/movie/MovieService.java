package com.example.app.domain.movie;

import com.example.app.config.KafkaStreamsProperties;
import com.example.app.exception.MovieNotFoundException;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Service;

@Service
public class MovieService {
    private final KafkaStreams streams;
    private final String stateStoreName;

    public MovieService(KafkaStreams streams, KafkaStreamsProperties properties) {
        this.streams = streams;
        this.stateStoreName = properties.stateStore();
    }

    public Double getAverageRating(Long movieId) {
        final KeyQueryMetadata keyQueryMetadata =
                streams.queryMetadataForKey(stateStoreName, movieId, Serdes.Long().serializer());

        final ReadOnlyKeyValueStore<Long, Double> store = streams.store(
                StoreQueryParameters.fromNameAndType(stateStoreName, QueryableStoreTypes.<Long, Double>keyValueStore())
                        .enableStaleStores()
                        .withPartition(keyQueryMetadata.partition()));

        final Double rating = store.get(movieId);

        if (rating == null) {
            throw new MovieNotFoundException(movieId);
        }

        return rating;
    }
}
