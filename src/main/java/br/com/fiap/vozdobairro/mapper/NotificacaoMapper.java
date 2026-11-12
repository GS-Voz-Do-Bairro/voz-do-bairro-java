package br.com.fiap.vozdobairro.mapper;

import br.com.fiap.vozdobairro.domain.Notificacao;
import br.com.fiap.vozdobairro.dto.NotificacaoResponse;

public final class NotificacaoMapper {

    private NotificacaoMapper() {
    }

    public static NotificacaoResponse paraResponse(Notificacao n) {
        // getId() de um proxy LAZY do Hibernate nao dispara consulta.
        Long propostaId = n.getProposta() != null ? n.getProposta().getId() : null;
        return new NotificacaoResponse(
                n.getId(),
                propostaId,
                n.getMensagem(),
                Boolean.TRUE.equals(n.getLida()),
                DataHora.paraInstant(n.getDtCriacao()));
    }
}
