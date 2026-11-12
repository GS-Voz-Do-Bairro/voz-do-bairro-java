package br.com.fiap.vozdobairro.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Monta o ProblemDetail no formato do contrato (secao 6):
 * type=about:blank, title em portugues, status, detail, instance e, quando houver, "errors" por campo.
 * Usado pelo {@link GlobalExceptionHandler} e pelos handlers de seguranca (401/403).
 */
public final class ProblemDetailFactory {

    private ProblemDetailFactory() {
    }

    public static ProblemDetail criar(HttpStatusCode status, String detail, String instance) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detail);
        problema.setTitle(titulo(status.value()));
        if (instance != null) {
            problema.setInstance(URI.create(instance));
        }
        return problema;
    }

    public static ProblemDetail criar(HttpStatusCode status, String detail, String instance,
                                      Map<String, List<String>> errors) {
        ProblemDetail problema = criar(status, detail, instance);
        if (errors != null && !errors.isEmpty()) {
            problema.setProperty("errors", errors);
        }
        return problema;
    }

    public static String titulo(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida";
            case 401 -> "Não autenticado";
            case 403 -> "Acesso negado";
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método não permitido";
            case 406 -> "Formato de resposta não aceito";
            case 409 -> "Conflito";
            case 415 -> "Tipo de mídia não suportado";
            case 422 -> "Regra de negócio violada";
            case 503 -> "Serviço indisponível";
            default -> status >= 500 ? "Erro interno" : "Erro na requisição";
        };
    }
}
