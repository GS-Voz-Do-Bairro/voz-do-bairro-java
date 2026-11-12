package br.com.fiap.vozdobairro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** VB_RESPOSTA: parecer do gestor sobre uma proposta que virou demanda (1 por proposta). */
@Entity
@Table(name = "VB_RESPOSTA")
public class Resposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    // Mapeado como ManyToOne + unique (UK_VB_RESPOSTA_PROPOSTA): mesma semantica de 1:1 no lado dono,
    // e @OnDelete em ManyToOne e suportado de forma estavel no Hibernate 6.x.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "PROPOSTA_ID", nullable = false, unique = true)
    private Proposta proposta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "GESTOR_ID", nullable = false)
    private Usuario gestor;

    @Enumerated(EnumType.STRING)
    @Column(name = "DECISAO", nullable = false, length = 20, updatable = false)
    private Decisao decisao;

    @Column(name = "JUSTIFICATIVA", nullable = false, length = 2000)
    private String justificativa;

    @Column(name = "PRAZO_EXECUCAO")
    private LocalDate prazoExecucao;

    @Column(name = "DT_RESPOSTA", nullable = false, updatable = false)
    private LocalDateTime dtResposta;

    protected Resposta() {
    }

    public Resposta(Proposta proposta, Usuario gestor, Decisao decisao, String justificativa,
                    LocalDate prazoExecucao, LocalDateTime dtResposta) {
        this.proposta = proposta;
        this.gestor = gestor;
        this.decisao = decisao;
        this.justificativa = justificativa;
        this.prazoExecucao = prazoExecucao;
        this.dtResposta = dtResposta;
    }

    public void atualizar(String justificativa, LocalDate prazoExecucao) {
        this.justificativa = justificativa;
        this.prazoExecucao = prazoExecucao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proposta getProposta() {
        return proposta;
    }

    public Usuario getGestor() {
        return gestor;
    }

    public Decisao getDecisao() {
        return decisao;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public LocalDate getPrazoExecucao() {
        return prazoExecucao;
    }

    public LocalDateTime getDtResposta() {
        return dtResposta;
    }
}
