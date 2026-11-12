package br.com.fiap.vozdobairro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Edição da resposta (a decisão não muda)")
public record AtualizarRespostaRequest(
        @NotBlank(message = "não pode ser vazio")
        @Size(max = 2000, message = "deve ter no máximo 2000 caracteres")
        String justificativa,

        @Schema(example = "2027-01-15", description = "Obrigatório e futuro quando a decisão é ACEITA; ignorado quando RECUSADA")
        LocalDate prazoExecucao
) {
}
