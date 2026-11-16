package com.edusphere.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI edusphereOpenApi() {
        return new OpenAPI().info(new Info()
                .title("EduSphere API")
                .version("1.0.0")
                .description("E-learning platform API. Session cookie auth; all writes need the X-XSRF-TOKEN header "
                        + "(call GET /api/auth/csrf first)."));
    }
}
