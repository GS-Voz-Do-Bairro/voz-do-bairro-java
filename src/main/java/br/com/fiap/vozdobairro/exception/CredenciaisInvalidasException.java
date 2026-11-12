package br.com.fiap.vozdobairro.exception;

/** 401 - e-mail ou senha invalidos no login. */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}
