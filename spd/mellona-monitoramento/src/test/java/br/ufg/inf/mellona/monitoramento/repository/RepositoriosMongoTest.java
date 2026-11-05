package br.ufg.inf.mellona.monitoramento.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;

import br.ufg.inf.mellona.monitoramento.model.Dominios.EstadoAlerta;
import br.ufg.inf.mellona.monitoramento.model.Dominios.Prioridade;
import br.ufg.inf.mellona.monitoramento.model.Dominios.TipoOcorrencia;
import br.ufg.inf.mellona.monitoramento.model.Medicao;
import br.ufg.inf.mellona.monitoramento.model.Ocorrencia;

/**
 * Testes dos repositórios contra um MongoDB de verdade (container Docker do README).
 * Ficam fora do {@code mvn test} padrão; rode com {@code mvn -pl mellona-monitoramento test -Dgroups=integracao -Dtestes.excluidos=}.
 */
@Tag("integracao")
@DataMongoTest
class RepositoriosMongoTest {

    static final Instant T0 = Instant.parse("2026-10-21T05:00:00Z");

    @Autowired
    MedicaoRepository medicoes;

    @Autowired
    OcorrenciaRepository ocorrencias;

    @BeforeEach
    void limpar() {
        medicoes.deleteAll();
        ocorrencias.deleteAll();
    }

    @Test
    @DisplayName("RN-67: o índice único de eventId recusa a mesma leitura duas vezes")
    void eventIdUnico() {
        medicoes.save(Medicao.leitura("evt-1", "leitura.temperatura", "no-A1-07", "colmeia:A1-07", T0, 1, 34.8));
        assertThrows(DuplicateKeyException.class, () ->
                medicoes.save(Medicao.leitura("evt-1", "leitura.temperatura", "no-A1-07", "colmeia:A1-07", T0, 1, 34.8)));
    }

    @Test
    @DisplayName("histórico de uma colmeia vem no período pedido, em ordem de ocorrência")
    void historicoPorPeriodo() {
        for (int i = 0; i < 5; i++) {
            medicoes.save(Medicao.leitura("evt-" + i, "leitura.temperatura", "no-A1-07", "colmeia:A1-07",
                    T0.plusSeconds(60L * (4 - i)), i, 34.0 + i));
        }
        medicoes.save(Medicao.leitura("evt-x", "leitura.temperatura", "no-B2-03", "colmeia:B2-03", T0, 1, 30.0));

        List<Medicao> lista = medicoes.findByEntityIdAndEventTimeBetweenOrderByEventTimeAsc(
                "colmeia:A1-07", T0, T0.plusSeconds(180));

        assertEquals(4, lista.size());
        assertEquals(T0, lista.get(0).getEventTime());
    }

    @Test
    @DisplayName("alertas ativos de um apiário, do mais recente ao mais antigo")
    void alertasAtivos() {
        Ocorrencia antiga = ocorrencias.save(Ocorrencia.nova("serra", TipoOcorrencia.PRESENCA, Prioridade.BAIXA, T0, null));
        ocorrencias.save(Ocorrencia.nova("serra", TipoOcorrencia.OCORRENCIA_SUSPEITA, Prioridade.ALTA, T0.plusSeconds(60), null));
        antiga.resolver(T0.plusSeconds(120));
        ocorrencias.save(antiga);

        List<Ocorrencia> ativas = ocorrencias.findByApiarioUuidAndEstadoOrderByInstanteDesc("serra", EstadoAlerta.ATIVO);

        assertEquals(1, ativas.size());
        assertEquals(TipoOcorrencia.OCORRENCIA_SUSPEITA, ativas.get(0).getTipo());
    }
}
