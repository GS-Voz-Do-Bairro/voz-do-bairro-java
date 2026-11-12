package br.com.fiap.vozdobairro.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IaChaveEnvironmentPostProcessorTest {

    private final IaChaveEnvironmentPostProcessor processor = new IaChaveEnvironmentPostProcessor();

    @Test
    void chaveVaziaViraPlaceholder() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("teste",
                Map.of(IaChaveEnvironmentPostProcessor.PROPRIEDADE, "  ")));

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty(IaChaveEnvironmentPostProcessor.PROPRIEDADE)).isEqualTo("sem-chave");
    }

    @Test
    void chaveInformadaEhMantida() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("teste",
                Map.of(IaChaveEnvironmentPostProcessor.PROPRIEDADE, "sk-abc")));

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty(IaChaveEnvironmentPostProcessor.PROPRIEDADE)).isEqualTo("sk-abc");
    }
}
