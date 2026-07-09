package com.example.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(servers = { @Server(url = "http://localhost:7001") }, info = @Info(title = "Sample Spring Boot Kafka Stream API", version = "v1", description = "A demo project using Spring Boot with Kafka Streams.", license = @License(name = "MIT License", url = "https://github.com/ben-jamin-chen/springboot-kafka-streams-rest-api/blob/main/LICENSE"), contact = @Contact(url = "https://www.linkedin.com/in/ben-jamin-chen/", name = "Ben Chen")))
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
}
