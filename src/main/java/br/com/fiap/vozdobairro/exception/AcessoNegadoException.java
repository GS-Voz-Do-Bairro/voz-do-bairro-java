package br.com.fiap.vozdobairro.exception;

/** 403 - usuario autenticado, mas sem permissao sobre o recurso (ex.: nao e o autor). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
