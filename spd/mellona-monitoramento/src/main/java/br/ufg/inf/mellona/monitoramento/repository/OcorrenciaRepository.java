package br.ufg.inf.mellona.monitoramento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.ufg.inf.mellona.monitoramento.model.Dominios.EstadoAlerta;
import br.ufg.inf.mellona.monitoramento.model.Ocorrencia;

/**
 * Repositório ODM das ocorrências. O Spring Data MongoDB gera a implementação a partir dos
 * nomes dos métodos; o índice composto de {@link Ocorrencia} atende às duas consultas por apiário.
 */
public interface OcorrenciaRepository extends MongoRepository<Ocorrencia, String> {

    Optional<Ocorrencia> findByUuid(String uuid);

    /** Alertas de um apiário, do mais recente ao mais antigo (central de alertas, HU-17). */
    List<Ocorrencia> findByApiarioUuidOrderByInstanteDesc(String apiarioUuid);

    /** Alertas de um apiário num estado, como os ativos que aparecem no Painel. */
    List<Ocorrencia> findByApiarioUuidAndEstadoOrderByInstanteDesc(String apiarioUuid, EstadoAlerta estado);
}
