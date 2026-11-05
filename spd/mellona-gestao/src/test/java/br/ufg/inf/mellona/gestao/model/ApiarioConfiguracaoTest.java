package br.ufg.inf.mellona.gestao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Relação 1:1 entre Apiário e Configuração de segurança (RN-60, HU-37, PAR-10 a PAR-14). */
class ApiarioConfiguracaoTest extends BaseDeTeste {

    @Test
    @DisplayName("1:1 — criar o apiário cria sua configuração com os valores iniciais do ERS")
    void criarApiarioCriaConfiguracaoPadrao() {
        Apiario serra = apiarios.criar("Apiário Serra", null);

        ConfiguracaoSeguranca cfg = apiarios.configuracaoDe(serra);

        assertEquals(serra.getId(), cfg.getApiario().getId());
        assertEquals(LocalTime.of(6, 0), cfg.getInicioDiurno());
        assertEquals(LocalTime.of(18, 0), cfg.getInicioNoturno());
        assertEquals(60, cfg.getIntervaloCorrelacaoS());
        assertEquals(30, cfg.getDuracaoSinalizacaoS());
        assertEquals(120, cfg.getIntervaloProtecaoS());
        assertEquals(60, cfg.getDuracaoAutorizacaoMin());
        assertFalse(cfg.isArmada(), "a proteção nasce desligada");
        assertEquals(1, cfg.getVersao());
        assertEquals(1, repos.configuracoes.count());
    }

    @Test
    @DisplayName("1:1 — a base recusa uma segunda configuração para o mesmo apiário")
    void segundaConfiguracaoERecusada() {
        Apiario serra = apiarios.criar("Apiário Serra", null);

        assertThrows(PersistenciaException.class,
                () -> repos.configuracoes.create(new ConfiguracaoSeguranca(serra)));
        assertEquals(1, repos.configuracoes.count());
    }

    @Test
    @DisplayName("salvar a configuração grava os parâmetros e incrementa a versão enviada à central")
    void salvarIncrementaVersao() {
        Apiario serra = apiarios.criar("Apiário Serra", null);
        ConfiguracaoSeguranca cfg = apiarios.configuracaoDe(serra);

        cfg.setDuracaoSinalizacaoS(45);
        cfg.setArmada(true);
        apiarios.salvarConfiguracao(cfg);

        ConfiguracaoSeguranca lida = apiarios.configuracaoDe(serra);
        assertEquals(45, lida.getDuracaoSinalizacaoS());
        assertEquals(true, lida.isArmada());
        assertEquals(2, lida.getVersao());
    }

    @Test
    @DisplayName("PAR-10 a PAR-13: valores fora das faixas configuráveis são recusados")
    void faixasConfiguraveis() {
        ConfiguracaoSeguranca cfg = apiarios.configuracaoDe(apiarios.criar("Apiário Serra", null));

        recusadaPor("PAR-10", () -> cfg.setIntervaloCorrelacaoS(5));
        recusadaPor("PAR-11", () -> cfg.setDuracaoSinalizacaoS(121));
        recusadaPor("PAR-12", () -> cfg.setIntervaloProtecaoS(29));
        recusadaPor("PAR-13", () -> cfg.setDuracaoAutorizacaoMin(241));
        cfg.setDuracaoSinalizacaoS(120); // limite superior é aceito
        assertEquals(120, cfg.getDuracaoSinalizacaoS());
    }

    @Test
    @DisplayName("PAR-14: dia e noite precisam começar em horários válidos e diferentes")
    void periodosDoDia() {
        ConfiguracaoSeguranca cfg = apiarios.configuracaoDe(apiarios.criar("Apiário Serra", null));

        recusadaPor("PAR-14", () -> cfg.definirPeriodos("06:00", "06:00"));
        recusadaPor("PAR-14", () -> cfg.definirPeriodos("6h", "18:00"));
        cfg.definirPeriodos("05:30", "18:30");
        assertEquals(LocalTime.of(5, 30), cfg.getInicioDiurno());
    }

    @Test
    @DisplayName("RN-60: o fuso horário do apiário precisa existir")
    void fusoHorarioValido() {
        recusadaPor("RN-60", () -> apiarios.criar("Apiário Serra", null, "Brasil/Goias"));
        Apiario cuiaba = apiarios.criar("Apiário Chapada", null, "America/Cuiaba");
        assertEquals("America/Cuiaba", repos.apiarios.loadFromId(cuiaba.getId()).orElseThrow().getFusoHorario());
        assertEquals(0, repos.apiarios.loadWhere("nome", "Apiário Serra").size(), "nada é gravado quando o fuso é inválido");
    }
}
