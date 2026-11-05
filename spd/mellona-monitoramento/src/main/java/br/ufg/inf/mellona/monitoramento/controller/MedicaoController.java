package br.ufg.inf.mellona.monitoramento.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufg.inf.mellona.monitoramento.model.Medicao;
import br.ufg.inf.mellona.monitoramento.repository.MedicaoRepository;

/**
 * Recursos REST de medições.
 *
 * <pre>
 * POST /api/v1/medicoes                                   recebe uma leitura (201), ou devolve a já gravada (200)
 * GET  /api/v1/medicoes?entidade=colmeia:A1-07&amp;de=...&amp;ate=...   histórico no período (ISO 8601)
 * </pre>
 *
 * <p>O POST é idempotente pelo {@code eventId} (RN-67): a central do apiário pode reenviar,
 * depois de uma queda de internet, leituras que já tinham chegado, sem criar duplicatas (RN-23).</p>
 */
@RestController
@RequestMapping("/api/v1/medicoes")
public class MedicaoController {

    private final MedicaoRepository repositorio;

    public MedicaoController(MedicaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @PostMapping
    public ResponseEntity<Medicao> receber(@RequestBody Medicao leitura) {
        leitura.receber(Instant.now());
        Optional<Medicao> existente = repositorio.findByEventId(leitura.getEventId());
        if (existente.isPresent()) {
            return ResponseEntity.ok(existente.get());
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(leitura));
        } catch (DuplicateKeyException e) {
            // dois reenvios simultâneos: o índice único de eventId barrou o segundo
            return ResponseEntity.ok(repositorio.findByEventId(leitura.getEventId()).orElseThrow());
        }
    }

    @GetMapping
    public List<Medicao> historico(@RequestParam("entidade") String entidade,
                                   @RequestParam("de") Instant de,
                                   @RequestParam("ate") Instant ate) {
        if (ate.isBefore(de)) {
            throw new IllegalArgumentException("O fim do período é anterior ao início.");
        }
        return repositorio.findByEntityIdAndEventTimeBetweenOrderByEventTimeAsc(entidade, de, ate);
    }
}
