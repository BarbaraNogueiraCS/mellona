package br.ufg.inf.mellona.monitoramento.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;

import br.ufg.inf.mellona.monitoramento.model.Dominios.EstadoAlerta;
import br.ufg.inf.mellona.monitoramento.model.Dominios.Prioridade;
import br.ufg.inf.mellona.monitoramento.model.Dominios.TipoOcorrencia;
import br.ufg.inf.mellona.monitoramento.model.Medicao;
import br.ufg.inf.mellona.monitoramento.model.Ocorrencia;
import br.ufg.inf.mellona.monitoramento.repository.MedicaoRepository;
import br.ufg.inf.mellona.monitoramento.repository.OcorrenciaRepository;

/**
 * Regras dos controladores REST com repositórios simulados (Mockito), sem subir o Spring
 * nem o MongoDB.
 */
class ControllersTest {

    static final Instant T0 = Instant.parse("2026-10-21T05:14:31Z");

    @Test
    @DisplayName("RN-67: POST de uma leitura nova grava e responde 201")
    void leituraNova() {
        MedicaoRepository repo = mock(MedicaoRepository.class);
        when(repo.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(repo.save(any(Medicao.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ResponseEntity<Medicao> resposta = new MedicaoController(repo)
                .receber(Medicao.leitura("evt-1", "leitura.temperatura", "no-colmeia-A1-07", "colmeia:A1-07", T0, 1, 34.8));

        assertEquals(201, resposta.getStatusCode().value());
        verify(repo, times(1)).save(any(Medicao.class));
    }

    @Test
    @DisplayName("RN-67 e RN-23: reenvio da mesma leitura não grava de novo e responde 200 com a gravada")
    void reenvioNaoDuplica() {
        Medicao gravada = Medicao.leitura("evt-1", "leitura.temperatura", "no-colmeia-A1-07", "colmeia:A1-07", T0, 1, 34.8);
        MedicaoRepository repo = mock(MedicaoRepository.class);
        when(repo.findByEventId("evt-1")).thenReturn(Optional.of(gravada));

        ResponseEntity<Medicao> resposta = new MedicaoController(repo)
                .receber(Medicao.leitura("evt-1", "leitura.temperatura", "no-colmeia-A1-07", "colmeia:A1-07", T0, 1, 34.8));

        assertEquals(200, resposta.getStatusCode().value());
        assertSame(gravada, resposta.getBody());
        verify(repo, never()).save(any(Medicao.class));
    }

    @Test
    @DisplayName("RN-67: se o índice único barrar um reenvio simultâneo, a resposta é 200, sem erro")
    void reenvioSimultaneo() {
        Medicao gravada = Medicao.leitura("evt-1", "leitura.umidade", "no-colmeia-A1-07", "colmeia:A1-07", T0, 1, 62.0);
        MedicaoRepository repo = mock(MedicaoRepository.class);
        when(repo.findByEventId("evt-1")).thenReturn(Optional.empty(), Optional.of(gravada));
        when(repo.save(any(Medicao.class))).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

        ResponseEntity<Medicao> resposta = new MedicaoController(repo)
                .receber(Medicao.leitura("evt-1", "leitura.umidade", "no-colmeia-A1-07", "colmeia:A1-07", T0, 1, 62.0));

        assertEquals(200, resposta.getStatusCode().value());
        assertSame(gravada, resposta.getBody());
    }

    @Test
    @DisplayName("POST de ocorrência responde 201 com o alerta ativo")
    void criarOcorrencia() {
        OcorrenciaRepository repo = mock(OcorrenciaRepository.class);
        when(repo.save(any(Ocorrencia.class))).thenAnswer(chamada -> chamada.getArgument(0));
        Ocorrencia nova = Ocorrencia.nova("apiario-serra", TipoOcorrencia.TOMBAMENTO_POSSIVEL, Prioridade.ALTA, T0,
                "Colmeia B2-03 inclinada 22° por 10 s");

        ResponseEntity<Ocorrencia> resposta = new OcorrenciaController(repo).criar(nova);

        assertEquals(201, resposta.getStatusCode().value());
        assertEquals(EstadoAlerta.ATIVO, resposta.getBody().getEstado());
    }

    @Test
    @DisplayName("\"Estou ciente\" muda o estado e grava; alerta inexistente gera 404")
    void reconhecer() {
        Ocorrencia alerta = Ocorrencia.nova("apiario-serra", TipoOcorrencia.OCORRENCIA_SUSPEITA, Prioridade.ALTA, T0, null);
        OcorrenciaRepository repo = mock(OcorrenciaRepository.class);
        when(repo.findByUuid(alerta.getUuid())).thenReturn(Optional.of(alerta));
        when(repo.findByUuid("nao-existe")).thenReturn(Optional.empty());
        when(repo.save(any(Ocorrencia.class))).thenAnswer(chamada -> chamada.getArgument(0));
        OcorrenciaController controller = new OcorrenciaController(repo);

        Ocorrencia resultado = controller.reconhecer(alerta.getUuid(), Map.of("usuario", "usuario-antonio"));

        assertEquals(EstadoAlerta.RECONHECIDO, resultado.getEstado());
        assertThrows(NoSuchElementException.class, () -> controller.reconhecer("nao-existe", Map.of("usuario", "x")));
        assertEquals(409, new TratamentoDeErros().conflito(new IllegalStateException("já resolvido")).getStatusCode().value());
    }
}
