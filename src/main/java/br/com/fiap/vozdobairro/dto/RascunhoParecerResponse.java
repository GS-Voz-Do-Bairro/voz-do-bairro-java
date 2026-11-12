package br.com.fiap.vozdobairro.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Rascunho de parecer gerado por IA para apoiar o gestor. Não é salvo.")
public record RascunhoParecerResponse(
        Long propostaId,
        @Schema(description = "ACEITA, RECUSADA ou null (quando a IA não pôde sugerir)")
        String sugestaoDecisao,
        String justificativa,
        List<String> pontosDeAtencao,
        boolean geradoPorIa,
        String aviso
) {
}
