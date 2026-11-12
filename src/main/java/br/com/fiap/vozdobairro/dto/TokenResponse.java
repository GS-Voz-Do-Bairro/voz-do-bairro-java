package br.com.fiap.vozdobairro.dto;

import java.time.Instant;

/** Resposta de login/registro exatamente como no CONTRATO.md (secao 4). */
public record TokenResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioTokenResponse usuario
) {
}
