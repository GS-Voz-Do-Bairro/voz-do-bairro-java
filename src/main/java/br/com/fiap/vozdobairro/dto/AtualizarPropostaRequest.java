package br.com.fiap.vozdobairro.dto;

import br.com.fiap.vozdobairro.domain.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Edição de proposta (somente o autor, somente enquanto ABERTA)")
public record AtualizarPropostaRequest(
        @NotBlank(message = "não pode ser vazio")
        @Size(min = 5, max = 150, message = "deve ter entre 5 e 150 caracteres")
        String titulo,

        @NotBlank(message = "não pode ser vazio")
        @Size(min = 20, max = 2000, message = "deve ter entre 20 e 2000 caracteres")
        String descricao,

        @NotNull(message = "é obrigatório")
        Categoria categoria
) {
}
