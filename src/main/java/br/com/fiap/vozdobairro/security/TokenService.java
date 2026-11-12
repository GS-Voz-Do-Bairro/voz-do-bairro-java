package br.com.fiap.vozdobairro.security;

import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.TokenResponse;
import br.com.fiap.vozdobairro.mapper.UsuarioMapper;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/** Emite o JWT no formato do contrato: iss, sub (id), email, name, role, iat, exp (2h). */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties props, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.clock = clock;
    }

    /** Deve ser chamado dentro de transacao (le usuario.bairro, que e LAZY). */
    public TokenResponse gerar(Usuario usuario) {
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(props.expiracao());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(String.valueOf(usuario.getId()))
                .claim(JwtConfig.CLAIM_EMAIL, usuario.getEmail())
                .claim(JwtConfig.CLAIM_NAME, usuario.getNome())
                .claim(JwtConfig.CLAIM_ROLE, usuario.getPapel().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", expiraEm, UsuarioMapper.paraToken(usuario));
    }
}
