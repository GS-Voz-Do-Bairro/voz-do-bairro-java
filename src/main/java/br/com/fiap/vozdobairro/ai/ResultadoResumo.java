package br.com.fiap.vozdobairro.ai;

/** Resumo gerado e a indicacao se veio do LLM (true) ou do fallback local (false). */
public record ResultadoResumo(String texto, boolean geradoPorIa) {
}
