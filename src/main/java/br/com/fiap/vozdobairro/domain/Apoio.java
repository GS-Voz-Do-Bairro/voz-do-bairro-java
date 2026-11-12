package br.com.fiap.vozdobairro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * VB_APOIO. Somente leitura na API Java (apoios pertencem a API .NET).
 * Mapeado apenas para descobrir os apoiadores que devem ser notificados.
 */
@Entity
@Immutable
@Table(name = "VB_APOIO")
public class Apoio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "PROPOSTA_ID", nullable = false)
    private Proposta proposta;

    @Column(name = "USUARIO_ID", nullable = false)
    private Long usuarioId;

    @Column(name = "DT_APOIO", nullable = false)
    private LocalDateTime dtApoio;

    protected Apoio() {
    }

    public Long getId() {
        return id;
    }

    public Proposta getProposta() {
        return proposta;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public LocalDateTime getDtApoio() {
        return dtApoio;
    }
}
