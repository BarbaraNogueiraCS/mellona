package br.ufg.inf.mellona.gestao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Relação N:N com histórico entre Dispositivo e Colmeia (RN-03, RN-04). */
class AssociacaoDispositivoTest extends BaseDeTeste {

    Apiario serra;
    Apiario barreiro;
    Colmeia a107;
    Colmeia a108;
    Colmeia b203;
    Dispositivo no;

    @BeforeEach
    void cenario() {
        serra = apiarios.criar("Apiário Serra", null);
        barreiro = apiarios.criar("Apiário Barreiro", null);
        a107 = colmeias.cadastrar(serra, "A1-07", null);
        a108 = colmeias.cadastrar(serra, "A1-08", null);
        b203 = colmeias.cadastrar(barreiro, "B2-03", null);
        no = dispositivos.cadastrar("no-colmeia-01", TipoDispositivo.NO_COLMEIA);
    }

    @Test
    @DisplayName("N:N — um equipamento passa por várias colmeias, e o histórico guarda cada período")
    void historicoDeColmeias() {
        dispositivos.associar(no, serra, a107, T0);
        dispositivos.encerrarAssociacao(no, T0.plus(Duration.ofDays(30)));
        dispositivos.associar(no, serra, a108, T0.plus(Duration.ofDays(30)));

        List<AssociacaoDispositivo> historico = dispositivos.historico(no);

        assertEquals(2, historico.size());
        assertEquals("A1-07", historico.get(0).getColmeia().getCodigo());
        assertFalse(historico.get(0).isAtiva());
        assertEquals(T0.plus(Duration.ofDays(30)), historico.get(0).getFim());
        assertEquals("A1-08", historico.get(1).getColmeia().getCodigo());
        assertTrue(historico.get(1).isAtiva());
    }

    @Test
    @DisplayName("N:N — uma colmeia recebe vários equipamentos ao longo do tempo")
    void colmeiaComVariosEquipamentos() {
        Dispositivo novo = dispositivos.cadastrar("no-colmeia-02", TipoDispositivo.NO_COLMEIA);
        dispositivos.associar(no, serra, a107, T0);
        dispositivos.encerrarAssociacao(no, T0.plusSeconds(3600));
        dispositivos.associar(novo, serra, a107, T0.plusSeconds(3600));

        assertEquals(List.of(novo), dispositivos.dispositivosAtivosNa(a107));
        assertEquals(2, repos.associacoes.loadWhere("colmeia_id", a107).size());
    }

    @Test
    @DisplayName("RN-03 — o equipamento não pode ter duas associações ativas")
    void semDuasAssociacoesAtivas() {
        dispositivos.associar(no, serra, a107, T0);
        recusadaPor("RN-03", () -> dispositivos.associar(no, serra, a108, T0.plusSeconds(60)));
    }

    @Test
    @DisplayName("RN-03 — a colmeia não pode ter dois sensores de colmeia ativos")
    void umSensorPorColmeia() {
        Dispositivo outro = dispositivos.cadastrar("no-colmeia-02", TipoDispositivo.NO_COLMEIA);
        dispositivos.associar(no, serra, a107, T0);
        recusadaPor("RN-03", () -> dispositivos.associar(outro, serra, a107, T0));
    }

    @Test
    @DisplayName("RN-03 — o apiário não pode ter duas centrais ativas")
    void umaCentralPorApiario() {
        Dispositivo central1 = dispositivos.cadastrar("seguranca-apiario-A1", TipoDispositivo.SEGURANCA_LOCAL);
        Dispositivo central2 = dispositivos.cadastrar("seguranca-apiario-A1b", TipoDispositivo.SEGURANCA_LOCAL);
        AssociacaoDispositivo a = dispositivos.associar(central1, serra, null, T0);
        assertNull(a.getColmeia());

        recusadaPor("RN-03", () -> dispositivos.associar(central2, serra, null, T0));
        dispositivos.associar(central2, barreiro, null, T0); // outro apiário: permitido
    }

    @Test
    @DisplayName("RN-03 — sensor de colmeia exige colmeia do mesmo apiário; equipamento de área não aceita colmeia")
    void localDoEquipamento() {
        Dispositivo presenca = dispositivos.cadastrar("presenca-A1", TipoDispositivo.SENSOR_PRESENCA);

        recusadaPor("RN-03", () -> dispositivos.associar(no, serra, null, T0));
        recusadaPor("RN-03", () -> dispositivos.associar(no, serra, b203, T0));
        recusadaPor("RN-03", () -> dispositivos.associar(presenca, serra, a107, T0));
        dispositivos.associar(presenca, serra, null, T0);
    }

    @Test
    @DisplayName("RN-03 — identificador de equipamento é único")
    void identificadorUnico() {
        recusadaPor("RN-03", () -> dispositivos.cadastrar("no-colmeia-01", TipoDispositivo.SENSOR_PRESENCA));
    }

    @Test
    @DisplayName("RN-04 — inativar a colmeia encerra a associação ativa, que fica no histórico")
    void inativarColmeiaEncerraAssociacao() {
        dispositivos.associar(no, serra, a107, T0);

        colmeias.inativar(a107, T0.plusSeconds(600));

        assertTrue(dispositivos.associacaoAtiva(no).isEmpty());
        assertEquals(1, dispositivos.historico(no).size());
        assertEquals(T0.plusSeconds(600), dispositivos.historico(no).get(0).getFim());
        dispositivos.associar(no, serra, a108, T0.plusSeconds(700)); // livre para outra colmeia
    }
}
