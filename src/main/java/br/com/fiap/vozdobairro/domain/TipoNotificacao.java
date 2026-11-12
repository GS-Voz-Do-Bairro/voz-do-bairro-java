package br.com.fiap.vozdobairro.domain;

/**
 * Tipos de notificacao. VB_NOTIFICACAO nao tem coluna de tipo, entao o tipo fica
 * codificado como prefixo fixo da MENSAGEM. Isso permite checar idempotencia
 * ("ja existe notificacao deste tipo para esta proposta?") sem alterar o schema.
 */
public enum TipoNotificacao {
    META_ATINGIDA("[Meta atingida] "),
    PRAZO_VENCIDO("[Prazo vencido] "),
    PROPOSTA_RESPONDIDA("[Proposta respondida] "),
    PROPOSTA_CONCLUIDA("[Proposta concluída] ");

    private final String prefixo;

    TipoNotificacao(String prefixo) {
        this.prefixo = prefixo;
    }

    public String getPrefixo() {
        return prefixo;
    }
}
