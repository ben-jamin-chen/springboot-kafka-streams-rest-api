# Spring Boot (4.1) RESTful API with Kafka Streams (4.3)

While looking through the Kafka Tutorials to see how I could setup a Spring Boot API project with Kafka Streams, I found it strange that there wasn't a complete or more informative example on how this could be achieved. Most use cases demonstrated how to compute aggregations and how to build simple topologies, but it was difficult to find a concrete example on how to build an API service that could query into these materialized name stores. Anyways, I thought I’d create my own using a more recent version of Spring Boot with Java 25.

## What You Need

* Java 25+
* Maven 3.9+ (or just use the included Maven wrapper, `./mvnw`)
* Docker 19+

## Getting Started
We need to first launch the Confluent services (i.e. Schema Registry and a KRaft-mode Kafka broker — no ZooKeeper required anymore) locally by running the `docker compose up -d` CLI command where the [compose.yaml](https://github.com/bchen04/springboot-kafka-streams-rest-api/blob/master/compose.yaml) file is. Typically, you can create a stack file (in the form of a YAML file) to define your applications. You can also run `docker compose ps` to check the status of the stack. Notice, the endpoints from within the containers on your host machine.

| Name | From within containers | From host machine |
| ------------- | ------------- | ------------- |
| Kafka Broker | broker:9092 | localhost:29092 |
| Schema Registry  | http://schema-registry:8081 | http://localhost:8081 |

> Note: you can run `docker compose down` to stop all services and containers.

As part of this sample, I've retrofitted the average aggregate example from [Confluent's Kafka Tutorials](https://kafka-tutorials.confluent.io/aggregating-average/kstreams.html) into this project. The API will calculate and return a running average rating for a given movie identifier. This should demonstrate how to build a basic API service on top of an aggregation result.

Notice in the `~/src/main/avro` directory, we have all our Avro schema files for the stream of `ratings` and `countsum`. The Java classes are generated automatically during the build (via the `avro-maven-plugin`) into `target/generated-sources/avro`, so they are no longer committed to the source tree. Feel free to tinker with the schemas and rebuild — see the [Avro getting started guide](https://avro.apache.org/docs/1.12.1/getting-started-java/) for details.

So before building and running the project, open a new terminal and run the following commands to generate your input and output topics.

```zsh
docker compose exec broker kafka-topics --create --bootstrap-server \
broker:9092 --replication-factor 1 --partitions 1 --topic ratings

docker compose exec broker kafka-topics --create --bootstrap-server \
broker:9092 --replication-factor 1 --partitions 1 --topic rating-averages
```

Next, we will need to produce some data onto the input topic.

```zsh
docker exec -i schema-registry /usr/bin/kafka-avro-console-producer --topic ratings --bootstrap-server broker:9092\
 --property "parse.key=false"\
 --property "key.separator=:"\
 --property value.schema="$(< src/main/avro/rating.avsc)"
 ```

Paste in the following `json` data when prompted and be sure to press enter twice to actually submit it.

```json
{"movie_id":362,"rating":10}
{"movie_id":362,"rating":8}
 ```

Optionally, you can also see the consumer results on the output topic by running this command on a new terminal window:

```zsh
docker exec -it broker /usr/bin/kafka-console-consumer --topic rating-averages --bootstrap-server broker:9092 \
 --property "print.key=true"\
 --property "key.deserializer=org.apache.kafka.common.serialization.LongDeserializer" \
 --property "value.deserializer=org.apache.kafka.common.serialization.DoubleDeserializer" \
 --from-beginning
```

## Project Structure

The API uses Spring's [functional endpoints](https://docs.spring.io/spring-framework/reference/web/webmvc-functional.html) (WebMvc.fn) — routes are declared in a `RouterFunction` bean and handled by plain handler/service classes, feature-packaged by domain.

```
src/main/java/com/example/app/
├── Application.java                     # Main entry point
├── config/
│   ├── KafkaStreamsConfig.java          # KafkaStreams bean & topology
│   ├── KafkaStreamsProperties.java      # @ConfigurationProperties record (app.kafka.*)
│   └── OpenApiConfig.java               # OpenAPI metadata
├── exception/
│   ├── GlobalErrorHandler.java          # Maps exceptions to RFC 7807 ProblemDetail responses
│   └── MovieNotFoundException.java
└── domain/movie/
    ├── MovieRoutes.java                 # RouterFunction endpoints (+ OpenAPI docs)
    ├── MovieHandler.java                # HTTP request/response mapping
    ├── MovieService.java                # Interactive query against the state store
    └── dto/MovieAverageRatingResponse.java
```

> Note: requesting a movie without any ratings now returns a `404` problem-details response instead of a `200` with a null rating.

## Build and Run the Sample

You can import the code straight into your preferred IDE or run the sample using the following command (in the root project folder).

```zsh
./mvnw spring-boot:run
```
After the application runs, navigate to [http://localhost:7001/swagger-ui/index.html](http://localhost:7001/swagger-ui/index.html) in your web browser to access the Swagger UI. If you used the same sample data from above, you can enter `362` as the `movieId` and it should return something similar like this below:

```json
{
  "movieId": 362,
  "rating": 9
}
```

You can also build and run it as a container (multi-stage `Dockerfile` included):

```zsh
docker build -t springboot-kafka-streams-rest-api .

docker run --rm -p 7001:7001 --network cp_network \
 -e APP_KAFKA_BOOTSTRAP_SERVERS=broker:9092 \
 -e APP_KAFKA_SCHEMA_REGISTRY_URL=http://schema-registry:8081 \
 springboot-kafka-streams-rest-api
```

> Note: keep in mind the various [states](https://kafka.apache.org/43/javadoc/org/apache/kafka/streams/KafkaStreams.State.html) of the client. When a Kafka Streams instance is in `RUNNING` state, it allows for inspection of the stream's metadata using methods like `queryMetadataForKey()`. While it is in `REBALANCING` state, the REST service cannot immediately answer requests until the state stores are fully rebuilt.

## Troubleshooting

* In certain conditions, you may need to do a complete application reset. You can delete the application’s local state directory where the application instance was run. In this project, Kafka Streams persists local states under the `~/data` folder.
