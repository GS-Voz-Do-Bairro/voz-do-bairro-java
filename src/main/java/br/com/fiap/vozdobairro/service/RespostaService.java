package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Decisao;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.Resposta;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.domain.TipoNotificacao;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.AtualizarRespostaRequest;
import br.com.fiap.vozdobairro.dto.RespostaRequest;
import br.com.fiap.vozdobairro.dto.RespostaResponse;
import br.com.fiap.vozdobairro.exception.CampoInvalidoException;
import br.com.fiap.vozdobairro.exception.ConflitoException;
import br.com.fiap.vozdobairro.exception.RecursoNaoEncontradoException;
import br.com.fiap.vozdobairro.exception.RegraNegocioException;
import br.com.fiap.vozdobairro.mapper.RespostaMapper;
import br.com.fiap.vozdobairro.repository.ApoioRepository;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import br.com.fiap.vozdobairro.repository.RespostaRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resposta do gestor a uma demanda (CONTRATO.md, secao 2):
 * DEMANDA/ATRASADA -> EM_EXECUCAO (ACEITA, prazoExecucao futuro obrigatorio)
 * DEMANDA/ATRASADA -> RECUSADA (RECUSADA, justificativa obrigatoria).
 */
@Service
public class RespostaService {

    private final RespostaRepository respostaRepository;
    private final PropostaRepository propostaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApoioRepository apoioRepository;
    private final NotificacaoService notificacaoService;
    private final Clock clock;

    public RespostaService(RespostaRepository respostaRepository,
                           PropostaRepository propostaRepository,
                           UsuarioRepository usuarioRepository,
                           ApoioRepository apoioRepository,
                           NotificacaoService notificacaoService,
                           Clock clock) {
        this.respostaRepository = respostaRepository;
        this.propostaRepository = propostaRepository;
        this.usuarioRepository = usuarioRepository;
        this.apoioRepository = apoioRepository;
        this.notificacaoService = notificacaoService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RespostaResponse buscar(Long propostaId) {
        if (!propostaRepository.existsById(propostaId)) {
            throw new RecursoNaoEncontradoException("Proposta " + propostaId + " não encontrada.");
        }
        return respostaRepository.buscarPorProposta(propostaId)
                .map(RespostaMapper::paraResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "A proposta " + propostaId + " ainda não foi respondida."));
    }

    @Transactional
    public RespostaResponse responder(Long propostaId, RespostaRequest req, UsuarioLogado logado) {
        Proposta proposta = propostaRepository.buscarDetalhada(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta " + propostaId + " não encontrada."));
        if (respostaRepository.existsByPropostaId(propostaId)) {
            throw new ConflitoException("Esta proposta já foi respondida. Use PUT para editar a resposta.");
        }
        if (!proposta.getStatus().aguardaResposta()) {
            throw new RegraNegocioException("Só é possível responder propostas com status DEMANDA ou ATRASADA "
                    + "(status atual: " + proposta.getStatus() + ").");
        }

        LocalDate prazoExecucao = null;
        if (req.decisao() == Decisao.ACEITA) {
            validarPrazoExecucao(req.prazoExecucao());
            prazoExecucao = req.prazoExecucao();
            proposta.setStatus(StatusProposta.EM_EXECUCAO);
        } else {
            proposta.setStatus(StatusProposta.RECUSADA);
        }

        Usuario gestor = usuarioRepository.findById(logado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + logado.id() + " não encontrado."));
        Resposta resposta = new Resposta(proposta, gestor, req.decisao(), req.justificativa().trim(),
                prazoExecucao, LocalDateTime.now(clock));
        respostaRepository.save(resposta);

        notificarAutorEApoiadores(proposta, req.decisao());
        return RespostaMapper.paraResponse(resposta);
    }

    /** GESTOR edita justificativa/prazo. A decisao nao muda. */
    @Transactional
    public RespostaResponse atualizar(Long propostaId, AtualizarRespostaRequest req) {
        Resposta resposta = respostaRepository.buscarPorProposta(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "A proposta " + propostaId + " ainda não foi respondida."));
        if (resposta.getProposta().getStatus() == StatusProposta.CONCLUIDA) {
            throw new RegraNegocioException("A proposta já foi concluída; a resposta não pode mais ser editada.");
        }
        LocalDate prazo = null;
        if (resposta.getDecisao() == Decisao.ACEITA) {
            validarPrazoExecucao(req.prazoExecucao());
            prazo = req.prazoExecucao();
        }
        resposta.atualizar(req.justificativa().trim(), prazo);
        return RespostaMapper.paraResponse(resposta);
    }

    private void validarPrazoExecucao(LocalDate prazo) {
        if (prazo == null) {
            throw new CampoInvalidoException("prazoExecucao", "é obrigatório quando a decisão é ACEITA");
        }
        if (!prazo.isAfter(LocalDate.now(clock))) {
            throw new CampoInvalidoException("prazoExecucao", "deve ser uma data futura");
        }
    }

    private void notificarAutorEApoiadores(Proposta proposta, Decisao decisao) {
        List<Long> destinatarios = new ArrayList<>();
        destinatarios.add(proposta.getAutor().getId());
        destinatarios.addAll(apoioRepository.buscarIdsApoiadores(proposta.getId()));
        String texto = decisao == Decisao.ACEITA
                ? "A prefeitura ACEITOU a demanda \"" + proposta.getTitulo() + "\". Ela agora está em execução."
                : "A prefeitura RECUSOU a demanda \"" + proposta.getTitulo() + "\". Veja a justificativa.";
        notificacaoService.notificar(destinatarios, proposta.getId(), TipoNotificacao.PROPOSTA_RESPONDIDA, texto);
    }
}
