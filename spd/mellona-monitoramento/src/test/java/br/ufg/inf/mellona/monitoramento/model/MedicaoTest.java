package br.ufg.inf.mellona.monitoramento.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.ufg.inf.mellona.monitoramento.model.Dominios.Qualidade;

/** Qualidade das leituras (RN-07, RN-22, PAR-04 a PAR-06). Teste de unidade, sem MongoDB. */
class MedicaoTest {

    static final Instant T0 = Instant.parse("2026-10-21T05:14:31Z");

    static Medicao leitura(String tipo, Double valor) {
        return Medicao.leitura("evt-1", tipo, "no-colmeia-A1-07", "colmeia:A1-07", T0, 1842, valor);
    }

    @ParameterizedTest(name = "{0} = {1} -> {2}")
    @CsvSource({
            "leitura.temperatura, -10.0, VALIDO",
            "leitura.temperatura, 34.8, VALIDO",
            "leitura.temperatura, 60.0, VALIDO",
            "leitura.temperatura, 60.1, INVALIDO",
            "leitura.temperatura, -10.5, INVALIDO",
            "leitura.umidade, 0.0, VALIDO",
            "leitura.umidade, 62.0, VALIDO",
            "leitura.umidade, 100.0, VALIDO",
            "leitura.umidade, 100.1, INVALIDO"
    })
    @DisplayName("PAR-05 e PAR-06: faixas válidas de temperatura e umidade, limites incluídos")
    void faixasValidas(String tipo, double valor, Qualidade esperada) {
        assertEquals(esperada, leitura(tipo, valor).getQuality());
    }

    @Test
    @DisplayName("RN-07: leitura ausente é inválida, nunca normal")
    void leituraAusenteEInvalida() {
        assertEquals(Qualidade.INVALIDO, leitura("leitura.temperatura", null).getQuality());
    }

    @Test
    @DisplayName("a unidade segue o tipo de leitura do contrato de eventos")
    void unidadePorTipo() {
        assertEquals("C", leitura("leitura.temperatura", 30.0).getUnit());
        assertEquals("%RH", leitura("leitura.umidade", 60.0).getUnit());
        assertThrows(IllegalArgumentException.class, () -> leitura("presenca.confirmada", 1.0));
    }

    @Test
    @DisplayName("PAR-04 e RN-22: leitura válida com mais de 180 s aparece como desatualizada")
    void leituraAntigaFicaDesatualizada() {
        Medicao m = leitura("leitura.temperatura", 34.8);
        assertEquals(Qualidade.VALIDO, m.qualidadeEm(T0.plusSeconds(180)));
        assertEquals(Qualidade.DESATUALIZADO, m.qualidadeEm(T0.plusSeconds(181)));
        assertEquals(Qualidade.INVALIDO, leitura("leitura.temperatura", 99.0).qualidadeEm(T0.plusSeconds(600)));
    }

    @Test
    @DisplayName("receber registra o instante de recebimento e exige os campos do contrato")
    void receberCompletaCampos() {
        Medicao m = leitura("leitura.umidade", 62.0);
        m.receber(T0.plusSeconds(2));
        assertEquals(T0.plusSeconds(2), m.getReceivedAt());
        assertNotNull(m.getQuality());

        Medicao semEvento = Medicao.leitura(null, "leitura.umidade", "no", "colmeia:A1-07", T0, 1, 50.0);
        assertThrows(IllegalArgumentException.class, () -> semEvento.receber(T0));
    }
}
