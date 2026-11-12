package br.com.fiap.vozdobairro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Cadastro de cidadão")
public record RegisterRequest(
        @Schema(example = "Ana Moradora")
        @NotBlank(message = "não pode ser vazio")
        @Size(max = 120, message = "deve ter no máximo 120 caracteres")
        String nome,

        @Schema(example = "ana@exemplo.com")
        @NotBlank(message = "não pode ser vazio")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String email,

        @Schema(example = "Senha@123")
        @NotBlank(message = "não pode ser vazio")
        @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
        String senha,

        @Schema(example = "2")
        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser positivo")
        Long bairroId
) {
}
