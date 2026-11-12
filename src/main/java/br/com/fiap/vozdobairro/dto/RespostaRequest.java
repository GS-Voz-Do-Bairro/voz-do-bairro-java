package br.com.fiap.vozdobairro.dto;

import br.com.fiap.vozdobairro.domain.Decisao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Resposta do gestor a uma demanda")
public record RespostaRequest(
        @Schema(example = "ACEITA")
        @NotNull(message = "é obrigatório")
        Decisao decisao,

        @Schema(example = "A troca das lâmpadas foi incluída no cronograma da subprefeitura.")
        @NotBlank(message = "não pode ser vazio")
        @Size(max = 2000, message = "deve ter no máximo 2000 caracteres")
        String justificativa,

        @Schema(example = "2026-12-20", description = "Obrigatório e futuro quando decisao = ACEITA")
        LocalDate prazoExecucao
) {
}
