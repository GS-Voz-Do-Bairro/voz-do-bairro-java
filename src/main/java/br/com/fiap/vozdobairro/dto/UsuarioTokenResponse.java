package br.com.fiap.vozdobairro.dto;

import br.com.fiap.vozdobairro.domain.Papel;

/** Bloco "usuario" da resposta de login/registro (contrato, secao 4). */
public record UsuarioTokenResponse(
        Long id,
        String nome,
        String email,
        Papel papel,
        Long bairroId
) {
}
