package br.com.fiap.vozdobairro.controller;

import br.com.fiap.vozdobairro.domain.Categoria;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.dto.AtualizarPropostaRequest;
import br.com.fiap.vozdobairro.dto.PaginaResponse;
import br.com.fiap.vozdobairro.dto.PropostaDetalheResponse;
import br.com.fiap.vozdobairro.dto.PropostaRequest;
import br.com.fiap.vozdobairro.dto.PropostaResponse;
import br.com.fiap.vozdobairro.dto.ResumoIaResponse;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import br.com.fiap.vozdobairro.service.AssistenteIaService;
import br.com.fiap.vozdobairro.service.PropostaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/propostas")
@Tag(name = "Propostas")
public class PropostaController {

    private final PropostaService propostaService;
    private final AssistenteIaService assistenteIaService;

    public PropostaController(PropostaService propostaService, AssistenteIaService assistenteIaService) {
        this.propostaService = propostaService;
        this.assistenteIaService = assistenteIaService;
    }

    @GetMapping
    @Operation(summary = "Lista propostas (paginado; filtros opcionais bairroId, status, categoria)")
    public PaginaResponse<PropostaResponse> listar(
            @RequestParam(name = "bairroId", required = false) Long bairroId,
            @RequestParam(name = "status", required = false) StatusProposta status,
            @RequestParam(name = "categoria", required = false) Categoria categoria,
            @ParameterObject @PageableDefault(size = 10, sort = "dtCriacao", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PaginaResponse.de(propostaService.listar(bairroId, status, categoria, pageable));
    }

    @GetMapping("/minhas")
    @Operation(summary = "Propostas criadas pelo usuário logado (paginado)")
    public PaginaResponse<PropostaResponse> minhas(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 10, sort = "dtCriacao", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PaginaResponse.de(propostaService.listarDoAutor(UsuarioLogado.de(jwt), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da proposta: resumo IA, resposta do gestor (se houver) e total de comentários")
    public PropostaDetalheResponse detalhar(@PathVariable("id") Long id) {
        return propostaService.detalhar(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('CIDADAO')")
    @Operation(summary = "Cria proposta (CIDADAO). Status ABERTA; meta copiada do bairro; resumo IA gerado de forma assíncrona")
    public ResponseEntity<PropostaResponse> criar(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody PropostaRequest req) {
        PropostaResponse criada = propostaService.criar(req, UsuarioLogado.de(jwt));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita proposta (somente o autor e somente com status ABERTA)")
    public PropostaResponse atualizar(@PathVariable("id") Long id,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody AtualizarPropostaRequest req) {
        return propostaService.atualizar(id, req, UsuarioLogado.de(jwt));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui proposta (autor: só ABERTA e sem apoios; GESTOR: qualquer uma)")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id,
                                        @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        propostaService.excluir(id, UsuarioLogado.de(jwt));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Marca proposta EM_EXECUCAO como CONCLUIDA (GESTOR)")
    public PropostaResponse concluir(@PathVariable("id") Long id) {
        return propostaService.concluir(id);
    }

    @PostMapping("/{id}/resumo-ia")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Regenera o resumo neutro da proposta com IA, de forma síncrona (GESTOR)")
    public ResumoIaResponse regenerarResumo(@PathVariable("id") Long id) {
        return assistenteIaService.regenerarResumo(id);
    }
}
