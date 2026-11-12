package br.com.fiap.vozdobairro.dto;

import br.com.fiap.vozdobairro.domain.Decisao;
import br.com.fiap.vozdobairro.domain.StatusProposta;

import java.time.Instant;
import java.time.LocalDate;

public record RespostaResponse(
        Long id,
        Long propostaId,
        StatusProposta statusProposta,
        Long gestorId,
        String gestorNome,
        Decisao decisao,
        String justificativa,
        LocalDate prazoExecucao,
        Instant dtResposta
) {
}
