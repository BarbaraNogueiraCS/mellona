package br.ufg.inf.mellona.gestao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Visitas (HU-33, RN-51, RN-52) e inspeções (HU-19, RN-24), relações 1:N com apiário e colmeia. */
class RegistroDeCampoTest extends BaseDeTeste {

    Usuario antonio;
    Usuario visitante;
    Apiario serra;
    Colmeia a107;

    @BeforeEach
    void cenario() {
        antonio = acessos.cadastrarUsuario("Antônio Souza", "antonio@mellona.app");
        visitante = acessos.cadastrarUsuario("Pessoa Sem Acesso", "sem.acesso@mellona.app");
        serra = apiarios.criar("Apiário Serra", null);
        a107 = colmeias.cadastrar(serra, "A1-07", null);
        acessos.conceder(antonio, serra, Papel.APICULTOR, T0);
    }

    @Test
    @DisplayName("Apêndice A — as 14 categorias são gravadas uma única vez")
    void categoriasDoApendiceA() {
        repos.garantirCategorias(); // segunda chamada não duplica
        assertEquals(14, repos.categorias.count());
        assertEquals(7, repos.categorias.loadWhere("grupo", GrupoAtividade.CM).size());
        assertEquals("Manejo da rainha", repos.categoria("IB-01").getNome());
        recusadaPor("RN-51", () -> repos.categoria("IB-99"));
    }

    @Test
    @DisplayName("RN-52 — a visita registra apiário, responsável, motivo e data")
    void registrarVisita() {
        Visita visita = campo.registrarVisita(serra, antonio, repos.categoria("IB-01"), T0, "Manejo da rainha da A1-07");

        Visita lida = repos.visitas.loadFromUuid(visita.getUuid()).orElseThrow();
        assertEquals("Apiário Serra", lida.getApiario().getNome());
        assertEquals("Antônio Souza", lida.getResponsavel().getNome());
        assertEquals(GrupoAtividade.IB, lida.getMotivo().getGrupo());
        assertEquals(T0, lida.getData());
        recusadaPor("RN-52", () -> campo.registrarVisita(serra, antonio, null, T0, null));
    }

    @Test
    @DisplayName("RN-51 — CM-07, \"Outros\", exige justificativa")
    void outrosExigeJustificativa() {
        recusadaPor("RN-51", () -> campo.registrarVisita(serra, antonio, repos.categoria("CM-07"), T0, "  "));
        campo.registrarVisita(serra, antonio, repos.categoria("CM-07"), T0, "Retirar galho caído sobre a cerca");
    }

    @Test
    @DisplayName("RNF-16 — só registra quem tem acesso ativo ao apiário")
    void exigeAcesso() {
        recusadaPor("RNF-16", () -> campo.registrarVisita(serra, visitante, repos.categoria("CM-02"), T0, null));
        recusadaPor("RNF-16", () -> campo.registrarInspecao(a107, visitante, T0, null, null, null));
    }

    @Test
    @DisplayName("1:N — a colmeia acumula inspeções, listadas da mais recente para a mais antiga")
    void inspecoesDaColmeia() {
        campo.registrarInspecao(a107, antonio, T0, SituacaoRainha.VISTA, "Boas", null);
        campo.registrarInspecao(a107, antonio, T0.plusSeconds(7 * 86_400), SituacaoRainha.SINAIS, "Boas", "Pouca reserva de mel");

        List<Inspecao> inspecoes = campo.inspecoesDe(a107);

        assertEquals(2, inspecoes.size());
        assertEquals(SituacaoRainha.SINAIS, inspecoes.get(0).getRainha());
        assertEquals("Pouca reserva de mel", inspecoes.get(0).getObservacoes());
        assertEquals(SituacaoRainha.VISTA, inspecoes.get(1).getRainha());
    }

    @Test
    @DisplayName("RN-24 — inspeção exige colmeia, e colmeia inativa não recebe inspeção")
    void inspecaoExigeColmeiaAtiva() {
        recusadaPor("RN-24", () -> campo.registrarInspecao(null, antonio, T0, null, null, null));
        colmeias.inativar(a107, T0);
        recusadaPor("RN-24", () -> campo.registrarInspecao(a107, antonio, T0, null, null, null));
        assertTrue(campo.inspecoesDe(a107).isEmpty());
    }
}
