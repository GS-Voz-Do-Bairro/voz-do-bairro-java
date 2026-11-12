package br.com.fiap.vozdobairro.exception;

/** 404 - recurso inexistente. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
