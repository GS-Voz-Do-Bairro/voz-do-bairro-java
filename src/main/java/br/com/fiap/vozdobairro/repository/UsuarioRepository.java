package br.com.fiap.vozdobairro.repository;

import br.com.fiap.vozdobairro.domain.Papel;
import br.com.fiap.vozdobairro.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u.id from Usuario u where u.papel = :papel")
    List<Long> findIdsByPapel(@Param("papel") Papel papel);

    @Query(value = "select u from Usuario u left join fetch u.bairro",
            countQuery = "select count(u) from Usuario u")
    Page<Usuario> listarComBairro(Pageable pageable);
}
