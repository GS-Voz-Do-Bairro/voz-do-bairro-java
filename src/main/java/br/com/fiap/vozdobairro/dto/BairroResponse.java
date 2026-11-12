package br.com.fiap.vozdobairro.dto;

public record BairroResponse(
        Long id,
        String nome,
        String subprefeitura,
        String cidade,
        Integer metaApoiosPadrao
) {
}
