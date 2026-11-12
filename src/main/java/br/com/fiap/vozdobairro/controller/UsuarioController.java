package br.com.fiap.vozdobairro.controller;

import br.com.fiap.vozdobairro.dto.AtualizarUsuarioRequest;
import br.com.fiap.vozdobairro.dto.PaginaResponse;
import br.com.fiap.vozdobairro.dto.UsuarioResponse;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import br.com.fiap.vozdobairro.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado")
    public UsuarioResponse me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.buscarLogado(UsuarioLogado.de(jwt));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza nome e bairro do usuário logado")
    public UsuarioResponse atualizarMe(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody AtualizarUsuarioRequest req) {
        return usuarioService.atualizarLogado(UsuarioLogado.de(jwt), req);
    }

    @GetMapping
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Lista usuários (GESTOR, paginado)")
    public PaginaResponse<UsuarioResponse> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return PaginaResponse.de(usuarioService.listar(pageable));
    }
}
