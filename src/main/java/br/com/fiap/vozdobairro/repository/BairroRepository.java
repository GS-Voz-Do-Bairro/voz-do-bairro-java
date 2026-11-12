package br.com.fiap.vozdobairro.repository;

import br.com.fiap.vozdobairro.domain.Bairro;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Somente leitura: estende Repository (e nao JpaRepository) para nao expor save/delete. */
public interface BairroRepository extends Repository<Bairro, Long> {

    Optional<Bairro> findById(Long id);

    List<Bairro> findAll(Sort sort);

    boolean existsById(Long id);
}
