package br.com.fiap.vozdobairro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** VB_NOTIFICACAO. LIDA e NUMBER(1) no Oracle: mapeado com {@link BooleanNumeroConverter}. */
@Entity
@Table(name = "VB_NOTIFICACAO")
public class Notificacao {

    public static final int TAMANHO_MAXIMO_MENSAGEM = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "PROPOSTA_ID")
    private Proposta proposta;

    @Column(name = "MENSAGEM", nullable = false, length = TAMANHO_MAXIMO_MENSAGEM)
    private String mensagem;

    @Convert(converter = BooleanNumeroConverter.class)
    @Column(name = "LIDA", nullable = false)
    private Boolean lida = Boolean.FALSE;

    @Column(name = "DT_CRIACAO", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    protected Notificacao() {
    }

    public Notificacao(Usuario usuario, Proposta proposta, String mensagem) {
        this.usuario = usuario;
        this.proposta = proposta;
        this.mensagem = mensagem;
        this.lida = Boolean.FALSE;
    }

    @PrePersist
    void aoInserir() {
        if (dtCriacao == null) {
            dtCriacao = LocalDateTime.now(ZoneOffset.UTC);
        }
        if (lida == null) {
            lida = Boolean.FALSE;
        }
    }

    public void marcarComoLida() {
        this.lida = Boolean.TRUE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Proposta getProposta() {
        return proposta;
    }

    public String getMensagem() {
        return mensagem;
    }

    public Boolean getLida() {
        return lida;
    }

    public LocalDateTime getDtCriacao() {
        return dtCriacao;
    }
}
