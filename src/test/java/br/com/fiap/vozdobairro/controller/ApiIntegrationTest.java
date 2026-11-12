package br.com.fiap.vozdobairro.controller;

import br.com.fiap.vozdobairro.config.RabbitConfig;
import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.Papel;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.repository.BairroRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import br.com.fiap.vozdobairro.security.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de API (MockMvc + H2 perfil test): seguranca 401/403 por papel, validacao 400 e fluxo feliz
 * completo (cria proposta -> evento proposta.criada -> vira DEMANDA -> gestor aceita -> conclui).
 * Os tokens sao emitidos pelo TokenService real e validados pelo JwtDecoder real (claim "role").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String PROPOSTA_VALIDA = """
            {"titulo":"Iluminação na Rua Agnaldo Macedo",
             "descricao":"A rua fica totalmente escura à noite entre os números 100 e 400.",
             "categoria":"SEGURANCA","bairroId":%d}
            """;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private BairroRepository bairroRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private Long bairroId;
    private String tokenCidadao;
    private String tokenVizinho;
    private String tokenGestor;

    @BeforeEach
    void prepararDados() {
        jdbc.update("DELETE FROM VB_NOTIFICACAO");
        jdbc.update("DELETE FROM VB_RESPOSTA");
        jdbc.update("DELETE FROM VB_COMENTARIO");
        jdbc.update("DELETE FROM VB_APOIO");
        jdbc.update("DELETE FROM VB_PROPOSTA");
        jdbc.update("DELETE FROM VB_USUARIO");
        jdbc.update("DELETE FROM VB_BAIRRO");

        jdbc.update("INSERT INTO VB_BAIRRO (NOME, SUBPREFEITURA, CIDADE, META_APOIOS_PADRAO) VALUES (?, ?, ?, ?)",
                "Capão Redondo", "Campo Limpo", "São Paulo", 30);
        bairroId = jdbc.queryForObject("SELECT ID FROM VB_BAIRRO WHERE NOME = ?", Long.class, "Capão Redondo");
        Bairro bairro = bairroRepository.findById(bairroId).orElseThrow();

        String hash = passwordEncoder.encode("Senha@123");
        Usuario cidadao = usuarioRepository.save(
                new Usuario("Maria Cidadã", "cidadao@vozdobairro.com", hash, Papel.CIDADAO, bairro));
        Usuario vizinho = usuarioRepository.save(
                new Usuario("João Vizinho", "vizinho@vozdobairro.com", hash, Papel.CIDADAO, bairro));
        Usuario gestor = usuarioRepository.save(
                new Usuario("Carla Gestora", "gestor@vozdobairro.com", hash, Papel.GESTOR, null));

        tokenCidadao = tokenService.gerar(cidadao).token();
        tokenVizinho = tokenService.gerar(vizinho).token();
        tokenGestor = tokenService.gerar(gestor).token();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ------------------------------------------------------------ publicos

    @Test
    void bairrosEPlacarSaoPublicos() throws Exception {
        mvc.perform(get("/api/bairros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Capão Redondo"))
                .andExpect(jsonPath("$[0].metaApoiosPadrao").value(30));

        mvc.perform(get("/api/bairros/{id}/placar", bairroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porStatus.ABERTA").value(0))
                .andExpect(jsonPath("$.totalPropostas").value(0));
    }

    // ------------------------------------------------------------ 401 / 403

    @Test
    void semTokenRetorna401EmProblemDetails() throws Exception {
        mvc.perform(get("/api/propostas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.instance").value("/api/propostas"));
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mvc.perform(get("/api/propostas").header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.xyz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gestorNaoPodeCriarProposta() throws Exception {
        mvc.perform(post("/api/propostas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROPOSTA_VALIDA.formatted(bairroId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    void somenteGestorListaUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));
    }

    @Test
    void cidadaoNaoPodeResponderNemGerarRascunhoIa() throws Exception {
        mvc.perform(post("/api/propostas/{id}/resposta/rascunho-ia", 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ 400

    @Test
    void validacaoDoCorpoRetorna400ComErrosPorCampo() throws Exception {
        mvc.perform(post("/api/propostas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descricao\":\"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.errors.titulo").isArray())
                .andExpect(jsonPath("$.errors.descricao").isArray())
                .andExpect(jsonPath("$.errors.categoria").isArray())
                .andExpect(jsonPath("$.errors.bairroId").isArray());
    }

    @Test
    void categoriaInexistenteRetorna400NoCampo() throws Exception {
        mvc.perform(post("/api/propostas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Titulo valido\",\"descricao\":\"Descrição suficientemente longa.\","
                                + "\"categoria\":\"FOGUETES\",\"bairroId\":" + bairroId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.categoria").isArray());
    }

    // ------------------------------------------------------------ auth

    @Test
    void registroELoginDevolvemTokenNoFormatoDoContrato() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Moradora\",\"email\":\"Ana@Exemplo.com\",\"senha\":\"Senha@123\","
                                + "\"bairroId\":" + bairroId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiraEm").isString())
                .andExpect(jsonPath("$.usuario.papel").value("CIDADAO"))
                .andExpect(jsonPath("$.usuario.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.usuario.bairroId").value(bairroId.intValue()));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outra\",\"email\":\"ana@exemplo.com\",\"senha\":\"Senha@123\","
                                + "\"bairroId\":" + bairroId + "}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gestor@vozdobairro.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gestor@vozdobairro.com\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.papel").value("GESTOR"))
                .andReturn();

        String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("gestor@vozdobairro.com"));
    }

    // ------------------------------------------------------------ fluxo feliz

    @Test
    void fluxoCompletoDaProposta() throws Exception {
        // 1. cidadao cria proposta
        MvcResult criada = mvc.perform(post("/api/propostas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROPOSTA_VALIDA.formatted(bairroId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.metaApoios").value(30))
                .andExpect(jsonPath("$.totalApoios").value(0))
                .andReturn();
        JsonNode body = objectMapper.readTree(criada.getResponse().getContentAsString());
        long propostaId = body.get("id").asLong();

        // 2. evento proposta.criada publicado no RabbitMQ apos o commit (assincrono)
        verify(rabbitTemplate, timeout(5000)).send(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.RK_PROPOSTA_CRIADA),
                any(Message.class));

        // 3. outro cidadao nao edita; autor edita enquanto ABERTA
        String edicao = """
                {"titulo":"Iluminação na Rua Agnaldo Macedo (trecho 100-400)",
                 "descricao":"A rua fica totalmente escura à noite entre os números 100 e 400.",
                 "categoria":"SEGURANCA"}
                """;
        mvc.perform(put("/api/propostas/{id}", propostaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVizinho))
                        .contentType(MediaType.APPLICATION_JSON).content(edicao))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/propostas/{id}", propostaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao))
                        .contentType(MediaType.APPLICATION_JSON).content(edicao))
                .andExpect(status().isOk());

        // 4. gestor nao responde proposta ABERTA (422)
        String aceite = "{\"decisao\":\"ACEITA\",\"justificativa\":\"Incluída no cronograma da subprefeitura.\","
                + "\"prazoExecucao\":\"" + LocalDate.now(ZoneOffset.UTC).plusDays(30) + "\"}";
        mvc.perform(post("/api/propostas/{id}/resposta", propostaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON).content(aceite))
                .andExpect(status().isUnprocessableEntity());

        // 5. simula a API .NET: meta atingida -> DEMANDA com prazo de resposta
        jdbc.update("UPDATE VB_PROPOSTA SET STATUS = 'DEMANDA', TOTAL_APOIOS = 30, DT_VIROU_DEMANDA = ?, "
                        + "PRAZO_RESPOSTA = ? WHERE ID = ?",
                Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)),
                Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).plusDays(15)),
                propostaId);

        // 6. autor nao edita mais
        mvc.perform(put("/api/propostas/{id}", propostaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao))
                        .contentType(MediaType.APPLICATION_JSON).content(edicao))
                .andExpect(status().isUnprocessableEntity());

        // 7. rascunho IA (fallback, IA desligada no perfil test) nao quebra
        mvc.perform(post("/api/propostas/{id}/resposta/rascunho-ia", propostaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geradoPorIa").value(false))
                .andExpect(jsonPath("$.justificativa").isString());

        // 8. gestor aceita -> EM_EXECUCAO
        mvc.perform(post("/api/propostas/{id}/resposta", propostaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON).content(aceite))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decisao").value("ACEITA"))
                .andExpect(jsonPath("$.statusProposta").value("EM_EXECUCAO"));

        mvc.perform(get("/api/propostas/{id}", propostaId).header(HttpHeaders.AUTHORIZATION, bearer(tokenVizinho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.resposta.decisao").value("ACEITA"))
                .andExpect(jsonPath("$.totalComentarios").value(0));

        // 9. filtros e "minhas"
        mvc.perform(get("/api/propostas").param("status", "EM_EXECUCAO").param("bairroId", bairroId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVizinho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/propostas/minhas").header(HttpHeaders.AUTHORIZATION, bearer(tokenVizinho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));

        // 10. gestor conclui
        mvc.perform(patch("/api/propostas/{id}/concluir", propostaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        // 11. autor recebeu notificacoes (respondida + concluida) e so ele marca como lida
        MvcResult notifs = mvc.perform(get("/api/notificacoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[*].lida", hasItem(false)))
                .andReturn();
        long notificacaoId = objectMapper.readTree(notifs.getResponse().getContentAsString())
                .get("conteudo").get(0).get("id").asLong();

        mvc.perform(patch("/api/notificacoes/{id}/lida", notificacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenVizinho)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/notificacoes/{id}/lida", notificacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lida").value(true));

        // 12. placar reflete a proposta concluida e respondida no prazo
        mvc.perform(get("/api/bairros/{id}/placar", bairroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porStatus.CONCLUIDA").value(1))
                .andExpect(jsonPath("$.demandasRespondidasNoPrazo").value(1))
                .andExpect(jsonPath("$.percentualRespondidasNoPrazo").value(100.0));
    }

    @Test
    void autorExcluiPropostaAbertaSemApoios() throws Exception {
        MvcResult criada = mvc.perform(post("/api/propostas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROPOSTA_VALIDA.formatted(bairroId)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(criada.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(delete("/api/propostas/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(tokenVizinho)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/propostas/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/propostas/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(tokenCidadao)))
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM VB_PROPOSTA", Integer.class)).isZero();
    }
}
