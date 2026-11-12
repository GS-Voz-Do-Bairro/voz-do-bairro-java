package br.com.fiap.vozdobairro.repository;

import br.com.fiap.vozdobairro.domain.Resposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RespostaRepository extends JpaRepository<Resposta, Long> {

    @Query("select r from Resposta r join fetch r.gestor join fetch r.proposta where r.proposta.id = :propostaId")
    Optional<Resposta> buscarPorProposta(@Param("propostaId") Long propostaId);

    boolean existsByPropostaId(Long propostaId);

    @Query("""
            select count(r) from Resposta r
            where r.proposta.bairro.id = :bairroId and r.proposta.prazoResposta is not null
            """)
    long contarRespondidasDoBairro(@Param("bairroId") Long bairroId);

    @Query("""
            select count(r) from Resposta r
            where r.proposta.bairro.id = :bairroId
              and r.proposta.prazoResposta is not null
              and r.dtResposta <= r.proposta.prazoResposta
            """)
    long contarRespondidasNoPrazoDoBairro(@Param("bairroId") Long bairroId);
}
