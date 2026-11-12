package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.ai.IaService;
import br.com.fiap.vozdobairro.ai.PropostaParaIa;
import br.com.fiap.vozdobairro.ai.ResultadoResumo;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.dto.RascunhoParecerResponse;
import br.com.fiap.vozdobairro.dto.ResumoIaResponse;
import br.com.fiap.vozdobairro.exception.RecursoNaoEncontradoException;
import br.com.fiap.vozdobairro.repository.ComentarioRepository;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Orquestra as funcionalidades de IA integradas ao fluxo:
 * (a) resumo neutro da proposta + discussao, gravado em RESUMO_IA (consumidor de proposta.criada e
 *     endpoint de regeneracao do gestor);
 * (b) rascunho de parecer para o gestor (nao e salvo).
 *
 * <p>Propositalmente NAO e @Transactional: a chamada ao LLM pode levar segundos e nao deve segurar
 * conexao/transacao do banco. A leitura usa join fetch e a escrita e um UPDATE pontual em RESUMO_IA.
 */
@Service
public class AssistenteIaService {

    private static final Logger log = LoggerFactory.getLogger(AssistenteIaService.class);

    /** Quantidade maxima de comentarios enviados ao LLM (controle de custo/tamanho do prompt). */
    static final int MAX_COMENTARIOS_NO_PROMPT = 30;

    private final PropostaRepository propostaRepository;
    private final ComentarioRepository comentarioRepository;
    private final IaService iaService;

    public AssistenteIaService(PropostaRepository propostaRepository,
                               ComentarioRepository comentarioRepository,
                               IaService iaService) {
        this.propostaRepository = propostaRepository;
        this.comentarioRepository = comentarioRepository;
        this.iaService = iaService;
    }

    /**
     * Usado pelo consumidor de proposta.criada. Idempotente: se a proposta ja tem resumo, nao refaz.
     * Se a proposta nao existe mais (excluida), apenas registra log.
     */
    public void gerarResumoSeAusente(Long propostaId) {
        Optional<Proposta> encontrada = propostaRepository.buscarDetalhada(propostaId);
        if (encontrada.isEmpty()) {
            log.warn("proposta.criada: proposta {} não existe mais; evento ignorado.", propostaId);
            return;
        }
        Proposta proposta = encontrada.get();
        if (StringUtils.hasText(proposta.getResumoIa())) {
            log.info("proposta.criada: proposta {} já possui resumo; evento ignorado (idempotência).", propostaId);
            return;
        }
        ResultadoResumo resultado = iaService.resumirProposta(montarDados(proposta));
        propostaRepository.atualizarResumoIa(propostaId, resultado.texto());
        log.info("Resumo da proposta {} gravado (geradoPorIa={}).", propostaId, resultado.geradoPorIa());
    }

    /** POST /api/propostas/{id}/resumo-ia (GESTOR): regenera sempre, de forma sincrona. */
    public ResumoIaResponse regenerarResumo(Long propostaId) {
        Proposta proposta = carregar(propostaId);
        ResultadoResumo resultado = iaService.resumirProposta(montarDados(proposta));
        propostaRepository.atualizarResumoIa(propostaId, resultado.texto());
        return new ResumoIaResponse(propostaId, resultado.texto(), resultado.geradoPorIa());
    }

    /** POST /api/propostas/{id}/resposta/rascunho-ia (GESTOR): devolve rascunho, nao salva. */
    public RascunhoParecerResponse rascunharParecer(Long propostaId) {
        Proposta proposta = carregar(propostaId);
        IaService.RascunhoGerado r = iaService.rascunharParecer(montarDados(proposta));
        String aviso = r.geradoPorIa()
                ? "Rascunho gerado por IA. Revise antes de usar: a decisão é do gestor."
                : "IA indisponível no momento; rascunho-modelo gerado sem IA.";
        return new RascunhoParecerResponse(propostaId, r.sugestaoDecisao(), r.justificativa(),
                r.pontosDeAtencao(), r.geradoPorIa(), aviso);
    }

    private Proposta carregar(Long propostaId) {
        return propostaRepository.buscarDetalhada(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta " + propostaId + " não encontrada."));
    }

    private PropostaParaIa montarDados(Proposta p) {
        List<String> comentarios = comentarioRepository.buscarTextos(p.getId(),
                PageRequest.of(0, MAX_COMENTARIOS_NO_PROMPT));
        return new PropostaParaIa(
                p.getTitulo(),
                p.getDescricao(),
                p.getCategoria().name(),
                p.getBairro().getNome(),
                p.getStatus().name(),
                p.getTotalApoios(),
                p.getMetaApoios() == null ? 0 : p.getMetaApoios(),
                comentarios);
    }
}
