package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Papel;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.domain.TipoNotificacao;
import br.com.fiap.vozdobairro.repository.ApoioRepository;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Regras executadas pelos consumidores de meta-atingida e prazo-vencido. Ambas idempotentes. */
@Service
public class EventoPropostaService {

    private static final Logger log = LoggerFactory.getLogger(EventoPropostaService.class);

    private final PropostaRepository propostaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApoioRepository apoioRepository;
    private final NotificacaoService notificacaoService;
    private final Clock clock;

    public EventoPropostaService(PropostaRepository propostaRepository,
                                 UsuarioRepository usuarioRepository,
                                 ApoioRepository apoioRepository,
                                 NotificacaoService notificacaoService,
                                 Clock clock) {
        this.propostaRepository = propostaRepository;
        this.usuarioRepository = usuarioRepository;
        this.apoioRepository = apoioRepository;
        this.notificacaoService = notificacaoService;
        this.clock = clock;
    }

    /**
     * proposta.meta-atingida (publicado pela API .NET, que ja gravou STATUS=DEMANDA e PRAZO_RESPOSTA):
     * notifica autor, apoiadores e todos os GESTOR. Idempotente: se ja existe notificacao do tipo
     * META_ATINGIDA para a proposta, nao notifica de novo.
     *
     * @return quantidade de notificacoes criadas (0 se ignorado)
     */
    @Transactional
    public int processarMetaAtingida(Long propostaId) {
        Optional<Proposta> encontrada = propostaRepository.buscarDetalhada(propostaId);
        if (encontrada.isEmpty()) {
            log.warn("meta-atingida: proposta {} não encontrada; evento ignorado.", propostaId);
            return 0;
        }
        if (notificacaoService.jaNotificado(propostaId, TipoNotificacao.META_ATINGIDA)) {
            log.info("meta-atingida: proposta {} já notificada; evento ignorado (idempotência).", propostaId);
            return 0;
        }
        Proposta proposta = encontrada.get();
        List<Long> destinatarios = new ArrayList<>();
        destinatarios.add(proposta.getAutor().getId());
        destinatarios.addAll(apoioRepository.buscarIdsApoiadores(propostaId));
        destinatarios.addAll(usuarioRepository.findIdsByPapel(Papel.GESTOR));

        String texto = "A proposta \"" + proposta.getTitulo() + "\" (" + proposta.getBairro().getNome()
                + ") atingiu a meta de apoios e virou DEMANDA. A prefeitura tem até "
                + formatarPrazo(proposta.getPrazoResposta()) + " para responder.";
        int criadas = notificacaoService.notificar(destinatarios, propostaId, TipoNotificacao.META_ATINGIDA, texto);
        log.info("meta-atingida: {} notificação(ões) criada(s) para a proposta {}.", criadas, propostaId);
        return criadas;
    }

    /**
     * proposta.prazo-vencido: DEMANDA -> ATRASADA e notifica autor e GESTORes.
     * Idempotente: o UPDATE condicional so altera se a proposta ainda estiver em DEMANDA,
     * com prazo vencido e sem resposta; caso contrario nada acontece.
     *
     * @return true se a proposta foi marcada como ATRASADA agora
     */
    @Transactional
    public boolean processarPrazoVencido(Long propostaId) {
        int alteradas = propostaRepository.marcarComoAtrasada(propostaId, StatusProposta.DEMANDA,
                StatusProposta.ATRASADA, LocalDateTime.now(clock));
        if (alteradas == 0) {
            log.info("prazo-vencido: proposta {} não está mais em DEMANDA com prazo vencido; nada a fazer.", propostaId);
            return false;
        }
        Optional<Proposta> encontrada = propostaRepository.buscarDetalhada(propostaId);
        if (encontrada.isPresent()) {
            Proposta proposta = encontrada.get();
            List<Long> destinatarios = new ArrayList<>();
            destinatarios.add(proposta.getAutor().getId());
            destinatarios.addAll(usuarioRepository.findIdsByPapel(Papel.GESTOR));
            String texto = "O prazo de resposta da demanda \"" + proposta.getTitulo()
                    + "\" venceu sem resposta da prefeitura. Status: ATRASADA.";
            notificacaoService.notificar(destinatarios, propostaId, TipoNotificacao.PRAZO_VENCIDO, texto);
        }
        log.info("prazo-vencido: proposta {} marcada como ATRASADA.", propostaId);
        return true;
    }

    private static String formatarPrazo(LocalDateTime prazo) {
        if (prazo == null) {
            return "o prazo definido";
        }
        return String.format("%02d/%02d/%04d (UTC)", prazo.getDayOfMonth(), prazo.getMonthValue(), prazo.getYear());
    }
}
