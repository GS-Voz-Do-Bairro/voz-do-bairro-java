package br.com.fiap.vozdobairro.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Placar público de um bairro")
public record PlacarResponse(
        Long bairroId,
        String bairroNome,
        long totalPropostas,
        @Schema(description = "Quantidade de propostas por status (todos os status aparecem, com 0 quando não houver)")
        Map<String, Long> porStatus,
        @Schema(description = "Demandas que já receberam resposta do gestor")
        long demandasRespondidas,
        @Schema(description = "Demandas respondidas até o PRAZO_RESPOSTA")
        long demandasRespondidasNoPrazo,
        @Schema(description = "Demandas com prazo vencido e ainda sem resposta")
        long demandasVencidasSemResposta,
        @Schema(description = "respondidasNoPrazo / (respondidas + vencidasSemResposta) * 100; null se não houver base de cálculo")
        Double percentualRespondidasNoPrazo
) {
}
