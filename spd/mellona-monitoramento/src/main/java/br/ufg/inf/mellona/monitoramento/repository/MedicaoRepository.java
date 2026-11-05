package br.ufg.inf.mellona.monitoramento.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.ufg.inf.mellona.monitoramento.model.Medicao;

/** Repositório ODM das medições. */
public interface MedicaoRepository extends MongoRepository<Medicao, String> {

    /** Usado para responder a um reenvio sem gravar de novo (RN-67). */
    Optional<Medicao> findByEventId(String eventId);

    /** Histórico de uma colmeia num período (HU-07), em ordem de ocorrência. */
    List<Medicao> findByEntityIdAndEventTimeBetweenOrderByEventTimeAsc(String entityId, Instant de, Instant ate);
}
