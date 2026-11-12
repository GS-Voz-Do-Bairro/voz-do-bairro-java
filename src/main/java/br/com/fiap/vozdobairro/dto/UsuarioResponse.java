package br.com.fiap.vozdobairro.dto;

import br.com.fiap.vozdobairro.domain.Papel;

import java.time.Instant;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Papel papel,
        Long bairroId,
        String bairroNome,
        Instant dtCadastro
) {
}
