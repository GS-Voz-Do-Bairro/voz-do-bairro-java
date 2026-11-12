package br.com.fiap.vozdobairro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login")
public record LoginRequest(
        @Schema(example = "cidadao@vozdobairro.com")
        @NotBlank(message = "não pode ser vazio")
        @Email(message = "deve ser um e-mail válido")
        String email,

        @Schema(example = "Senha@123")
        @NotBlank(message = "não pode ser vazio")
        String senha
) {
}
