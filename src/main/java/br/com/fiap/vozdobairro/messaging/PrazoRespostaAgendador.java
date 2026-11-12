package br.com.fiap.vozdobairro.messaging;

import br.com.fiap.vozdobairro.config.RabbitConfig;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Agendador: procura DEMANDA com PRAZO_RESPOSTA vencido e sem resposta e publica proposta.prazo-vencido.
 * A mudanca de status acontece no consumidor (idempotente), entao rodar em mais de uma instancia
 * ou publicar duas vezes nao causa efeito duplicado.
 * Cron configuravel por vozdobairro.agendador.prazo-cron (padrao: a cada 10 minutos).
 */
@Component
@ConditionalOnProperty(name = "vozdobairro.agendador.habilitado", havingValue = "true", matchIfMissing = true)
public class PrazoRespostaAgendador {

    private static final Logger log = LoggerFactory.getLogger(PrazoRespostaAgendador.class);

    private final PropostaRepository propostaRepository;
    private final EventoPublisher publisher;
    private final Clock clock;

    public PrazoRespostaAgendador(PropostaRepository propostaRepository, EventoPublisher publisher, Clock clock) {
        this.propostaRepository = propostaRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Scheduled(cron = "${vozdobairro.agendador.prazo-cron:0 */10 * * * *}")
    public void verificarPrazosVencidos() {
        try {
            List<Proposta> vencidas = propostaRepository.buscarComPrazoVencido(StatusProposta.DEMANDA,
                    LocalDateTime.now(clock));
            if (!vencidas.isEmpty()) {
                log.info("Agendador: {} demanda(s) com prazo vencido.", vencidas.size());
            }
            for (Proposta p : vencidas) {
                String eventId = UUID.randomUUID().toString();
                String prazo = p.getPrazoResposta() == null ? null
                        : p.getPrazoResposta().toInstant(ZoneOffset.UTC).toString();
                PrazoVencidoMensagem msg = new PrazoVencidoMensagem(eventId, p.getId(), prazo,
                        clock.instant().toString());
                publisher.publicar(RabbitConfig.RK_PRAZO_VENCIDO, eventId, msg);
            }
        } catch (Exception e) {
            log.error("Agendador de prazos falhou nesta execução: {}", e.getMessage(), e);
        }
    }
}
