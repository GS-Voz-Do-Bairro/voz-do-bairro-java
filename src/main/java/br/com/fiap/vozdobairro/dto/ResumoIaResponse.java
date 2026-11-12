package br.com.fiap.vozdobairro.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResumoIaResponse(
        Long propostaId,
        String resumoIa,
        @Schema(description = "false quando o LLM não está configurado ou falhou e foi usado o resumo simples (fallback)")
        boolean geradoPorIa
) {
}
