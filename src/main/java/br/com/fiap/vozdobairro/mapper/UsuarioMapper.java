package br.com.fiap.vozdobairro.mapper;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.UsuarioResponse;
import br.com.fiap.vozdobairro.dto.UsuarioTokenResponse;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse paraResponse(Usuario usuario) {
        Bairro bairro = usuario.getBairro();
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                bairro != null ? bairro.getId() : null,
                bairro != null ? bairro.getNome() : null,
                DataHora.paraInstant(usuario.getDtCadastro()));
    }

    public static UsuarioTokenResponse paraToken(Usuario usuario) {
        Bairro bairro = usuario.getBairro();
        return new UsuarioTokenResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                bairro != null ? bairro.getId() : null);
    }
}
