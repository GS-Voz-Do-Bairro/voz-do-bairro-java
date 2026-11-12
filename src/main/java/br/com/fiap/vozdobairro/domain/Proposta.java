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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * VB_PROPOSTA.
 *
 * <p>Esta tabela e escrita por duas APIs. Para nao sobrescrever o que a API .NET grava:
 * <ul>
 *   <li>{@code @DynamicUpdate}: o UPDATE gerado contem so as colunas alteradas;</li>
 *   <li>TOTAL_APOIOS, DT_VIROU_DEMANDA e PRAZO_RESPOSTA sao somente leitura no Java
 *       (insertable/updatable = false). Quem grava e a API .NET.</li>
 * </ul>
 * Datas sao gravadas em UTC (LocalDateTime com relogio UTC).
 */
@Entity
@DynamicUpdate
@Table(name = "VB_PROPOSTA")
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TITULO", nullable = false, length = 150)
    private String titulo;

    @Column(name = "DESCRICAO", nullable = false, length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORIA", nullable = false, length = 30)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private StatusProposta status;

    @Column(name = "RESUMO_IA", length = 1000)
    private String resumoIa;

    @Column(name = "META_APOIOS", nullable = false, updatable = false)
    private Integer metaApoios;

    /** Mantido pela API .NET (apoios). Valor 0 vem do DEFAULT da coluna. */
    @ColumnDefault("0")
    @Column(name = "TOTAL_APOIOS", nullable = false, insertable = false, updatable = false)
    private int totalApoios;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AUTOR_ID", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BAIRRO_ID", nullable = false)
    private Bairro bairro;

    @Column(name = "DT_CRIACAO", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    /** Gravado pela API .NET quando a meta e atingida. */
    @Column(name = "DT_VIROU_DEMANDA", insertable = false, updatable = false)
    private LocalDateTime dtVirouDemanda;

    /** Gravado pela API .NET (agora + 15 dias) quando a meta e atingida. */
    @Column(name = "PRAZO_RESPOSTA", insertable = false, updatable = false)
    private LocalDateTime prazoResposta;

    protected Proposta() {
    }

    public Proposta(String titulo, String descricao, Categoria categoria, Usuario autor, Bairro bairro) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.autor = autor;
        this.bairro = bairro;
        this.status = StatusProposta.ABERTA;
        this.metaApoios = bairro.getMetaApoiosPadrao();
        this.totalApoios = 0;
    }

    @PrePersist
    void aoInserir() {
        if (dtCriacao == null) {
            dtCriacao = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public boolean isAutor(Long usuarioId) {
        return autor != null && Objects.equals(autor.getId(), usuarioId);
    }

    public void editar(String titulo, String descricao, Categoria categoria) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public StatusProposta getStatus() {
        return status;
    }

    public void setStatus(StatusProposta status) {
        this.status = status;
    }

    public String getResumoIa() {
        return resumoIa;
    }

    public void setResumoIa(String resumoIa) {
        this.resumoIa = resumoIa;
    }

    public Integer getMetaApoios() {
        return metaApoios;
    }

    public int getTotalApoios() {
        return totalApoios;
    }

    /** Apenas para testes/montagem em memoria: a coluna nao e gravada pelo Java. */
    public void setTotalApoios(int totalApoios) {
        this.totalApoios = totalApoios;
    }

    public Usuario getAutor() {
        return autor;
    }

    public Bairro getBairro() {
        return bairro;
    }

    public LocalDateTime getDtCriacao() {
        return dtCriacao;
    }

    public LocalDateTime getDtVirouDemanda() {
        return dtVirouDemanda;
    }

    public LocalDateTime getPrazoResposta() {
        return prazoResposta;
    }

    /** Apenas para testes/montagem em memoria: a coluna nao e gravada pelo Java. */
    public void setPrazoResposta(LocalDateTime prazoResposta) {
        this.prazoResposta = prazoResposta;
    }
}
