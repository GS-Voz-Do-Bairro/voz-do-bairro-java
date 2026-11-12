package br.com.fiap.vozdobairro.messaging;

/** Corpo de proposta.criada (contrato, secao 5). Datas como String ISO-8601 UTC. */
public record PropostaCriadaMensagem(String eventId, Long propostaId, String ocorridoEm) {
}
