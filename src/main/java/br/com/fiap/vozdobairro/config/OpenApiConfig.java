package br.com.fiap.vozdobairro.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger/OpenAPI com esquema de seguranca Bearer (JWT). UI em /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI vozDoBairroOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Voz do Bairro - API Java")
                        .version("1.0.0")
                        .description("""
                                Propostas de melhorias para o bairro, respostas da prefeitura, notificações, \
                                placar público e apoio de IA (Spring AI). Global Solution FIAP 2026/2 - CitizenTech.
                                Faça login em /api/auth/login, copie o token e clique em "Authorize".\
                                """))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido em /api/auth/login ou /api/auth/register")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
