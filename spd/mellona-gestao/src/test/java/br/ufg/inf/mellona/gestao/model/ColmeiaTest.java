package br.ufg.inf.mellona.gestao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Relação 1:N entre Apiário e Colmeia, com RN-01, RN-02 e RN-04. */
class ColmeiaTest extends BaseDeTeste {

    @Test
    @DisplayName("1:N — o apiário carregado da base navega até suas colmeias")
    void apiarioNavegaAteColmeias() {
        Apiario serra = apiarios.criar("Apiário Serra", null);
        colmeias.cadastrar(serra, "A1-01", LocalDate.of(2026, 3, 10));
        colmeias.cadastrar(serra, "A1-02", null);
        colmeias.cadastrar(serra, "A1-07", null);
        Apiario barreiro = apiarios.criar("Apiário Barreiro", null);
        colmeias.cadastrar(barreiro, "B2-03", null);

        Apiario lido = repos.apiarios.loadFromId(serra.getId()).orElseThrow();
        List<String> codigos = lido.getColmeias().stream().map(Colmeia::getCodigo).sorted().toList();

        assertEquals(List.of("A1-01", "A1-02", "A1-07"), codigos);
        assertEquals(serra.getId(), colmeias.buscarPorCodigo("a1-07").orElseThrow().getApiario().getId());
        assertEquals(LocalDate.of(2026, 3, 10), colmeias.buscarPorCodigo("A1-01").orElseThrow().getDataInstalacao());
    }

    @Test
    @DisplayName("RN-01 — o código da colmeia é único, sem diferenciar maiúsculas")
    void codigoUnico() {
        Apiario serra = apiarios.criar("Apiário Serra", null);
        Apiario barreiro = apiarios.criar("Apiário Barreiro", null);
        colmeias.cadastrar(serra, "A1-07", null);

        recusadaPor("RN-01", () -> colmeias.cadastrar(barreiro, " a1-07 ", null));
        assertEquals(1, repos.colmeias.count());
    }

    @Test
    @DisplayName("RN-01 — a base também recusa código repetido, mesmo fora do cadastro")
    void codigoUnicoNaBase() {
        Apiario serra = apiarios.criar("Apiário Serra", null);
        repos.colmeias.create(new Colmeia("A1-07", serra, null));

        org.junit.jupiter.api.Assertions.assertThrows(PersistenciaException.class,
                () -> repos.colmeias.create(new Colmeia("A1-07", serra, null)));
    }

    @Test
    @DisplayName("RN-02 — colmeia só é cadastrada ou movida para apiário ativo")
    void apiarioPrecisaEstarAtivo() {
        Apiario serra = apiarios.criar("Apiário Serra", null);
        Apiario antigo = apiarios.criar("Apiário Antigo", null);
        apiarios.inativar(antigo);
        Colmeia a107 = colmeias.cadastrar(serra, "A1-07", null);

        recusadaPor("RN-02", () -> colmeias.cadastrar(antigo, "X-01", null));
        recusadaPor("RN-02", () -> colmeias.mover(a107, antigo));
    }

    @Test
    @DisplayName("RN-02 — mover a colmeia troca seu único apiário")
    void moverTrocaApiario() {
        Apiario serra = apiarios.criar("Apiário Serra", null);
        Apiario barreiro = apiarios.criar("Apiário Barreiro", null);
        Colmeia a107 = colmeias.cadastrar(serra, "A1-07", null);

        colmeias.mover(a107, barreiro);

        assertTrue(colmeias.colmeiasAtivasDe(serra).isEmpty());
        assertEquals(List.of(a107), colmeias.colmeiasAtivasDe(barreiro));
    }

    @Test
    @DisplayName("RN-02 e RN-04 — apiário com colmeias ativas não é inativado; depois de inativá-las, é, e nada é apagado")
    void inativacaoPreservaHistorico() {
        Apiario serra = apiarios.criar("Apiário Serra", null);
        Colmeia a101 = colmeias.cadastrar(serra, "A1-01", null);
        Colmeia a102 = colmeias.cadastrar(serra, "A1-02", null);

        recusadaPor("RN-02", () -> apiarios.inativar(serra));

        colmeias.inativar(a101, T0);
        colmeias.inativar(a102, T0);
        apiarios.inativar(serra);

        assertFalse(repos.apiarios.loadFromId(serra.getId()).orElseThrow().isAtivo());
        assertTrue(apiarios.listarAtivos().isEmpty());
        assertEquals(2, repos.colmeias.count(), "as colmeias continuam na base");
        assertEquals(1, repos.apiarios.count());
    }
}
