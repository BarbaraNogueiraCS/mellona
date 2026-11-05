package br.ufg.inf.mellona.integrador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/** Contrato da mensagem publicada no Redis (RN-73, RN-74). */
class OperacaoSincronizacaoTest {

    static final Instant T0 = Instant.parse("2026-10-21T10:00:00Z");
    static final UUID COLMEIA = UUID.fromString("a3c1e2d4-5b6f-4a7b-8c9d-0e1f2a3b4c5d");

    static JsonObject dadosColmeia() {
        JsonObject dados = new JsonObject();
        dados.addProperty("codigo", "A1-07");
        dados.addProperty("apiarioNome", "Apiário Serra");
        dados.addProperty("ativa", true);
        return dados;
    }

    @Test
    @DisplayName("ida e volta em JSON preserva todos os campos, inclusive acentos")
    void idaEVoltaEmJson() {
        OperacaoSincronizacao op = OperacaoSincronizacao.nova("Colmeia", COLMEIA, TipoOperacao.CRIACAO,
                Base.GESTAO, T0, dadosColmeia());

        OperacaoSincronizacao lida = OperacaoSincronizacao.fromJson(op.toJson());

        assertEquals(op.getOperacaoId(), lida.getOperacaoId());
        assertEquals("Colmeia", lida.getEntidade());
        assertEquals(COLMEIA, lida.getEntidadeUuid());
        assertEquals(TipoOperacao.CRIACAO, lida.getTipo());
        assertEquals(Base.GESTAO, lida.getOrigem());
        assertEquals(T0, lida.getInstante());
        assertEquals("Apiário Serra", lida.getDados().get("apiarioNome").getAsString());
        assertTrue(op.toJson().contains("\"schemaVersion\":1"));
    }

    @Test
    @DisplayName("RN-74: cada operação tem identificador próprio, mesmo sobre o mesmo registro")
    void identificadorProprio() {
        OperacaoSincronizacao a = OperacaoSincronizacao.nova("Colmeia", COLMEIA, TipoOperacao.ALTERACAO, Base.GESTAO, T0, null);
        OperacaoSincronizacao b = OperacaoSincronizacao.nova("Colmeia", COLMEIA, TipoOperacao.ALTERACAO, Base.GESTAO, T0, null);
        assertNotEquals(a.getOperacaoId(), b.getOperacaoId());
    }

    @Test
    @DisplayName("RN-74: a operação não volta à base de origem")
    void naoRepropaga() {
        OperacaoSincronizacao op = OperacaoSincronizacao.nova("Visita", UUID.randomUUID(), TipoOperacao.CRIACAO,
                Base.MONITORAMENTO, T0, null);
        assertTrue(op.deveSerAplicadaEm(Base.GESTAO));
        assertFalse(op.deveSerAplicadaEm(Base.MONITORAMENTO));
    }

    @Test
    @DisplayName("RN-74: cada entidade só parte da base prevista no ERS")
    void sentidoDaPropagacao() {
        assertThrows(IllegalArgumentException.class, () -> OperacaoSincronizacao.nova("Ocorrencia", UUID.randomUUID(),
                TipoOperacao.CRIACAO, Base.GESTAO, T0, null));
        assertThrows(IllegalArgumentException.class, () -> OperacaoSincronizacao.nova("Colmeia", UUID.randomUUID(),
                TipoOperacao.CRIACAO, Base.MONITORAMENTO, T0, null));
        assertThrows(IllegalArgumentException.class, () -> OperacaoSincronizacao.nova("Medicao", UUID.randomUUID(),
                TipoOperacao.CRIACAO, Base.MONITORAMENTO, T0, null), "medições não são compartilhadas");
        OperacaoSincronizacao.nova("Ocorrencia", UUID.randomUUID(), TipoOperacao.ALTERACAO, Base.MONITORAMENTO, T0, null);
    }

    @Test
    @DisplayName("mensagens incompletas ou malformadas são recusadas na leitura")
    void mensagensInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> OperacaoSincronizacao.fromJson("{não é json"));
        assertThrows(IllegalArgumentException.class, () -> OperacaoSincronizacao.fromJson("{}"));
        String semInstante = OperacaoSincronizacao.nova("Colmeia", COLMEIA, TipoOperacao.CRIACAO, Base.GESTAO, T0, null)
                .toJson().replace("\"instante\":\"2026-10-21T10:00:00Z\",", "");
        assertThrows(IllegalArgumentException.class, () -> OperacaoSincronizacao.fromJson(semInstante));
    }

    @Test
    @DisplayName("os dados devolvidos são cópia: o consumidor não altera a mensagem")
    void dadosImutaveis() {
        OperacaoSincronizacao op = OperacaoSincronizacao.nova("Colmeia", COLMEIA, TipoOperacao.CRIACAO, Base.GESTAO, T0, dadosColmeia());
        op.getDados().addProperty("codigo", "ALTERADO");
        assertEquals("A1-07", op.getDados().get("codigo").getAsString());
    }
}
