package br.com.fiap.vozdobairro.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracao do JWT compartilhado com a API .NET (CONTRATO.md, secao 4).
 *
 * @param secret    valor de JWT_SECRET (UTF-8, minimo 32 bytes; os bytes da string sao a chave HS256)
 * @param issuer    "voz-do-bairro"
 * @param expiracao validade do token (2h)
 */
@ConfigurationProperties(prefix = "vozdobairro.jwt")
public record JwtProperties(String secret, String issuer, Duration expiracao) {
}
