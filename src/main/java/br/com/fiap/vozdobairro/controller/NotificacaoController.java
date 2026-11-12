package br.com.fiap.vozdobairro.controller;

import br.com.fiap.vozdobairro.dto.NotificacaoResponse;
import br.com.fiap.vozdobairro.dto.PaginaResponse;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import br.com.fiap.vozdobairro.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
@Tag(name = "Notificações")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    @Operation(summary = "Notificações do usuário logado (paginado, mais recentes primeiro)")
    public PaginaResponse<NotificacaoResponse> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 20, sort = "dtCriacao", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PaginaResponse.de(notificacaoService.listarDoUsuario(UsuarioLogado.de(jwt), pageable));
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marca a notificação como lida (somente o dono)")
    public NotificacaoResponse marcarComoLida(@PathVariable("id") Long id,
                                              @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return notificacaoService.marcarComoLida(id, UsuarioLogado.de(jwt));
    }
}
