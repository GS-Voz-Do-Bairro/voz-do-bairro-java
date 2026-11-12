package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.Categoria;
import br.com.fiap.vozdobairro.domain.Papel;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.domain.TipoNotificacao;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.AtualizarPropostaRequest;
import br.com.fiap.vozdobairro.dto.PropostaRequest;
import br.com.fiap.vozdobairro.dto.PropostaResponse;
import br.com.fiap.vozdobairro.exception.AcessoNegadoException;
import br.com.fiap.vozdobairro.exception.CampoInvalidoException;
import br.com.fiap.vozdobairro.exception.RegraNegocioException;
import br.com.fiap.vozdobairro.messaging.PropostaCriadaEvent;
import br.com.fiap.vozdobairro.repository.BairroRepository;
import br.com.fiap.vozdobairro.repository.ComentarioRepository;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import br.com.fiap.vozdobairro.repository.RespostaRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
class PropostaServiceTest {

    @Mock
    private PropostaRepository propostaRepository;
    @Mock
    private BairroRepository bairroRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RespostaRepository respostaRepository;
    @Mock
    private ComentarioRepository comentarioRepository;
    @Mock
    private NotificacaoService notificacaoService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PropostaService service;

    private Bairro bairro;
    private Usuario autor;

    private final UsuarioLogado logadoAutor = new UsuarioLogado(1L, Papel.CIDADAO);
    private final UsuarioLogado logadoOutroCidadao = new UsuarioLogado(2L, Papel.CIDADAO);
    private final UsuarioLogado logadoGestor = new UsuarioLogado(3L, Papel.GESTOR);

    @BeforeEach
    void setUp() {
        bairro = new Bairro(2L, "Capão Redondo", "Campo Limpo", "São Paulo", 30);
        autor = new Usuario("Maria Cidadã", "cidadao@vozdobairro.com", "hash", Papel.CIDADAO, bairro);
        autor.setId(1L);
    }

    private Proposta proposta(StatusProposta status, int apoios) {
        Proposta p = new Proposta("Iluminação na rua", "A rua fica escura à noite entre os números 100 e 400.",
                Categoria.SEGURANCA, autor, bairro);
        p.setId(10L);
        p.setStatus(status);
        p.setTotalApoios(apoios);
        return p;
    }

    @Test
    void criarDeveGerarPropostaAbertaComMetaDoBairroEPublicarEvento() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(autor));
        when(bairroRepository.findById(2L)).thenReturn(Optional.of(bairro));
        when(propostaRepository.save(any(Proposta.class))).thenAnswer(inv -> {
            Proposta p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        PropostaRequest req = new PropostaRequest("Iluminação na rua",
                "A rua fica escura à noite entre os números 100 e 400.", Categoria.SEGURANCA, 2L);
        PropostaResponse resp = service.criar(req, logadoAutor);

        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.status()).isEqualTo(StatusProposta.ABERTA);
        assertThat(resp.metaApoios()).isEqualTo(30);
        assertThat(resp.totalApoios()).isZero();
        assertThat(resp.autorId()).isEqualTo(1L);
        verify(eventPublisher).publishEvent(new PropostaCriadaEvent(10L));
    }

    @Test
    void criarComBairroInexistenteDeveFalharNoCampoBairroId() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(autor));
        when(bairroRepository.findById(99L)).thenReturn(Optional.empty());

        PropostaRequest req = new PropostaRequest("Iluminação na rua",
                "A rua fica escura à noite entre os números 100 e 400.", Categoria.SEGURANCA, 99L);

        assertThatThrownBy(() -> service.criar(req, logadoAutor))
                .isInstanceOf(CampoInvalidoException.class)
                .extracting("campo").isEqualTo("bairroId");
        verify(propostaRepository, never()).save(any());
    }

    @Test
    void autorPodeEditarPropostaAberta() {
        Proposta p = proposta(StatusProposta.ABERTA, 0);
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(p));

        PropostaResponse resp = service.atualizar(10L, new AtualizarPropostaRequest("Novo título",
                "Descrição nova e suficientemente longa.", Categoria.ZELADORIA), logadoAutor);

        assertThat(resp.titulo()).isEqualTo("Novo título");
        assertThat(resp.categoria()).isEqualTo(Categoria.ZELADORIA);
    }

    @Test
    void naoAutorNaoPodeEditar() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta(StatusProposta.ABERTA, 0)));

        assertThatThrownBy(() -> service.atualizar(10L, new AtualizarPropostaRequest("Novo título",
                "Descrição nova e suficientemente longa.", Categoria.ZELADORIA), logadoOutroCidadao))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void autorNaoPodeEditarPropostaQueJaVirouDemanda() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta(StatusProposta.DEMANDA, 30)));

        assertThatThrownBy(() -> service.atualizar(10L, new AtualizarPropostaRequest("Novo título",
                "Descrição nova e suficientemente longa.", Categoria.ZELADORIA), logadoAutor))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void autorExcluiPropostaAbertaSemApoios() {
        Proposta p = proposta(StatusProposta.ABERTA, 0);
        when(propostaRepository.findById(10L)).thenReturn(Optional.of(p));

        service.excluir(10L, logadoAutor);

        verify(propostaRepository).delete(p);
    }

    @Test
    void autorNaoExcluiPropostaComApoios() {
        when(propostaRepository.findById(10L)).thenReturn(Optional.of(proposta(StatusProposta.ABERTA, 3)));

        assertThatThrownBy(() -> service.excluir(10L, logadoAutor)).isInstanceOf(RegraNegocioException.class);
        verify(propostaRepository, never()).delete(any(Proposta.class));
    }

    @Test
    void autorNaoExcluiPropostaQueNaoEstaAberta() {
        when(propostaRepository.findById(10L)).thenReturn(Optional.of(proposta(StatusProposta.RECUSADA, 0)));

        assertThatThrownBy(() -> service.excluir(10L, logadoAutor)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void outroCidadaoNaoExclui() {
        when(propostaRepository.findById(10L)).thenReturn(Optional.of(proposta(StatusProposta.ABERTA, 0)));

        assertThatThrownBy(() -> service.excluir(10L, logadoOutroCidadao)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void gestorExcluiQualquerProposta() {
        Proposta p = proposta(StatusProposta.EM_EXECUCAO, 40);
        when(propostaRepository.findById(10L)).thenReturn(Optional.of(p));

        service.excluir(10L, logadoGestor);

        verify(propostaRepository).delete(p);
    }

    @Test
    void concluirMudaEmExecucaoParaConcluidaENotificaAutor() {
        Proposta p = proposta(StatusProposta.EM_EXECUCAO, 30);
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(p));

        PropostaResponse resp = service.concluir(10L);

        assertThat(resp.status()).isEqualTo(StatusProposta.CONCLUIDA);
        verify(notificacaoService).notificar(eq(List.of(1L)), eq(10L), eq(TipoNotificacao.PROPOSTA_CONCLUIDA),
                anyString());
    }

    @Test
    void naoConcluiPropostaQueNaoEstaEmExecucao() {
        when(propostaRepository.buscarDetalhada(10L)).thenReturn(Optional.of(proposta(StatusProposta.DEMANDA, 30)));

        assertThatThrownBy(() -> service.concluir(10L)).isInstanceOf(RegraNegocioException.class);
    }
}
