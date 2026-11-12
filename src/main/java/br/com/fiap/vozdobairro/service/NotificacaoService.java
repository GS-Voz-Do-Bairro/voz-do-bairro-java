package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Notificacao;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.TipoNotificacao;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.NotificacaoResponse;
import br.com.fiap.vozdobairro.exception.AcessoNegadoException;
import br.com.fiap.vozdobairro.exception.RecursoNaoEncontradoException;
import br.com.fiap.vozdobairro.mapper.NotificacaoMapper;
import br.com.fiap.vozdobairro.repository.NotificacaoRepository;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PropostaRepository propostaRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              UsuarioRepository usuarioRepository,
                              PropostaRepository propostaRepository) {
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.propostaRepository = propostaRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificacaoResponse> listarDoUsuario(UsuarioLogado usuario, Pageable pageable) {
        return notificacaoRepository.findByUsuarioId(usuario.id(), pageable).map(NotificacaoMapper::paraResponse);
    }

    @Transactional
    public NotificacaoResponse marcarComoLida(Long id, UsuarioLogado usuario) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação " + id + " não encontrada."));
        if (!Objects.equals(notificacao.getUsuario().getId(), usuario.id())) {
            throw new AcessoNegadoException("Esta notificação pertence a outro usuário.");
        }
        notificacao.marcarComoLida();
        return NotificacaoMapper.paraResponse(notificacao);
    }

    /** Idempotencia dos consumidores: ja existe notificacao deste tipo para a proposta? */
    @Transactional(readOnly = true)
    public boolean jaNotificado(Long propostaId, TipoNotificacao tipo) {
        return notificacaoRepository.existsByPropostaIdAndMensagemStartingWith(propostaId, tipo.getPrefixo());
    }

    /**
     * Cria uma notificacao por usuario (ids repetidos/nulos sao ignorados).
     * Participa da transacao do chamador, se houver.
     */
    @Transactional
    public int notificar(Collection<Long> usuarioIds, Long propostaId, TipoNotificacao tipo, String texto) {
        Set<Long> destinatarios = new LinkedHashSet<>();
        for (Long id : usuarioIds) {
            if (id != null) {
                destinatarios.add(id);
            }
        }
        if (destinatarios.isEmpty()) {
            return 0;
        }
        String mensagem = limitar(tipo.getPrefixo() + texto);
        Proposta proposta = propostaId != null ? propostaRepository.getReferenceById(propostaId) : null;
        List<Notificacao> novas = new ArrayList<>();
        for (Long usuarioId : destinatarios) {
            Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
            novas.add(new Notificacao(usuario, proposta, mensagem));
        }
        notificacaoRepository.saveAll(novas);
        return novas.size();
    }

    private static String limitar(String mensagem) {
        int max = Notificacao.TAMANHO_MAXIMO_MENSAGEM;
        return mensagem.length() <= max ? mensagem : mensagem.substring(0, max - 3) + "...";
    }
}
