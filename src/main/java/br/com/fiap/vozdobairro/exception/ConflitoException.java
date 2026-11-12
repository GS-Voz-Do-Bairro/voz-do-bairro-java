package br.com.fiap.vozdobairro.exception;

/** 409 - conflito com o estado atual (ex.: e-mail ja cadastrado, proposta ja respondida). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
