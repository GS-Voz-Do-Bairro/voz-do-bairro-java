package br.com.fiap.vozdobairro.repository;

import br.com.fiap.vozdobairro.domain.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByUsuarioId(Long usuarioId, Pageable pageable);

    /** Idempotencia: o tipo da notificacao e o prefixo fixo da mensagem (ver TipoNotificacao). */
    boolean existsByPropostaIdAndMensagemStartingWith(Long propostaId, String prefixo);
}
