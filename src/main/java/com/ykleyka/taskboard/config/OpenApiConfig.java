package com.ykleyka.taskboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskBoardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskBoard API")
                        .version("v1")
                        .description(
                                "REST API for managing users, projects, tasks, tags and task comments.")
                        .contact(new Contact().name("TaskBoard Team")));
    }
}
