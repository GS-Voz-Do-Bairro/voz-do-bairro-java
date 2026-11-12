package br.com.fiap.vozdobairro.messaging;

import br.com.fiap.vozdobairro.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Publica eventos JSON puros (UTF-8, camelCase, content_type=application/json, persistentes)
 * na exchange vozdobairro.events, sem cabecalhos de tipo do Spring (__TypeId__), conforme contrato.
 * Falha de publicacao (broker fora do ar etc.) e registrada em log e NAO propaga excecao.
 */
@Component
public class EventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventoPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EventoPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /** @return true se a mensagem foi entregue ao broker; false se falhou (ja logado). */
    public boolean publicar(String routingKey, String eventId, Object corpo) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(corpo);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setContentEncoding(StandardCharsets.UTF_8.name());
            props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            props.setMessageId(eventId);
            rabbitTemplate.send(RabbitConfig.EXCHANGE, routingKey, new Message(json, props));
            log.info("Evento publicado: {} eventId={}", routingKey, eventId);
            return true;
        } catch (Exception e) {
            log.error("Falha ao publicar evento {} eventId={} (a requisicao NAO foi afetada): {}",
                    routingKey, eventId, e.getMessage());
            return false;
        }
    }
}
