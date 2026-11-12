package br.com.fiap.vozdobairro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * VB_BAIRRO. Somente leitura na API Java (o CRUD de bairros pertence a API .NET).
 */
@Entity
@Immutable
@Table(name = "VB_BAIRRO")
public class Bairro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "SUBPREFEITURA", nullable = false, length = 100)
    private String subprefeitura;

    @Column(name = "CIDADE", nullable = false, length = 100)
    private String cidade;

    @Column(name = "META_APOIOS_PADRAO", nullable = false)
    private Integer metaApoiosPadrao;

    protected Bairro() {
    }

    public Bairro(Long id, String nome, String subprefeitura, String cidade, Integer metaApoiosPadrao) {
        this.id = id;
        this.nome = nome;
        this.subprefeitura = subprefeitura;
        this.cidade = cidade;
        this.metaApoiosPadrao = metaApoiosPadrao;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSubprefeitura() {
        return subprefeitura;
    }

    public String getCidade() {
        return cidade;
    }

    public Integer getMetaApoiosPadrao() {
        return metaApoiosPadrao;
    }
}
