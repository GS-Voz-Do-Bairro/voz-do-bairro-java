package br.com.fiap.vozdobairro.controller;

import br.com.fiap.vozdobairro.dto.AtualizarRespostaRequest;
import br.com.fiap.vozdobairro.dto.RascunhoParecerResponse;
import br.com.fiap.vozdobairro.dto.RespostaRequest;
import br.com.fiap.vozdobairro.dto.RespostaResponse;
import br.com.fiap.vozdobairro.security.UsuarioLogado;
import br.com.fiap.vozdobairro.service.AssistenteIaService;
import br.com.fiap.vozdobairro.service.RespostaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/propostas/{id}/resposta")
@Tag(name = "Respostas", description = "Resposta oficial da prefeitura (gestor) a uma demanda")
public class RespostaController {

    private final RespostaService respostaService;
    private final AssistenteIaService assistenteIaService;

    public RespostaController(RespostaService respostaService, AssistenteIaService assistenteIaService) {
        this.respostaService = respostaService;
        this.assistenteIaService = assistenteIaService;
    }

    @GetMapping
    @Operation(summary = "Resposta da proposta (qualquer usuário autenticado)")
    public RespostaResponse buscar(@PathVariable("id") Long id) {
        return respostaService.buscar(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Responde demanda (GESTOR). Só DEMANDA/ATRASADA. ACEITA exige prazoExecucao futuro -> EM_EXECUCAO; RECUSADA -> RECUSADA")
    public ResponseEntity<RespostaResponse> responder(@PathVariable("id") Long id,
                                                      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody RespostaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respostaService.responder(id, req, UsuarioLogado.de(jwt)));
    }

    @PutMapping
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Edita justificativa/prazo da resposta (GESTOR). A decisão não muda")
    public RespostaResponse atualizar(@PathVariable("id") Long id, @Valid @RequestBody AtualizarRespostaRequest req) {
        return respostaService.atualizar(id, req);
    }

    @PostMapping("/rascunho-ia")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Gera rascunho de parecer com IA (GESTOR). Não salva nada; o gestor revisa e envia pelo POST")
    public RascunhoParecerResponse rascunho(@PathVariable("id") Long id) {
        return assistenteIaService.rascunharParecer(id);
    }
}
