package br.com.fiap.vozdobairro.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IaServiceTest {

    private static PropostaParaIa dados(String descricao, List<String> comentarios) {
        return new PropostaParaIa("Iluminação na rua", descricao, "SEGURANCA", "Capão Redondo", "ABERTA",
                3, 30, comentarios);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ChatClient.Builder> provider(ChatClient.Builder builder) {
        ObjectProvider<ChatClient.Builder> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(builder);
        return provider;
    }

    @Test
    void semChaveConfiguradaUsaFallbackESemChamarOLlm() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        IaService ia = new IaService(provider(builder), IaService.CHAVE_PLACEHOLDER, true);

        ResultadoResumo r = ia.resumirProposta(dados("A rua fica escura à noite.", List.of("Concordo", "Urgente")));

        assertThat(ia.isHabilitada()).isFalse();
        assertThat(r.geradoPorIa()).isFalse();
        assertThat(r.texto()).contains("A rua fica escura à noite.").contains("2 comentários");
        verify(builder, never()).build();
    }

    @Test
    void fallbackRespeitaLimiteDaColunaResumoIa() {
        IaService ia = new IaService(provider(null), "", true);

        ResultadoResumo r = ia.resumirProposta(dados("x".repeat(5000), List.of()));

        assertThat(r.texto().length()).isLessThanOrEqualTo(IaService.TAMANHO_MAXIMO_RESUMO);
    }

    @Test
    void falhaNaChamadaDoLlmNaoPropagaExcecao() {
        ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenThrow(new RuntimeException("provedor fora do ar"));
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);

        IaService ia = new IaService(provider(builder), "chave-qualquer", true);
        ResultadoResumo resumo = ia.resumirProposta(dados("A rua fica escura à noite.", List.of()));
        IaService.RascunhoGerado rascunho = ia.rascunharParecer(dados("A rua fica escura à noite.", List.of()));

        assertThat(ia.isHabilitada()).isTrue();
        assertThat(resumo.geradoPorIa()).isFalse();
        assertThat(rascunho.geradoPorIa()).isFalse();
        assertThat(rascunho.sugestaoDecisao()).isNull();
        assertThat(rascunho.pontosDeAtencao()).isNotEmpty();
    }

    @Test
    void iaDesligadaPorConfiguracao() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        IaService ia = new IaService(provider(builder), "chave-qualquer", false);

        assertThat(ia.isHabilitada()).isFalse();
    }

    @Test
    void normalizaDecisaoSugerida() {
        assertThat(IaService.normalizarDecisao(" aceita ")).isEqualTo("ACEITA");
        assertThat(IaService.normalizarDecisao("RECUSADA")).isEqualTo("RECUSADA");
        assertThat(IaService.normalizarDecisao("talvez")).isNull();
        assertThat(IaService.normalizarDecisao(null)).isNull();
    }

    @Test
    void sanitizaChavesParaNaoVirarTemplate() {
        assertThat(IaService.sanitizar("texto {com} chaves\n e  espaços")).isEqualTo("texto (com) chaves e espaços");
    }
}
