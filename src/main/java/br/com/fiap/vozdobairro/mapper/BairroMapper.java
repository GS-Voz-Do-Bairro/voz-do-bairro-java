package br.com.fiap.vozdobairro.mapper;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.dto.BairroResponse;

public final class BairroMapper {

    private BairroMapper() {
    }

    public static BairroResponse paraResponse(Bairro bairro) {
        return new BairroResponse(
                bairro.getId(),
                bairro.getNome(),
                bairro.getSubprefeitura(),
                bairro.getCidade(),
                bairro.getMetaApoiosPadrao());
    }
}
