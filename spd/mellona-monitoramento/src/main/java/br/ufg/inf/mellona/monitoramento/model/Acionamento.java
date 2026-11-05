package br.ufg.inf.mellona.monitoramento.model;

import java.time.Instant;

import br.ufg.inf.mellona.monitoramento.model.Dominios.Recurso;

/**
 * Uso de um recurso de sinalização (som ou luz) numa ocorrência (ERS, 13.1).
 *
 * <p>No modelo relacional, Acionamento seria uma tabela com chave estrangeira para Ocorrência
 * (1:N). No MongoDB, os acionamentos são <b>documentos embutidos</b> no array
 * {@code acionamentos} da ocorrência: são sempre lidos junto com ela e não existem sozinhos,
 * então embutir evita uma segunda consulta.</p>
 *
 * @param recurso recurso acionado
 * @param inicio  início da sinalização
 * @param fim     fim da sinalização (PAR-11: 30 s por padrão)
 */
public record Acionamento(Recurso recurso, Instant inicio, Instant fim) {

    public Acionamento {
        if (recurso == null || inicio == null || fim == null) {
            throw new IllegalArgumentException("Acionamento exige recurso, início e fim.");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("O fim do acionamento não pode ser anterior ao início.");
        }
    }
}
