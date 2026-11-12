package br.com.fiap.vozdobairro.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte Boolean <-> NUMBER(1) (0/1). Usado em VB_NOTIFICACAO.LIDA.
 * Explicito para funcionar igual no Oracle (NUMBER(1)) e no H2 (INTEGER gerado pelo Hibernate),
 * sem depender do mapeamento padrao de boolean de cada dialeto.
 */
@Converter
public class BooleanNumeroConverter implements AttributeConverter<Boolean, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? 1 : 0;
    }

    @Override
    public Boolean convertToEntityAttribute(Integer valor) {
        return valor != null && valor == 1;
    }
}
