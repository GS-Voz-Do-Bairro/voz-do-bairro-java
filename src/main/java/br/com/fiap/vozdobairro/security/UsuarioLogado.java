package br.com.fiap.vozdobairro.security;

import br.com.fiap.vozdobairro.domain.Papel;
import br.com.fiap.vozdobairro.exception.AcessoNegadoException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Dados do usuario autenticado extraidos do JWT (sub e role). */
public record UsuarioLogado(Long id, Papel papel) {

    public static UsuarioLogado de(Jwt jwt) {
        if (jwt == null) {
            throw new AcessoNegadoException("Token ausente.");
        }
        try {
            Long id = Long.valueOf(jwt.getSubject());
            Papel papel = Papel.valueOf(jwt.getClaimAsString(JwtConfig.CLAIM_ROLE));
            return new UsuarioLogado(id, papel);
        } catch (RuntimeException e) {
            throw new AcessoNegadoException("Token sem as claims esperadas (sub numérico e role).");
        }
    }

    public boolean isGestor() {
        return papel == Papel.GESTOR;
    }
}
