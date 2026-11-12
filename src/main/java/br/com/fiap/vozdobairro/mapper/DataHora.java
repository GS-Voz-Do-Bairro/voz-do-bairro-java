package br.com.fiap.vozdobairro.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** As colunas TIMESTAMP guardam horario UTC; na API elas saem como Instant ISO-8601 com "Z". */
public final class DataHora {

    private DataHora() {
    }

    public static Instant paraInstant(LocalDateTime valor) {
        return valor == null ? null : valor.toInstant(ZoneOffset.UTC);
    }
}
