package br.ufg.inf.mellona.monitoramento.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufg.inf.mellona.monitoramento.model.Dominios.EstadoAlerta;
import br.ufg.inf.mellona.monitoramento.model.Dominios.Prioridade;
import br.ufg.inf.mellona.monitoramento.model.Dominios.Recurso;
import br.ufg.inf.mellona.monitoramento.model.Dominios.TipoOcorrencia;

/** Estados do alerta (RN-21) e acionamentos embutidos. Teste de unidade, sem MongoDB. */
class OcorrenciaTest {

    static final Instant T0 = Instant.parse("2026-10-21T05:14:32Z");
    static final String SERRA = "3b0b2f4e-0000-4000-8000-000000000001";

    static Ocorrencia suspeita() {
        return Ocorrencia.nova(SERRA, TipoOcorrencia.OCORRENCIA_SUSPEITA, Prioridade.ALTA, T0,
                "Presença e colmeia A1-07 mexida em 18 s, período noturno");
    }

    @Test
    @DisplayName("nova ocorrência nasce ativa, com uuid próprio")
    void nasceAtiva() {
        Ocorrencia o = suspeita();
        assertEquals(EstadoAlerta.ATIVO, o.getEstado());
        assertNotNull(o.getUuid());
        assertThrows(IllegalArgumentException.class,
                () -> Ocorrencia.nova(null, TipoOcorrencia.PRESENCA, Prioridade.BAIXA, T0, null));
    }

    @Test
    @DisplayName("RN-21: ativo → reconhecido (\"Estou ciente\") → resolvido (\"Encerrar alerta\")")
    void cicloCompleto() {
        Ocorrencia o = suspeita();
        o.reconhecer("usuario-antonio", T0.plusSeconds(300));
        assertEquals(EstadoAlerta.RECONHECIDO, o.getEstado());
        assertEquals("usuario-antonio", o.getReconhecidoPor());

        o.resolver(T0.plusSeconds(3600));
        assertEquals(EstadoAlerta.RESOLVIDO, o.getEstado());
        assertEquals(T0.plusSeconds(3600), o.getAtualizadoEm());
    }

    @Test
    @DisplayName("RN-21: só alerta ativo é reconhecido; resolvido é estado final")
    void transicoesInvalidas() {
        Ocorrencia o = suspeita();
        o.reconhecer("usuario-antonio", T0);
        assertThrows(IllegalStateException.class, () -> o.reconhecer("usuario-carla", T0));

        o.resolver(T0);
        assertThrows(IllegalStateException.class, () -> o.resolver(T0));
        assertThrows(IllegalStateException.class, () -> o.reconhecer("usuario-carla", T0));
    }

    @Test
    @DisplayName("um alerta ativo pode ser encerrado sem ter sido reconhecido")
    void resolverDireto() {
        Ocorrencia o = suspeita();
        o.resolver(T0.plusSeconds(60));
        assertEquals(EstadoAlerta.RESOLVIDO, o.getEstado());
    }

    @Test
    @DisplayName("acionamentos ficam embutidos na ocorrência (1:N como documentos embutidos)")
    void acionamentosEmbutidos() {
        Ocorrencia o = suspeita();
        o.registrarAcionamento(new Acionamento(Recurso.SOM, T0, T0.plusSeconds(30)));
        o.registrarAcionamento(new Acionamento(Recurso.LUZ, T0, T0.plusSeconds(30)));

        assertEquals(List.of(Recurso.SOM, Recurso.LUZ), o.getAcionamentos().stream().map(Acionamento::recurso).toList());
        assertThrows(IllegalArgumentException.class, () -> new Acionamento(Recurso.SOM, T0, T0.minusSeconds(1)));
    }
}
