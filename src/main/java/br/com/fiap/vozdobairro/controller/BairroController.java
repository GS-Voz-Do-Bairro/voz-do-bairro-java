package br.com.fiap.vozdobairro.controller;

import br.com.fiap.vozdobairro.dto.BairroResponse;
import br.com.fiap.vozdobairro.dto.PlacarResponse;
import br.com.fiap.vozdobairro.service.BairroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Leitura publica de bairros e placar. O CRUD de bairros e da API .NET. */
@RestController
@RequestMapping("/api/bairros")
@Tag(name = "Bairros", description = "Lista e placar público (sem token)")
@SecurityRequirements
public class BairroController {

    private final BairroService bairroService;

    public BairroController(BairroService bairroService) {
        this.bairroService = bairroService;
    }

    @GetMapping
    @Operation(summary = "Lista os bairros (público)")
    public List<BairroResponse> listar() {
        return bairroService.listar();
    }

    @GetMapping("/{id}/placar")
    @Operation(summary = "Placar do bairro: propostas por status e % de demandas respondidas no prazo (público)")
    public PlacarResponse placar(@PathVariable("id") Long id) {
        return bairroService.placar(id);
    }
}
