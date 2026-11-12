package br.com.fiap.vozdobairro.ai;

import java.util.List;

/** Dados da proposta enviados ao LLM (sem dados pessoais: nem nome nem e-mail do autor). */
public record PropostaParaIa(
        String titulo,
        String descricao,
        String categoria,
        String bairro,
        String status,
        int totalApoios,
        int metaApoios,
        List<String> comentarios
) {
}
