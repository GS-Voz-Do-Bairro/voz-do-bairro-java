package br.com.fiap.vozdobairro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Atualização do próprio perfil (e-mail e papel não mudam por aqui)")
public record AtualizarUsuarioRequest(
        @Schema(example = "Maria Cidadã")
        @NotBlank(message = "não pode ser vazio")
        @Size(max = 120, message = "deve ter no máximo 120 caracteres")
        String nome,

        @Schema(example = "2", description = "Opcional para GESTOR; obrigatório para CIDADAO")
        @Positive(message = "deve ser positivo")
        Long bairroId
) {
}
