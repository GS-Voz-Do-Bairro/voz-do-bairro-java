package br.com.fiap.vozdobairro.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.fiap.vozdobairro.service.AssistenteIaService;
import br.com.fiap.vozdobairro.service.EventoPropostaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;

/** Teste de contrato: o JSON exato que a API .NET publica (CONTRATO.md secao 5) e lido pelo consumidor. */
@ExtendWith(MockitoExtension.class)
class PropostaEventosConsumerTest {

    @Mock AssistenteIaService assistenteIaService;
    @Mock EventoPropostaService eventoPropostaService;

    private PropostaEventosConsumer consumer() {
        return new PropostaEventosConsumer(new ObjectMapper(), assistenteIaService, eventoPropostaService);
    }

    private static Message msg(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void metaAtingidaNoFormatoDoDotNetEProcessada() {
        String json = "{\"eventId\":\"0b0c6a3e-6f3e-4a77-9b1e-1d6f0f0f0f0f\",\"propostaId\":10,\"totalApoios\":30,"
                + "\"metaApoios\":30,\"prazoResposta\":\"2026-11-27T13:00:00Z\",\"ocorridoEm\":\"2026-11-12T13:00:00.1234567Z\"}";
        consumer().aoAtingirMeta(msg(json));
        verify(eventoPropostaService).processarMetaAtingida(10L);
    }

    @Test
    void jsonInvalidoEDescartadoSemChamarServico() {
        consumer().aoAtingirMeta(msg("isto nao e json"));
        verifyNoInteractions(eventoPropostaService);
    }
}
