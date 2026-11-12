package br.com.fiap.vozdobairro.repository;

import br.com.fiap.vozdobairro.domain.Comentario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Somente leitura (comentarios pertencem a API .NET). */
public interface ComentarioRepository extends Repository<Comentario, Long> {

    long countByPropostaId(Long propostaId);

    /** Textos dos comentarios mais antigos primeiro; o Pageable limita quantos vao para o prompt do LLM. */
    @Query("select c.texto from Comentario c where c.proposta.id = :propostaId order by c.dtCriacao asc")
    List<String> buscarTextos(@Param("propostaId") Long propostaId, Pageable limite);
}
