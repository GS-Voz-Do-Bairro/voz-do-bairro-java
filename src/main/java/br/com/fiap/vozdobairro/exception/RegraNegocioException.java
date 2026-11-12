package br.com.fiap.vozdobairro.exception;

/** 422 - a requisicao e valida, mas viola uma regra do dominio (ex.: transicao de status invalida). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
