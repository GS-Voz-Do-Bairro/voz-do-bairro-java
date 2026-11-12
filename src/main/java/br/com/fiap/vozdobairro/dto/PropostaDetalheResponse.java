package br.com.fiap.vozdobairro.dto;

import br.com.fiap.vozdobairro.domain.Categoria;
import br.com.fiap.vozdobairro.domain.StatusProposta;

import java.time.Instant;

/** Detalhe da proposta: inclui resumo IA, resposta do gestor (se houver) e total de comentarios. */
public record PropostaDetalheResponse(
        Long id,
        String titulo,
        String descricao,
        Categoria categoria,
        StatusProposta status,
        String resumoIa,
        Integer metaApoios,
        Integer totalApoios,
        Long autorId,
        String autorNome,
        Long bairroId,
        String bairroNome,
        Instant dtCriacao,
        Instant dtVirouDemanda,
        Instant prazoResposta,
        long totalComentarios,
        RespostaResponse resposta
) {
}
