package br.com.fiap.vozdobairro.repository;

import br.com.fiap.vozdobairro.domain.Apoio;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Somente leitura (apoios pertencem a API .NET). */
public interface ApoioRepository extends Repository<Apoio, Long> {

    @Query("select distinct a.usuarioId from Apoio a where a.proposta.id = :propostaId")
    List<Long> buscarIdsApoiadores(@Param("propostaId") Long propostaId);
}
