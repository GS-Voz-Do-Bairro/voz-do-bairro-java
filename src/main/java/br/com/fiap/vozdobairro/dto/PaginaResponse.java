package br.com.fiap.vozdobairro.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope estavel de paginacao para o app Mobile. Evita serializar PageImpl diretamente
 * (formato nao garantido pelo Spring Data).
 */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {
    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
