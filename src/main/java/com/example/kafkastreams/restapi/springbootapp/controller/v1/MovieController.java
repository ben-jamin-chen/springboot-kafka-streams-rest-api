package com.example.kafkastreams.restapi.springbootapp.controller.v1;

import com.example.kafkastreams.restapi.springbootapp.dto.MovieAverageRatingResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@OpenAPIDefinition(servers = { @Server(url = "http://localhost:7001") }, info = @Info(title = "Sample Spring Boot Kafka Stream API", version = "v1", description = "A demo project using Spring Boot with Kafka Streams.", license = @License(name = "MIT License", url = "https://github.com/bchen04/springboot-kafka-streams-rest-api/blob/master/LICENSE"), contact = @Contact(url = "https://www.linkedin.com/in/bchen04/", name = "Ben Chen")))
@RestController
@RequestMapping("v1/movie")
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
            @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(type = "object"))),
            @ApiResponse(responseCode = "500", description = "internal server error")})
    @GetMapping(value = "{movieId}/rating", produces = { "application/json" })
    public ResponseEntity<MovieAverageRatingResponse> getMovieAverageRating(@Parameter(description = "Movie identifier", required = true, example = "362") @PathVariable Long movieId) {
        try {
            //find active, standby host list and partition for key
            //get the metadata related to the key.
            final KeyQueryMetadata keyQueryMetadata = streams.queryMetadataForKey(stateStoreName, movieId, Serdes.Long().serializer());

            //use the above information to redirect the query to the host containing the partition for the key
            final int keyPartition = keyQueryMetadata.getPartition();

            //querying local key-value stores
            final QueryableStoreType<ReadOnlyKeyValueStore<Long, Double>> queryableStoreType = QueryableStoreTypes.keyValueStore();

            //fetch the store for specific partition where the key belongs and look into stale stores as well
            ReadOnlyKeyValueStore<Long, Double> store = streams
                    .store(StoreQueryParameters.fromNameAndType(stateStoreName, queryableStoreType)
                            .enableStaleStores()
                            .withPartition(keyPartition));

            //get the value by key
            Double result = store.get(movieId);

            return ResponseEntity
                    .ok()
                    .body(new MovieAverageRatingResponse(movieId, result));
        }
        catch(Exception ex) {
            logger.error("Failed due to exception: {}", ex.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
