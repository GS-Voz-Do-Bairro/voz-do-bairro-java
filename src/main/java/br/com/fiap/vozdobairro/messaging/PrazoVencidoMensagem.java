package br.com.fiap.vozdobairro.messaging;

/** Corpo de proposta.prazo-vencido (contrato, secao 5). */
public record PrazoVencidoMensagem(String eventId, Long propostaId, String prazoResposta, String ocorridoEm) {
}
