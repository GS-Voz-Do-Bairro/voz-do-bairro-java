package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.Papel;
import br.com.fiap.vozdobairro.domain.Usuario;
import br.com.fiap.vozdobairro.dto.LoginRequest;
import br.com.fiap.vozdobairro.dto.RegisterRequest;
import br.com.fiap.vozdobairro.dto.TokenResponse;
import br.com.fiap.vozdobairro.exception.CampoInvalidoException;
import br.com.fiap.vozdobairro.exception.ConflitoException;
import br.com.fiap.vozdobairro.exception.CredenciaisInvalidasException;
import br.com.fiap.vozdobairro.repository.BairroRepository;
import br.com.fiap.vozdobairro.repository.UsuarioRepository;
import br.com.fiap.vozdobairro.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BairroRepository bairroRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository, BairroRepository bairroRepository,
                       PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.bairroRepository = bairroRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /** Cadastro publico: sempre cria CIDADAO (GESTOR so via banco/seed). */
    @Transactional
    public TokenResponse registrar(RegisterRequest req) {
        String email = normalizarEmail(req.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um usuário cadastrado com este e-mail.");
        }
        Bairro bairro = bairroRepository.findById(req.bairroId())
                .orElseThrow(() -> new CampoInvalidoException("bairroId", "bairro não encontrado"));

        Usuario usuario = new Usuario(req.nome().trim(), email, passwordEncoder.encode(req.senha()),
                Papel.CIDADAO, bairro);
        usuarioRepository.save(usuario);
        return tokenService.gerar(usuario);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(req.email()))
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!passwordEncoder.matches(req.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return tokenService.gerar(usuario);
    }

    private static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
