package br.com.fiap.vozdobairro.messaging;

import br.com.fiap.vozdobairro.config.RabbitConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.util.UUID;

/**
 * Publica proposta.criada SOMENTE apos o commit da transacao que criou a proposta
 * (evita o consumidor buscar uma proposta que ainda nao esta visivel no banco ou que sofreu rollback).
 * Roda em thread separada (@Async) para que lentidao/queda do broker nao atrase a resposta HTTP.
 */
@Component
public class PropostaCriadaEventListener {

    private final EventoPublisher publisher;
    private final Clock clock;

    public PropostaCriadaEventListener(EventoPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoCriarProposta(PropostaCriadaEvent evento) {
        String eventId = UUID.randomUUID().toString();
        PropostaCriadaMensagem mensagem = new PropostaCriadaMensagem(eventId, evento.propostaId(),
                clock.instant().toString());
        publisher.publicar(RabbitConfig.RK_PROPOSTA_CRIADA, eventId, mensagem);
    }
}
