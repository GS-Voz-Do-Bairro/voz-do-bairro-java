package br.com.fiap.vozdobairro.mapper;

import br.com.fiap.vozdobairro.domain.Resposta;
import br.com.fiap.vozdobairro.dto.RespostaResponse;

/** Deve ser chamado dentro de transacao (gestor e proposta sao LAZY). */
public final class RespostaMapper {

    private RespostaMapper() {
    }

    public static RespostaResponse paraResponse(Resposta r) {
        return new RespostaResponse(
                r.getId(),
                r.getProposta().getId(),
                r.getProposta().getStatus(),
                r.getGestor().getId(),
                r.getGestor().getNome(),
                r.getDecisao(),
                r.getJustificativa(),
                r.getPrazoExecucao(),
                DataHora.paraInstant(r.getDtResposta()));
    }
}
