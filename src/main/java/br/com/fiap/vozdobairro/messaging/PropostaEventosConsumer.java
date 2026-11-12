package br.com.fiap.vozdobairro.messaging;

import br.com.fiap.vozdobairro.config.RabbitConfig;
import br.com.fiap.vozdobairro.service.AssistenteIaService;
import br.com.fiap.vozdobairro.service.EventoPropostaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Consumidores assincronos dos 3 eventos do contrato.
 *
 * <p>Recebem a {@link Message} crua, convertem o corpo para String (UTF-8) e desserializam com o
 * ObjectMapper: o .NET publica JSON puro, sem cabecalhos de tipo do Spring.
 * Mensagens malformadas sao descartadas com log (nao adianta reprocessar). Erros de infraestrutura
 * (ex.: banco fora) propagam excecao para o retry do container (ver application.properties).
 */
@Component
public class PropostaEventosConsumer {

    private static final Logger log = LoggerFactory.getLogger(PropostaEventosConsumer.class);

    private final ObjectMapper objectMapper;
    private final AssistenteIaService assistenteIaService;
    private final EventoPropostaService eventoPropostaService;

    public PropostaEventosConsumer(ObjectMapper objectMapper,
                                   AssistenteIaService assistenteIaService,
                                   EventoPropostaService eventoPropostaService) {
        this.objectMapper = objectMapper;
        this.assistenteIaService = assistenteIaService;
        this.eventoPropostaService = eventoPropostaService;
    }

    /** proposta.criada -> gera RESUMO_IA com Spring AI (ou fallback). */
    @RabbitListener(queues = RabbitConfig.FILA_PROPOSTA_CRIADA)
    public void aoCriarProposta(Message message) {
        ler(message, PropostaCriadaMensagem.class, RabbitConfig.RK_PROPOSTA_CRIADA)
                .filter(m -> m.propostaId() != null)
                .ifPresent(m -> {
                    log.info("Recebido proposta.criada eventId={} propostaId={}", m.eventId(), m.propostaId());
                    assistenteIaService.gerarResumoSeAusente(m.propostaId());
                });
    }

    /** proposta.meta-atingida (publicado pelo .NET) -> notifica autor, apoiadores e gestores. */
    @RabbitListener(queues = RabbitConfig.FILA_META_ATINGIDA)
    public void aoAtingirMeta(Message message) {
        ler(message, MetaAtingidaMensagem.class, RabbitConfig.RK_META_ATINGIDA)
                .filter(m -> m.propostaId() != null)
                .ifPresent(m -> {
                    log.info("Recebido proposta.meta-atingida eventId={} propostaId={}", m.eventId(), m.propostaId());
                    eventoPropostaService.processarMetaAtingida(m.propostaId());
                });
    }

    /** proposta.prazo-vencido (publicado pelo agendador Java) -> DEMANDA vira ATRASADA e notifica. */
    @RabbitListener(queues = RabbitConfig.FILA_PRAZO_VENCIDO)
    public void aoVencerPrazo(Message message) {
        ler(message, PrazoVencidoMensagem.class, RabbitConfig.RK_PRAZO_VENCIDO)
                .filter(m -> m.propostaId() != null)
                .ifPresent(m -> {
                    log.info("Recebido proposta.prazo-vencido eventId={} propostaId={}", m.eventId(), m.propostaId());
                    eventoPropostaService.processarPrazoVencido(m.propostaId());
                });
    }

    private <T> Optional<T> ler(Message message, Class<T> tipo, String evento) {
        String corpo = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            T valor = objectMapper.readValue(corpo, tipo);
            if (valor == null) {
                log.warn("Evento {} com corpo vazio; descartado.", evento);
            }
            return Optional.ofNullable(valor);
        } catch (JsonProcessingException e) {
            log.warn("Evento {} com JSON inválido; descartado. Corpo: {}", evento, corpo);
            return Optional.empty();
        }
    }
}
