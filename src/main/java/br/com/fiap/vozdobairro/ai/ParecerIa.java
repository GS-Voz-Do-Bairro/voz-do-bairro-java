package br.com.fiap.vozdobairro.ai;

import java.util.List;

/**
 * Saida estruturada do LLM para o rascunho de parecer. Preenchida via ChatClient...call().entity(ParecerIa.class):
 * o Spring AI (BeanOutputConverter) gera um JSON Schema deste record, anexa as instrucoes de formato
 * ao prompt e converte a resposta JSON de volta para o record.
 *
 * @param sugestaoDecisao  "ACEITA" ou "RECUSADA"
 * @param justificativa    texto sugerido para a justificativa da resposta oficial
 * @param pontosDeAtencao  itens que o gestor deve verificar antes de decidir
 */
public record ParecerIa(
        String sugestaoDecisao,
        String justificativa,
        List<String> pontosDeAtencao
) {
}
