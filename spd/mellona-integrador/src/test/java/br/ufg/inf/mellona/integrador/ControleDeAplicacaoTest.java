package br.ufg.inf.mellona.integrador;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufg.inf.mellona.integrador.ControleDeAplicacao.Decisao;

/** Idempotência (RN-74) e conflitos (RN-75) na aplicação das operações. */
class ControleDeAplicacaoTest {

    static final Instant T0 = Instant.parse("2026-10-21T10:00:00Z");
    final UUID colmeia = UUID.randomUUID();

    OperacaoSincronizacao alteracao(Instant quando) {
        return OperacaoSincronizacao.nova("Colmeia", colmeia, TipoOperacao.ALTERACAO, Base.GESTAO, quando, null);
    }

    @Test
    @DisplayName("RN-74: a mesma operação recebida duas vezes é aplicada uma vez só")
    void aplicadaUmaVez() {
        ControleDeAplicacao monitoramento = new ControleDeAplicacao(Base.MONITORAMENTO);
        OperacaoSincronizacao op = alteracao(T0);

        assertEquals(Decisao.APLICAR, monitoramento.decidir(op));
        assertEquals(Decisao.IGNORAR_REPETIDA, monitoramento.decidir(OperacaoSincronizacao.fromJson(op.toJson())));
    }

    @Test
    @DisplayName("RN-74: operação nascida na própria base não é reaplicada")
    void mesmaOrigem() {
        ControleDeAplicacao gestao = new ControleDeAplicacao(Base.GESTAO);
        assertEquals(Decisao.IGNORAR_MESMA_ORIGEM, gestao.decidir(alteracao(T0)));
    }

    @Test
    @DisplayName("RN-75: operações fora de ordem — prevalece a mais recente, e a descartada fica registrada")
    void foraDeOrdem() {
        ControleDeAplicacao monitoramento = new ControleDeAplicacao(Base.MONITORAMENTO);
        OperacaoSincronizacao recente = alteracao(T0.plusSeconds(60));
        OperacaoSincronizacao antiga = alteracao(T0);

        assertEquals(Decisao.APLICAR, monitoramento.decidir(recente));
        assertEquals(Decisao.DESCARTAR_MAIS_ANTIGA, monitoramento.decidir(antiga));
        assertEquals(T0.plusSeconds(60), monitoramento.versaoVigente(colmeia).orElseThrow());
        assertEquals(1, monitoramento.getDescartes().size());
        assertEquals(antiga.getOperacaoId(), monitoramento.getDescartes().get(0).descartada().getOperacaoId());
    }

    @Test
    @DisplayName("RN-75: alteração local mais recente vence a operação que chega atrasada")
    void alteracaoLocalVence() {
        ControleDeAplicacao monitoramento = new ControleDeAplicacao(Base.MONITORAMENTO);
        monitoramento.registrarAlteracaoLocal(colmeia, T0.plusSeconds(30));

        assertEquals(Decisao.DESCARTAR_MAIS_ANTIGA, monitoramento.decidir(alteracao(T0)));
        assertEquals(Decisao.APLICAR, monitoramento.decidir(alteracao(T0.plusSeconds(31))));
    }

    @Test
    @DisplayName("RN-75: em empate de instante, a versão vigente é a mesma qualquer que seja a ordem de chegada")
    void empateDeterministico() {
        OperacaoSincronizacao a = alteracao(T0);
        OperacaoSincronizacao b = alteracao(T0);
        String vencedora = a.getOperacaoId().compareTo(b.getOperacaoId()) > 0 ? a.getOperacaoId() : b.getOperacaoId();

        ControleDeAplicacao ordem1 = new ControleDeAplicacao(Base.MONITORAMENTO);
        ordem1.decidir(a);
        ordem1.decidir(b);
        ControleDeAplicacao ordem2 = new ControleDeAplicacao(Base.MONITORAMENTO);
        ordem2.decidir(b);
        ordem2.decidir(a);

        assertEquals(vencedora, ordem1.operacaoVigente(colmeia).orElseThrow());
        assertEquals(vencedora, ordem2.operacaoVigente(colmeia).orElseThrow());
    }
}
