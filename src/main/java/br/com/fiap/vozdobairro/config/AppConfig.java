package br.com.fiap.vozdobairro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Configuracoes gerais.
 * - Clock UTC injetavel (datas gravadas em UTC; facilita testes).
 * - @EnableAsync: publicacao no RabbitMQ apos o commit nao bloqueia a requisicao.
 * - @EnableScheduling: agendador de prazos vencidos.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
