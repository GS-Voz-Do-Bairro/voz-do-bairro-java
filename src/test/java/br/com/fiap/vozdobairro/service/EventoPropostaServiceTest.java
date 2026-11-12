package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.Categoria;
import br.com.fiap.vozdobairro.domain.Papel;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.domain.TipoNotificacao;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.repository.ApoioRepository;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoPropostaServiceTest {

    @Mock
    private PropostaRepository propostaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ApoioRepository apoioRepository;
    @Mock
    private NotificacaoService notificacaoService;

    private EventoPropostaService service;
    private Proposta proposta;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-30T12:00:00Z"), ZoneOffset.UTC);
        service = new EventoPropostaService(propostaRepository, usuarioRepository, apoioRepository,
                notificacaoService, clock);
        Bairro bairro = new Bairro(2L, "Capão Redondo", "Campo Limpo", "São Paulo", 30);
        Usuario autor = new Usuario("Maria Cidadã", "cidadao@vozdobairro.com", "hash", Papel.CIDADAO, bairro);
        autor.setId(1L);
        proposta = new Proposta("Iluminação na rua", "A rua fica escura à noite entre os números 100 e 400.",
                Categoria.SEGURANCA, autor, bairro);
        proposta.setId(10L);
        proposta.setStatus(StatusProposta.DEMANDA);
        proposta.setPrazoResposta(LocalDateTime.of(2026, 11, 27, 13, 0));
    }

    @Test
    void metaAtingidaNotificaAutorApoiadoresEGestores() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta));
        when(notificacaoService.jaNotificado(10L, TipoNotificacao.META_ATINGIDA)).thenReturn(false);
        when(apoioRepository.buscarIdsApoiadores(10L)).thenReturn(List.of(2L, 4L));
        when(usuarioRepository.findIdsByPapel(Papel.GESTOR)).thenReturn(List.of(3L));
        when(notificacaoService.notificar(anyCollection(), eq(10L), eq(TipoNotificacao.META_ATINGIDA), anyString()))
                .thenReturn(4);

        int criadas = service.processarMetaAtingida(10L);

        assertThat(criadas).isEqualTo(4);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(notificacaoService).notificar(captor.capture(), eq(10L), eq(TipoNotificacao.META_ATINGIDA), anyString());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(1L, 2L, 4L, 3L);
    }

    @Test
    void metaAtingidaReprocessadaNaoDuplicaNotificacao() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta));
        when(notificacaoService.jaNotificado(10L, TipoNotificacao.META_ATINGIDA)).thenReturn(true);

        int criadas = service.processarMetaAtingida(10L);

        assertThat(criadas).isZero();
        verify(notificacaoService, never()).notificar(anyCollection(), anyLong(), any(), anyString());
    }

    @Test
    void metaAtingidaDePropostaInexistenteEhIgnorada() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.empty());

        assertThat(service.processarMetaAtingida(10L)).isZero();
        verify(notificacaoService, never()).notificar(anyCollection(), anyLong(), any(), anyString());
    }

    @Test
    void prazoVencidoMarcaAtrasadaENotifica() {
        when(propostaRepository.marcarComoAtrasada(eq(10L), eq(StatusProposta.DEMANDA), eq(StatusProposta.ATRASADA),
                any(LocalDateTime.class))).thenReturn(1);
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta));
        when(usuarioRepository.findIdsByPapel(Papel.GESTOR)).thenReturn(List.of(3L));

        boolean alterou = service.processarPrazoVencido(10L);

        assertThat(alterou).isTrue();
        verify(notificacaoService).notificar(anyCollection(), eq(10L), eq(TipoNotificacao.PRAZO_VENCIDO), anyString());
    }

    @Test
    void prazoVencidoReprocessadoOuJaRespondidoNaoFazNada() {
        when(propostaRepository.marcarComoAtrasada(eq(10L), eq(StatusProposta.DEMANDA), eq(StatusProposta.ATRASADA),
                any(LocalDateTime.class))).thenReturn(0);

        boolean alterou = service.processarPrazoVencido(10L);

        assertThat(alterou).isFalse();
        verify(propostaRepository, never()).buscarDetalhada(anyLong());
        verify(notificacaoService, never()).notificar(anyCollection(), anyLong(), any(), anyString());
    }
}
