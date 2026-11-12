package br.com.fiap.vozdobairro.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ do contrato (secao 5). O RabbitAdmin do Spring Boot declara tudo
 * na primeira conexao com o broker (declaracao idempotente).
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "vozdobairro.events";

    public static final String RK_PROPOSTA_CRIADA = "proposta.criada";
    public static final String RK_META_ATINGIDA = "proposta.meta-atingida";
    public static final String RK_PRAZO_VENCIDO = "proposta.prazo-vencido";

    public static final String FILA_PROPOSTA_CRIADA = "vb.proposta.criada.resumo-ia";
    public static final String FILA_META_ATINGIDA = "vb.proposta.meta-atingida";
    public static final String FILA_PRAZO_VENCIDO = "vb.proposta.prazo-vencido";

    @Bean
    public Declarables vozDoBairroTopologia() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);

        Queue filaCriada = QueueBuilder.durable(FILA_PROPOSTA_CRIADA).build();
        Queue filaMeta = QueueBuilder.durable(FILA_META_ATINGIDA).build();
        Queue filaPrazo = QueueBuilder.durable(FILA_PRAZO_VENCIDO).build();

        Binding bCriada = BindingBuilder.bind(filaCriada).to(exchange).with(RK_PROPOSTA_CRIADA);
        Binding bMeta = BindingBuilder.bind(filaMeta).to(exchange).with(RK_META_ATINGIDA);
        Binding bPrazo = BindingBuilder.bind(filaPrazo).to(exchange).with(RK_PRAZO_VENCIDO);

        return new Declarables(exchange, filaCriada, filaMeta, filaPrazo, bCriada, bMeta, bPrazo);
    }
}
