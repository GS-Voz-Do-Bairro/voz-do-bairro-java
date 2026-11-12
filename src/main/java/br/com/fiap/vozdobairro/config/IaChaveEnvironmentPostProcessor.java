package br.com.fiap.vozdobairro.config;

import br.com.fiap.vozdobairro.ai.IaService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Garante que spring.ai.openai.api-key nunca fique vazia.
 *
 * <p>O autoconfigure do Spring AI (OpenAI) aborta a subida da aplicacao se a chave estiver vazia.
 * O default "${OPENAI_API_KEY:sem-chave}" do application.properties so cobre a variavel AUSENTE;
 * se OPENAI_API_KEY existir mas vier vazia (comum em Docker/Azure), o valor seria "".
 * Este post-processor roda depois do carregamento do application.properties e, nesse caso,
 * injeta o placeholder "sem-chave", que o {@link IaService} reconhece para usar o fallback.
 *
 * <p>Registrado em META-INF/spring.factories (nao e um bean).
 */
public class IaChaveEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPRIEDADE = "spring.ai.openai.api-key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String valor;
        try {
            valor = environment.getProperty(PROPRIEDADE);
        } catch (IllegalArgumentException e) {
            valor = null; // placeholder nao resolvido
        }
        if (!StringUtils.hasText(valor)) {
            environment.getPropertySources().addFirst(new MapPropertySource("vozdobairro-ia-chave-padrao",
                    Map.of(PROPRIEDADE, IaService.CHAVE_PLACEHOLDER)));
        }
    }

    @Override
    public int getOrder() {
        // Depois do ConfigDataEnvironmentPostProcessor (que carrega application.properties).
        return Ordered.LOWEST_PRECEDENCE;
    }
}
