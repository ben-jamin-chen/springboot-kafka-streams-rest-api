package com.example.kafkastreams.restapi.springbootapp.controller.v1;

import com.example.kafkastreams.restapi.springbootapp.dto.MovieAverageRatingResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreType;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenAPIDefinition(servers = { @Server(url = "http://localhost:7001") }, info = @Info(title = "Sample Spring Boot Kafka Stream API", version = "v1", description = "A demo project using Spring Boot with Kafka Stream", license = @License(name = "MIT License", url = "https://github.com/bchen04/springboot-swagger-rest-api/blob/master/LICENSE"), contact = @Contact(url = "https://www.linkedin.com/in/bchen04/", name = "Ben Chen")))
@RestController
public class MovieController {
    private final KafkaStreams streams;
    private static final Logger logger = LoggerFactory.getLogger(MovieController.class);

    @Autowired
    public MovieController(KafkaStreams streams) {
        this.streams = streams;
    }

    @Value("${state.store.name}")
    private String stateStoreName;

    @Operation(summary = "Returns the average rating for a particular movie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(type = "object"))) })
    @GetMapping(value = "/rating", produces = { "application/json" })
    public MovieAverageRatingResponse getMovieAverageRating(@RequestParam Long movieId) {
        try {
            final KeyQueryMetadata keyQueryMetadata = streams.queryMetadataForKey(stateStoreName, movieId, Serdes.Long().serializer());

            final int keyPartition = keyQueryMetadata.getPartition();

            final QueryableStoreType<ReadOnlyKeyValueStore<Long, Double>> queryableStoreType = QueryableStoreTypes.keyValueStore();

            //fetch the store for specific partition “keyPartition” where the key belongs and look into stale stores as well
            ReadOnlyKeyValueStore<Long, Double> store = streams
                    .store(StoreQueryParameters.fromNameAndType(stateStoreName, queryableStoreType)
                            .enableStaleStores()
                            .withPartition(keyPartition));

            Double result = store.get(movieId);

            return new MovieAverageRatingResponse(movieId, result);
        }
        catch(Exception ex) {
            logger.error("Failed due to exception: {}", ex.getMessage());
            return null;
        }
    }
}
