package br.com.fiap.vozdobairro.repository;

import br.com.fiap.vozdobairro.domain.StatusProposta;

/** Projecao usada no placar (constructor expression em JPQL). */
public record ContagemPorStatus(StatusProposta status, Long total) {
}
