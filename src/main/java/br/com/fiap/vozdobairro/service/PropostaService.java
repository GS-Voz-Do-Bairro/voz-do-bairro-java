package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.Categoria;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.domain.TipoNotificacao;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.AtualizarPropostaRequest;
import br.com.fiap.vozdobairro.dto.PropostaDetalheResponse;
import br.com.fiap.vozdobairro.dto.PropostaRequest;
import br.com.fiap.vozdobairro.dto.PropostaResponse;
import br.com.fiap.vozdobairro.dto.RespostaResponse;
import br.com.fiap.vozdobairro.exception.AcessoNegadoException;
import br.com.fiap.vozdobairro.exception.CampoInvalidoException;
import br.com.fiap.vozdobairro.exception.RecursoNaoEncontradoException;
import br.com.fiap.vozdobairro.exception.RegraNegocioException;
import br.com.fiap.vozdobairro.mapper.PropostaMapper;
import br.com.fiap.vozdobairro.mapper.RespostaMapper;
import br.com.fiap.vozdobairro.messaging.PropostaCriadaEvent;
import br.com.fiap.vozdobairro.repository.BairroRepository;
import br.com.fiap.vozdobairro.repository.ComentarioRepository;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import br.com.fiap.vozdobairro.repository.RespostaRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Regras de proposta (CONTRATO.md, secao 2). */
@Service
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final BairroRepository bairroRepository;
    private final UsuarioRepository usuarioRepository;
    private final RespostaRepository respostaRepository;
    private final ComentarioRepository comentarioRepository;
    private final NotificacaoService notificacaoService;
    private final ApplicationEventPublisher eventPublisher;

    public PropostaService(PropostaRepository propostaRepository,
                           BairroRepository bairroRepository,
                           UsuarioRepository usuarioRepository,
                           RespostaRepository respostaRepository,
                           ComentarioRepository comentarioRepository,
                           NotificacaoService notificacaoService,
                           ApplicationEventPublisher eventPublisher) {
        this.propostaRepository = propostaRepository;
        this.bairroRepository = bairroRepository;
        this.usuarioRepository = usuarioRepository;
        this.respostaRepository = respostaRepository;
        this.comentarioRepository = comentarioRepository;
        this.notificacaoService = notificacaoService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<PropostaResponse> listar(Long bairroId, StatusProposta status, Categoria categoria, Pageable pageable) {
        return propostaRepository.buscar(bairroId, status, categoria, pageable).map(PropostaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public Page<PropostaResponse> listarDoAutor(UsuarioLogado usuario, Pageable pageable) {
        return propostaRepository.buscarPorAutor(usuario.id(), pageable).map(PropostaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public PropostaDetalheResponse detalhar(Long id) {
        Proposta proposta = propostaRepository.buscarDetalhada(id)
                .orElseThrow(() -> naoEncontrada(id));
        long totalComentarios = comentarioRepository.countByPropostaId(id);
        RespostaResponse resposta = respostaRepository.buscarPorProposta(id)
                .map(RespostaMapper::paraResponse)
                .orElse(null);
        return PropostaMapper.paraDetalhe(proposta, totalComentarios, resposta);
    }

    /**
     * Cria a proposta ABERTA com META_APOIOS copiada de META_APOIOS_PADRAO do bairro e dispara
     * o evento interno que, apos o commit, publica proposta.criada no RabbitMQ (resumo IA assincrono).
     */
    @Transactional
    public PropostaResponse criar(PropostaRequest req, UsuarioLogado logado) {
        Usuario autor = usuarioRepository.findById(logado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + logado.id() + " não encontrado."));
        Bairro bairro = bairroRepository.findById(req.bairroId())
                .orElseThrow(() -> new CampoInvalidoException("bairroId", "bairro não encontrado"));

        Proposta proposta = new Proposta(req.titulo().trim(), req.descricao().trim(), req.categoria(), autor, bairro);
        propostaRepository.save(proposta);

        eventPublisher.publishEvent(new PropostaCriadaEvent(proposta.getId()));
        return PropostaMapper.paraResponse(proposta);
    }

    /** Somente o autor, somente enquanto ABERTA. */
    @Transactional
    public PropostaResponse atualizar(Long id, AtualizarPropostaRequest req, UsuarioLogado logado) {
        Proposta proposta = propostaRepository.buscarDetalhada(id).orElseThrow(() -> naoEncontrada(id));
        if (!proposta.isAutor(logado.id())) {
            throw new AcessoNegadoException("Somente o autor pode editar a proposta.");
        }
        if (proposta.getStatus() != StatusProposta.ABERTA) {
            throw new RegraNegocioException("Só é possível editar propostas com status ABERTA (status atual: "
                    + proposta.getStatus() + ").");
        }
        proposta.editar(req.titulo().trim(), req.descricao().trim(), req.categoria());
        return PropostaMapper.paraResponse(proposta);
    }

    /** Autor: so ABERTA e sem apoios. GESTOR: qualquer proposta. Demais: 403. */
    @Transactional
    public void excluir(Long id, UsuarioLogado logado) {
        Proposta proposta = propostaRepository.findById(id).orElseThrow(() -> naoEncontrada(id));
        if (!logado.isGestor()) {
            if (!proposta.isAutor(logado.id())) {
                throw new AcessoNegadoException("Somente o autor ou um gestor pode excluir a proposta.");
            }
            if (proposta.getStatus() != StatusProposta.ABERTA) {
                throw new RegraNegocioException("O autor só pode excluir propostas com status ABERTA.");
            }
            if (proposta.getTotalApoios() > 0) {
                throw new RegraNegocioException("A proposta já recebeu apoios e não pode mais ser excluída pelo autor.");
            }
        }
        // Filhos (resposta, comentarios, apoios, notificacoes) saem por ON DELETE CASCADE no banco.
        propostaRepository.delete(proposta);
    }

    /** GESTOR: EM_EXECUCAO -> CONCLUIDA, notificando o autor. */
    @Transactional
    public PropostaResponse concluir(Long id) {
        Proposta proposta = propostaRepository.buscarDetalhada(id).orElseThrow(() -> naoEncontrada(id));
        if (proposta.getStatus() != StatusProposta.EM_EXECUCAO) {
            throw new RegraNegocioException("Só propostas EM_EXECUCAO podem ser concluídas (status atual: "
                    + proposta.getStatus() + ").");
        }
        proposta.setStatus(StatusProposta.CONCLUIDA);
        notificacaoService.notificar(List.of(proposta.getAutor().getId()), proposta.getId(),
                TipoNotificacao.PROPOSTA_CONCLUIDA,
                "A proposta \"" + proposta.getTitulo() + "\" foi concluída pela prefeitura.");
        return PropostaMapper.paraResponse(proposta);
    }

    private static RecursoNaoEncontradoException naoEncontrada(Long id) {
        return new RecursoNaoEncontradoException("Proposta " + id + " não encontrada.");
    }
}
