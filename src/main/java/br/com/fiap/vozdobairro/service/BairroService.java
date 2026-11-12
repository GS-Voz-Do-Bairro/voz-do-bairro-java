package br.com.fiap.vozdobairro.service;

import br.com.fiap.vozdobairro.domain.Bairro;
import br.com.fiap.vozdobairro.domain.StatusProposta;
import br.com.fiap.vozdobairro.dto.BairroResponse;
import br.com.fiap.vozdobairro.dto.PlacarResponse;
import br.com.fiap.vozdobairro.exception.RecursoNaoEncontradoException;
import br.com.fiap.vozdobairro.mapper.BairroMapper;
import br.com.fiap.vozdobairro.repository.BairroRepository;
import br.com.fiap.vozdobairro.repository.ContagemPorStatus;
import br.com.fiap.vozdobairro.repository.PropostaRepository;
import br.com.fiap.vozdobairro.repository.RespostaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BairroService {

    private final BairroRepository bairroRepository;
    private final PropostaRepository propostaRepository;
    private final RespostaRepository respostaRepository;
    private final Clock clock;

    public BairroService(BairroRepository bairroRepository, PropostaRepository propostaRepository,
                         RespostaRepository respostaRepository, Clock clock) {
        this.bairroRepository = bairroRepository;
        this.propostaRepository = propostaRepository;
        this.respostaRepository = respostaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BairroResponse> listar() {
        return bairroRepository.findAll(Sort.by("nome")).stream().map(BairroMapper::paraResponse).toList();
    }

    /**
     * Placar publico do bairro: contagem por status e % de demandas respondidas no prazo.
     * Base do percentual = demandas ja respondidas + demandas com prazo vencido sem resposta
     * (demandas ainda dentro do prazo e sem resposta nao entram na conta).
     */
    @Transactional(readOnly = true)
    public PlacarResponse placar(Long bairroId) {
        Bairro bairro = bairroRepository.findById(bairroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bairro " + bairroId + " não encontrado."));

        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (StatusProposta s : StatusProposta.values()) {
            porStatus.put(s.name(), 0L);
        }
        long total = 0;
        for (ContagemPorStatus c : propostaRepository.contarPorStatus(bairroId)) {
            long qtd = c.total() == null ? 0L : c.total();
            porStatus.put(c.status().name(), qtd);
            total += qtd;
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        long respondidas = respostaRepository.contarRespondidasDoBairro(bairroId);
        long noPrazo = respostaRepository.contarRespondidasNoPrazoDoBairro(bairroId);
        long vencidasSemResposta = propostaRepository.contarVencidasSemResposta(bairroId, agora);

        long base = respondidas + vencidasSemResposta;
        Double percentual = base == 0 ? null : Math.round(noPrazo * 1000.0 / base) / 10.0;

        return new PlacarResponse(bairro.getId(), bairro.getNome(), total, porStatus,
                respondidas, noPrazo, vencidasSemResposta, percentual);
    }
}
