package br.com.fiap.vozdobairro.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Tratamento global de erros. Todas as respostas de erro seguem o Problem Details (RFC 7807)
 * no formato do contrato: type, title (pt-BR), status, detail, instance e "errors" por campo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> tratarValidacao(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            adicionar(errors, erro.getField(), erro.getDefaultMessage());
        }
        for (ObjectError erro : ex.getBindingResult().getGlobalErrors()) {
            adicionar(errors, erro.getObjectName(), erro.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> tratarConstraint(ConstraintViolationException ex,
                                                          HttpServletRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacao : ex.getConstraintViolations()) {
            String caminho = violacao.getPropertyPath() == null ? "" : violacao.getPropertyPath().toString();
            String campo = caminho.contains(".") ? caminho.substring(caminho.lastIndexOf('.') + 1) : caminho;
            adicionar(errors, campo, violacao.getMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais parâmetros são inválidos.", request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> tratarCorpoIlegivel(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        if (ex.getCause() instanceof MismatchedInputException mie) {
            String campo = caminhoJson(mie);
            if (!campo.isBlank()) {
                adicionar(errors, campo, mensagemFormatoInvalido(mie));
            }
        }
        return responder(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente, mal formatado ou com valores inválidos.", request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> tratarTipoParametro(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        String mensagem = "valor inválido";
        Class<?> tipo = ex.getRequiredType();
        if (tipo != null && tipo.isEnum()) {
            mensagem = "valor inválido; aceitos: " + valoresEnum(tipo);
        }
        adicionar(errors, ex.getName(), mensagem);
        return responder(HttpStatus.BAD_REQUEST, "Parâmetro com valor inválido.", request, errors);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> tratarOrdenacao(PropertyReferenceException ex, HttpServletRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        adicionar(errors, "sort", "campo de ordenação inexistente: " + ex.getPropertyName());
        return responder(HttpStatus.BAD_REQUEST, "Parâmetro de ordenação inválido.", request, errors);
    }

    @ExceptionHandler(CampoInvalidoException.class)
    public ResponseEntity<ProblemDetail> tratarCampoInvalido(CampoInvalidoException ex, HttpServletRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        adicionar(errors, ex.getCampo(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", request, errors);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> tratarNaoEncontrado(RecursoNaoEncontradoException ex,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ProblemDetail> tratarRegra(RegraNegocioException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ProblemDetail> tratarConflito(ConflitoException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ProblemDetail> tratarAcessoNegado(AcessoNegadoException ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ProblemDetail> tratarCredenciais(CredenciaisInvalidasException ex,
                                                           HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    /** Lancada pelo @PreAuthorize quando o papel nao confere (precisa ser tratada aqui, senao viraria 500). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> tratarAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, "Seu papel não tem permissão para esta operação.", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> tratarIntegridade(DataIntegrityViolationException ex,
                                                           HttpServletRequest request) {
        log.warn("Violacao de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados.", request, null);
    }

    /**
     * Demais excecoes. As excecoes padrao do Spring MVC (405, 415, 404 de rota inexistente,
     * parametro obrigatorio ausente...) implementam {@link ErrorResponse} e mantem o status correto.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarGenerico(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String detail = errorResponse.getBody().getDetail();
            if (status.is5xxServerError()) {
                log.error("Erro em {}", request.getRequestURI(), ex);
            }
            return responder(status, detail != null ? detail : ex.getMessage(), request, null);
        }
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.", request, null);
    }

    private ResponseEntity<ProblemDetail> responder(HttpStatusCode status, String detail, HttpServletRequest request,
                                                    Map<String, List<String>> errors) {
        ProblemDetail problema = ProblemDetailFactory.criar(status, detail, request.getRequestURI(), errors);
        return ResponseEntity.status(status).body(problema);
    }

    private static void adicionar(Map<String, List<String>> errors, String campo, String mensagem) {
        errors.computeIfAbsent(campo, k -> new ArrayList<>()).add(mensagem);
    }

    private static String caminhoJson(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    private static String mensagemFormatoInvalido(MismatchedInputException ex) {
        if (ex instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            return "valor inválido; aceitos: " + valoresEnum(ife.getTargetType());
        }
        return "valor inválido ou em formato incorreto";
    }

    private static String valoresEnum(Class<?> tipoEnum) {
        return Arrays.stream(tipoEnum.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
