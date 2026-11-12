package br.com.fiap.vozdobairro.dto;

import java.time.Instant;

public record NotificacaoResponse(
        Long id,
        Long propostaId,
        String mensagem,
        boolean lida,
        Instant dtCriacao
) {
}
