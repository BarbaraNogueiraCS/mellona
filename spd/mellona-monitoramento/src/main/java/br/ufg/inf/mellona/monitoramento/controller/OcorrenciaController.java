package br.ufg.inf.mellona.monitoramento.controller;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufg.inf.mellona.monitoramento.model.Dominios.EstadoAlerta;
import br.ufg.inf.mellona.monitoramento.model.Ocorrencia;
import br.ufg.inf.mellona.monitoramento.repository.OcorrenciaRepository;

/**
 * Recursos REST de ocorrências (alertas), em JSON.
 *
 * <pre>
 * GET  /api/v1/ocorrencias?apiario={uuid}[&amp;estado=ATIVO]   lista, mais recentes primeiro
 * GET  /api/v1/ocorrencias/{uuid}                             detalhe
 * POST /api/v1/ocorrencias                                    cria (201)
 * POST /api/v1/ocorrencias/{uuid}/reconhecimento              "Estou ciente"   {"usuario": "..."}
 * POST /api/v1/ocorrencias/{uuid}/resolucao                   "Encerrar alerta"
 * </pre>
 *
 * <p>Ocorrências não são excluídas: mudam de estado e ficam no histórico (RN-04, RN-47).</p>
 */
@RestController
@RequestMapping("/api/v1/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaRepository repositorio;
    private final Clock relogio;

    public OcorrenciaController(OcorrenciaRepository repositorio) {
        this.repositorio = repositorio;
        this.relogio = Clock.systemUTC();
    }

    @GetMapping
    public List<Ocorrencia> listar(@RequestParam("apiario") String apiarioUuid,
                                   @RequestParam(value = "estado", required = false) EstadoAlerta estado) {
        return estado == null
                ? repositorio.findByApiarioUuidOrderByInstanteDesc(apiarioUuid)
                : repositorio.findByApiarioUuidAndEstadoOrderByInstanteDesc(apiarioUuid, estado);
    }

    @GetMapping("/{uuid}")
    public Ocorrencia detalhar(@PathVariable("uuid") String uuid) {
        return buscar(uuid);
    }

    @PostMapping
    public ResponseEntity<Ocorrencia> criar(@RequestBody Ocorrencia nova) {
        nova.prepararNova(Instant.now(relogio));
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nova));
    }

    @PostMapping("/{uuid}/reconhecimento")
    public Ocorrencia reconhecer(@PathVariable("uuid") String uuid, @RequestBody Map<String, String> corpo) {
        Ocorrencia ocorrencia = buscar(uuid);
        ocorrencia.reconhecer(corpo.get("usuario"), Instant.now(relogio));
        return repositorio.save(ocorrencia);
    }

    @PostMapping("/{uuid}/resolucao")
    public Ocorrencia resolver(@PathVariable("uuid") String uuid) {
        Ocorrencia ocorrencia = buscar(uuid);
        ocorrencia.resolver(Instant.now(relogio));
        return repositorio.save(ocorrencia);
    }

    private Ocorrencia buscar(String uuid) {
        return repositorio.findByUuid(uuid)
                .orElseThrow(() -> new NoSuchElementException("Ocorrência " + uuid + " não encontrada."));
    }
}
