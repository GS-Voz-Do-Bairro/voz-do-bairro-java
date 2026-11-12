package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.AtualizarUsuarioRequest;
import br.com.fiap.vozdobairro.dto.UsuarioResponse;
import br.com.fiap.vozdobairro.exception.CampoInvalidoException;
import br.com.fiap.vozdobairro.exception.RecursoNaoEncontradoException;
import br.com.fiap.vozdobairro.mapper.UsuarioMapper;
import br.com.fiap.vozdobairro.repository.BairroRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BairroRepository bairroRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, BairroRepository bairroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.bairroRepository = bairroRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarLogado(UsuarioLogado logado) {
        return UsuarioMapper.paraResponse(carregar(logado.id()));
    }

    @Transactional
    public UsuarioResponse atualizarLogado(UsuarioLogado logado, AtualizarUsuarioRequest req) {
        Usuario usuario = carregar(logado.id());
        if (req.bairroId() == null) {
            if (!usuario.isGestor()) {
                throw new CampoInvalidoException("bairroId", "é obrigatório para cidadão");
            }
            usuario.setBairro(null);
        } else {
            Bairro bairro = bairroRepository.findById(req.bairroId())
                    .orElseThrow(() -> new CampoInvalidoException("bairroId", "bairro não encontrado"));
            usuario.setBairro(bairro);
        }
        usuario.setNome(req.nome().trim());
        return UsuarioMapper.paraResponse(usuario);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.listarComBairro(pageable).map(UsuarioMapper::paraResponse);
    }

    private Usuario carregar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado."));
    }
}
