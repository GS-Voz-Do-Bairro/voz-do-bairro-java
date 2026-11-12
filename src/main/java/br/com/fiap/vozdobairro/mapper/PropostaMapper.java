package br.com.fiap.vozdobairro.mapper;

import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.dto.PropostaDetalheResponse;
import br.com.fiap.vozdobairro.dto.PropostaResponse;
import br.com.fiap.vozdobairro.dto.RespostaResponse;

/** Deve ser chamado dentro de transacao (ou com autor/bairro ja carregados via join fetch). */
public final class PropostaMapper {

    private PropostaMapper() {
    }

    public static PropostaResponse paraResponse(Proposta p) {
        return new PropostaResponse(
                p.getId(),
                p.getTitulo(),
                p.getDescricao(),
                p.getCategoria(),
                p.getStatus(),
                p.getResumoIa(),
                p.getMetaApoios(),
                p.getTotalApoios(),
                p.getAutor().getId(),
                p.getAutor().getNome(),
                p.getBairro().getId(),
                p.getBairro().getNome(),
                DataHora.paraInstant(p.getDtCriacao()),
                DataHora.paraInstant(p.getDtVirouDemanda()),
                DataHora.paraInstant(p.getPrazoResposta()));
    }

    public static PropostaDetalheResponse paraDetalhe(Proposta p, long totalComentarios, RespostaResponse resposta) {
        return new PropostaDetalheResponse(
                p.getId(),
                p.getTitulo(),
                p.getDescricao(),
                p.getCategoria(),
                p.getStatus(),
                p.getResumoIa(),
                p.getMetaApoios(),
                p.getTotalApoios(),
                p.getAutor().getId(),
                p.getAutor().getNome(),
                p.getBairro().getId(),
                p.getBairro().getNome(),
                DataHora.paraInstant(p.getDtCriacao()),
                DataHora.paraInstant(p.getDtVirouDemanda()),
                DataHora.paraInstant(p.getPrazoResposta()),
                totalComentarios,
                resposta);
    }
}
