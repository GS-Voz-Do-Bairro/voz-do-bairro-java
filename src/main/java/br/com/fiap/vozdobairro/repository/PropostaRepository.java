package br.com.fiap.vozdobairro.repository;

import br.com.fiap.vozdobairro.domain.Categoria;
import br.com.fiap.vozdobairro.domain.Proposta;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    @Query(value = """
            select p from Proposta p join fetch p.autor join fetch p.bairro
            where (:bairroId is null or p.bairro.id = :bairroId)
              and (:status is null or p.status = :status)
              and (:categoria is null or p.categoria = :categoria)
            """,
            countQuery = """
            select count(p) from Proposta p
            where (:bairroId is null or p.bairro.id = :bairroId)
              and (:status is null or p.status = :status)
              and (:categoria is null or p.categoria = :categoria)
            """)
    Page<Proposta> buscar(@Param("bairroId") Long bairroId,
                          @Param("status") StatusProposta status,
                          @Param("categoria") Categoria categoria,
                          Pageable pageable);

    @Query(value = "select p from Proposta p join fetch p.autor join fetch p.bairro where p.autor.id = :autorId",
            countQuery = "select count(p) from Proposta p where p.autor.id = :autorId")
    Page<Proposta> buscarPorAutor(@Param("autorId") Long autorId, Pageable pageable);

    /** Carrega autor e bairro juntos: seguro para usar fora de transacao (ex.: chamada ao LLM). */
    @Query("select p from Proposta p join fetch p.autor join fetch p.bairro where p.id = :id")
    Optional<Proposta> buscarDetalhada(@Param("id") Long id);

    @Query("""
            select new br.com.fiap.vozdobairro.repository.ContagemPorStatus(p.status, count(p))
            from Proposta p where p.bairro.id = :bairroId group by p.status
            """)
    List<ContagemPorStatus> contarPorStatus(@Param("bairroId") Long bairroId);

    @Query("""
            select count(p) from Proposta p
            where p.bairro.id = :bairroId
              and p.prazoResposta is not null
              and p.prazoResposta < :agora
              and not exists (select r.id from Resposta r where r.proposta = p)
            """)
    long contarVencidasSemResposta(@Param("bairroId") Long bairroId, @Param("agora") LocalDateTime agora);

    /** Usado pelo agendador: DEMANDA com prazo vencido e sem resposta. */
    @Query("""
            select p from Proposta p
            where p.status = :status
              and p.prazoResposta < :agora
              and not exists (select r.id from Resposta r where r.proposta = p)
            """)
    List<Proposta> buscarComPrazoVencido(@Param("status") StatusProposta status,
                                         @Param("agora") LocalDateTime agora);

    /**
     * Transicao DEMANDA -> ATRASADA de forma atomica e idempotente: so altera se ainda estiver
     * no status esperado, com prazo vencido e sem resposta. Retorna 0 se nada mudou.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Proposta p set p.status = :novoStatus
            where p.id = :id
              and p.status = :statusAtual
              and p.prazoResposta < :agora
              and not exists (select r.id from Resposta r where r.proposta.id = :id)
            """)
    int marcarComoAtrasada(@Param("id") Long id,
                           @Param("statusAtual") StatusProposta statusAtual,
                           @Param("novoStatus") StatusProposta novoStatus,
                           @Param("agora") LocalDateTime agora);

    /** Atualiza so a coluna RESUMO_IA (nao toca em colunas mantidas pela API .NET). */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Proposta p set p.resumoIa = :resumo where p.id = :id")
    int atualizarResumoIa(@Param("id") Long id, @Param("resumo") String resumo);
}
