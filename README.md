# Spring Boot (2.3.3) RESTful API with Kafka Streams (2.6.0)

While looking through the Kafka Tutorials to see how I could setup a Spring Boot API project with Kafka Streams, I found it strange that there wasn't a complete or more informative example on how this could be achieved. Most use cases demonstrated how to compute aggregations and how to build simple topologies, but it was difficult to find a concrete example on how to build an API service that could query into these materialized name stores. Anyways, I thought I’d create my own using a more recent version of Spring Boot with Java 14.

## What You Need

* Java 14
* Maven 3.6.0+
* Docker 19+

## Getting Started
We need to first launch the Confluent services (i.e. Schema Registry, Broker, ZooKeeper) locally by running the `docker-compose up -d` CLI command where the [docker-compose.yml](https://github.com/bchen04/springboot-kafka-streams-rest-api/blob/master/docker-compose.yml) file is. Typically, you can create a stack file (in the form of a YAML file) to define your applications. You can also run `docker-compose ps` to check the status of the stack. Notice, the endpoints from within the containers on your host machine.

| Name | From within containers | From host machine |
| ------------- | ------------- | ------------- |
| Kafka Broker | broker:9092 | localhost:9092 |
| Schema Registry  | http://schema-registry:8081 | http://localhost:8081 |
| ZooKeeper | zookeeper:2181 | localhost:2181 |

> Note: you can run `docker-compose down` to stop all services and containers.

As part of this sample, I've retrofitted the average aggregate example from [Confluent's Kafka Tutorials](https://kafka-tutorials.confluent.io/aggregating-average/kstreams.html) into this project. The API will calculate and return a running average rating for a given movie identifier. This should demonstrate how to build a basic API service on top of an aggregation result.

Notice in the `~/src/main/avro` directory, we have all our Avro schema files for the stream of `ratings` and `countsum`. For your convenience, the classes were already generated under the `~/src/main/java/io/confluent/demo` directory, but feel free to tinker with them and recompile the schemas if needed. The Avro classes can be programmatically generated using `Maven` or by manually invoking the [schema compiler](https://avro.apache.org/docs/1.10.0/gettingstartedjava.html#Compiling+the+schema). 

So before building and running the project, open a new terminal and run the following commands to generate your input and output topics.

```zsh
$  docker-compose exec broker kafka-topics --create --bootstrap-server \
   localhost:9092 --replication-factor 1 --partitions 1 --topic ratings

$  docker-compose exec broker kafka-topics --create --bootstrap-server \
   localhost:9092 --replication-factor 1 --partitions 1 --topic rating-averages
```

Next, we will need to produce some data onto the input topic.

```zsh
$  docker exec -i schema-registry /usr/bin/kafka-avro-console-producer --topic ratings --broker-list broker:9092\
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
$  docker exec -it broker /usr/bin/kafka-console-consumer --topic rating-averages --bootstrap-server broker:9092 \
    --property "print.key=true"\
    --property "key.deserializer=org.apache.kafka.common.serialization.LongDeserializer" \
    --property "value.deserializer=org.apache.kafka.common.serialization.DoubleDeserializer" \
    --from-beginning
```

## Build and Run the Sample

You can import the code straight into your preferred IDE or run the sample using the following command (in the root project folder).

```zsh
$  mvn spring-boot:run
```
After the application runs, navigate to [http://localhost:7001/swagger-ui/index.html?configUrl=/api-docs/swagger-config](http://localhost:7001/swagger-ui/index.html?configUrl=/api-docs/swagger-config) in your web browser to access the Swagger UI. If you used the same sample data from above, you can enter `362` as the `movieId` and it should return something similar like this below:

```json
{
  "movieId": 362,
  "rating": 9
}
```

> Note: keep in mind the various [states](https://kafka.apache.org/25/javadoc/org/apache/kafka/streams/KafkaStreams.State.html) of the client. When a Kafka Streams instance is in `RUNNING` state, it allows for inspection of the stream's metadata using methods like `queryMetadataForKey()`. While it is in `REBALANCING` state, the REST service cannot immediately answer requests until the state stores are fully rebuilt.

## Troubleshooting

* In certain conditions, you may need to do a complete application reset. You can delete the application’s local state directory where the application instance was run. In this project, Kafka Streams persists local states under the `~/data` folder.
