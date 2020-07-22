# Sample Spring Boot (2.3.1) RESTful API with Kafka Stream (2.5.0)

While searching through GitHub for some boilerplate code on how to setup a Spring Boot API project with Kafka Stream, I found it quite difficult to find a working example with a more recent version of Spring Boot and Java (i.e. 14). Anyways, I thought I’d create my own and share with everyone. This is for anyone that needs some quick boilerplate code to setup their new API project with Kafka Stream.

## What You Need

* Java 14
* Maven 3.2+
* Docker 19+

## Initialize the project topics and data
To get started, we need to first launch the Confluent services (i.e. Schema Registry, Brokers, ZooKeeper) locally by running the `docker-compose up -d` CLI command on the [docker-compose.yml](https://github.com/bchen04/springboot-kafka-streams-rest-api/blob/master/docker-compose.yml) file. Typically, you can create a stack file (in the form of a YAML file) to define your applications.

> Note: You can run `docker-compose down` to stop it.

As part of this sample, I've retrofitted the average aggregate example from [Confluent's Kafka Tutorials](https://kafka-tutorials.confluent.io/aggregating-average/kstreams.html). The API will calculate and return a running average rating for a given movie identifier. This should demonstrate how to build a basic API service on top of an aggregation result. 

So open a new terminal and run the following commands to generate your input and output topics.

```bash
$  docker-compose exec broker kafka-topics --create --bootstrap-server \
   localhost:9092 --replication-factor 1 --partitions 1 --topic ratings

$  docker-compose exec broker kafka-topics --create --bootstrap-server \
   localhost:9092 --replication-factor 1 --partitions 1 --topic rating-averages
```

Next, we will need to produce some data onto the input topic.

```bash
$  docker exec -i schema-registry /usr/bin/kafka-avro-console-producer --topic ratings --broker-list broker:9092\
    --property "parse.key=false"\
    --property "key.separator=:"\
    --property value.schema="$(< src/main/avro/rating.avsc)"
 ```
 
Paste in the following `json` data when promoted and be sure to press enter twice to actually submit it.

```json
{"movie_id":362,"rating":10}
{"movie_id":362,"rating":8}
 ```

Optionally, you can also see the consumer results on the output topic by running this command:

```bash
$  docker exec -it broker /usr/bin/kafka-console-consumer --topic rating-averages --bootstrap-server broker:9092 \
    --property "print.key=true"\
    --property "key.deserializer=org.apache.kafka.common.serialization.LongDeserializer" \
    --property "value.deserializer=org.apache.kafka.common.serialization.DoubleDeserializer" \
    --from-beginning
```

## Build and run the sample

You can import the code straight into your preferred IDE or run the sample using the following command (in the root project folder).

```bash
$  mvn spring-boot:run
```
After the application runs, navigate to `http://localhost:7001/swagger-ui/index.html?configUrl=/api-docs/swagger-config` in your web browser to access the Swagger UI. If you used the same sample data from above, you can enter `362` as the movieId and it should return something similar like this below.

```json
{
  "movieId": 362,
  "rating": 10.0
}
```
