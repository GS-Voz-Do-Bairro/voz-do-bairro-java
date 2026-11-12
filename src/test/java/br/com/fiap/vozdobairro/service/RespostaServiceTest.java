package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.Categoria;
import br.com.fiap.vozdobairro.domain.Decisao;
import br.com.fiap.vozdobairro.domain.Papel;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.Resposta;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.domain.TipoNotificacao;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.RespostaRequest;
import br.com.fiap.vozdobairro.dto.RespostaResponse;
import br.com.fiap.vozdobairro.exception.CampoInvalidoException;
import br.com.fiap.vozdobairro.exception.ConflitoException;
import br.com.fiap.vozdobairro.exception.RegraNegocioException;
import br.com.fiap.vozdobairro.repository.ApoioRepository;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import br.com.fiap.vozdobairro.repository.RespostaRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespostaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 15);

    @Mock
    private RespostaRepository respostaRepository;
    @Mock
    private PropostaRepository propostaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ApoioRepository apoioRepository;
    @Mock
    private NotificacaoService notificacaoService;

    private RespostaService service;
    private Usuario autor;
    private Usuario gestor;
    private Bairro bairro;
    private final UsuarioLogado logadoGestor = new UsuarioLogado(3L, Papel.GESTOR);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);
        service = new RespostaService(respostaRepository, propostaRepository, usuarioRepository,
                apoioRepository, notificacaoService, clock);
        bairro = new Bairro(2L, "Capão Redondo", "Campo Limpo", "São Paulo", 30);
        autor = new Usuario("Maria Cidadã", "cidadao@vozdobairro.com", "hash", Papel.CIDADAO, bairro);
        autor.setId(1L);
        gestor = new Usuario("Carla Gestora", "gestor@vozdobairro.com", "hash", Papel.GESTOR, null);
        gestor.setId(3L);
    }

    private Proposta proposta(StatusProposta status) {
        Proposta p = new Proposta("Iluminação na rua", "A rua fica escura à noite entre os números 100 e 400.",
                Categoria.SEGURANCA, autor, bairro);
        p.setId(10L);
        p.setStatus(status);
        p.setTotalApoios(30);
        return p;
    }

    private void prepararRespostaValida(Proposta p) {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(p));
        when(respostaRepository.existsByPropostaId(10L)).thenReturn(false);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(gestor));
        when(apoioRepository.buscarIdsApoiadores(10L)).thenReturn(List.of(2L));
        when(respostaRepository.save(any(Resposta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aceitaComPrazoFuturoLevaParaEmExecucaoENotificaAutorEApoiadores() {
        Proposta p = proposta(StatusProposta.DEMANDA);
        prepararRespostaValida(p);

        RespostaResponse resp = service.responder(10L,
                new RespostaRequest(Decisao.ACEITA, "Incluída no cronograma.", HOJE.plusDays(30)), logadoGestor);

        assertThat(p.getStatus()).isEqualTo(StatusProposta.EM_EXECUCAO);
        assertThat(resp.statusProposta()).isEqualTo(StatusProposta.EM_EXECUCAO);
        assertThat(resp.prazoExecucao()).isEqualTo(HOJE.plusDays(30));
        assertThat(resp.gestorId()).isEqualTo(3L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(notificacaoService).notificar(captor.capture(), eq(10L), eq(TipoNotificacao.PROPOSTA_RESPONDIDA),
                anyString());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void propostaAtrasadaTambemPodeSerAceita() {
        Proposta p = proposta(StatusProposta.ATRASADA);
        prepararRespostaValida(p);

        service.responder(10L, new RespostaRequest(Decisao.ACEITA, "Ok.", HOJE.plusDays(10)), logadoGestor);

        assertThat(p.getStatus()).isEqualTo(StatusProposta.EM_EXECUCAO);
    }

    @Test
    void recusadaLevaParaRecusadaSemPrazo() {
        Proposta p = proposta(StatusProposta.DEMANDA);
        prepararRespostaValida(p);

        RespostaResponse resp = service.responder(10L,
                new RespostaRequest(Decisao.RECUSADA, "Área particular, fora da competência.", HOJE.plusDays(5)),
                logadoGestor);

        assertThat(p.getStatus()).isEqualTo(StatusProposta.RECUSADA);
        assertThat(resp.prazoExecucao()).isNull();
    }

    @Test
    void aceitaSemPrazoExecucaoEhInvalida() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta(StatusProposta.DEMANDA)));
        when(respostaRepository.existsByPropostaId(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.responder(10L,
                new RespostaRequest(Decisao.ACEITA, "Ok.", null), logadoGestor))
                .isInstanceOf(CampoInvalidoException.class)
                .extracting("campo").isEqualTo("prazoExecucao");
        verify(respostaRepository, never()).save(any());
    }

    @Test
    void aceitaComPrazoNoPassadoOuHojeEhInvalida() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta(StatusProposta.DEMANDA)));
        when(respostaRepository.existsByPropostaId(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.responder(10L,
                new RespostaRequest(Decisao.ACEITA, "Ok.", HOJE), logadoGestor))
                .isInstanceOf(CampoInvalidoException.class);
    }

    @Test
    void naoRespondePropostaAberta() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta(StatusProposta.ABERTA)));
        when(respostaRepository.existsByPropostaId(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.responder(10L,
                new RespostaRequest(Decisao.RECUSADA, "Não.", null), logadoGestor))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoRespondeDuasVezes() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta(StatusProposta.DEMANDA)));
        when(respostaRepository.existsByPropostaId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.responder(10L,
                new RespostaRequest(Decisao.RECUSADA, "Não.", null), logadoGestor))
                .isInstanceOf(ConflitoException.class);
    }
}
