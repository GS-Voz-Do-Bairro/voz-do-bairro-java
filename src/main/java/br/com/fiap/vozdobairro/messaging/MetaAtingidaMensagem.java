package br.com.fiap.vozdobairro.messaging;

/**
 * Corpo de proposta.meta-atingida (publicado pela API .NET). Datas lidas como String para nao
 * depender do formato exato que o .NET serializa (o Java so precisa do propostaId).
 */
public record MetaAtingidaMensagem(String eventId, Long propostaId, Integer totalApoios, Integer metaApoios,
                                   String prazoResposta, String ocorridoEm) {
}
