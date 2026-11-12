package br.com.fiap.vozdobairro.messaging;

/**
 * Evento interno (Spring ApplicationEvent) disparado dentro da transacao de criacao da proposta.
 * O {@link PropostaCriadaEventListener} so o publica no RabbitMQ depois do COMMIT.
 */
public record PropostaCriadaEvent(Long propostaId) {
}
