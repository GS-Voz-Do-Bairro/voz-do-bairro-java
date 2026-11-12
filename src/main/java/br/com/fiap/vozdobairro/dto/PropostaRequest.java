package br.com.fiap.vozdobairro.dto;

import br.com.fiap.vozdobairro.domain.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Criação de proposta")
public record PropostaRequest(
        @Schema(example = "Iluminação na Rua Agnaldo Macedo")
        @NotBlank(message = "não pode ser vazio")
        @Size(min = 5, max = 150, message = "deve ter entre 5 e 150 caracteres")
        String titulo,

        @Schema(example = "A rua fica totalmente escura à noite entre os números 100 e 400.")
        @NotBlank(message = "não pode ser vazio")
        @Size(min = 20, max = 2000, message = "deve ter entre 20 e 2000 caracteres")
        String descricao,

        @Schema(example = "SEGURANCA")
        @NotNull(message = "é obrigatório")
        Categoria categoria,

        @Schema(example = "2")
        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser positivo")
        Long bairroId
) {
}
