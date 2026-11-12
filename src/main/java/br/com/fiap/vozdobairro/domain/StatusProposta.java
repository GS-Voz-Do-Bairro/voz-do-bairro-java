package br.com.fiap.vozdobairro.domain;

/** Status aceitos pelo CHECK CK_VB_PROPOSTA_STATUS. Transicoes descritas no CONTRATO.md, secao 2. */
public enum StatusProposta {
    ABERTA,
    DEMANDA,
    ATRASADA,
    EM_EXECUCAO,
    CONCLUIDA,
    RECUSADA;

    /** Proposta aguardando a resposta da prefeitura (gestor). */
    public boolean aguardaResposta() {
        return this == DEMANDA || this == ATRASADA;
    }
}
