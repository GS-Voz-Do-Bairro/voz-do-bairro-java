package br.com.fiap.vozdobairro.exception;

/**
 * 400 - erro de validacao de um campo que so pode ser verificado no service
 * (ex.: bairroId inexistente). Vira um item em "errors" no Problem Details.
 */
public class CampoInvalidoException extends RuntimeException {

    private final String campo;

    public CampoInvalidoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
