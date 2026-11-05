package br.ufg.inf.mellona.integrador;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Decide se uma operação recebida deve ser aplicada no destino.
 *
 * <ol>
 *   <li><b>Origem</b> (RN-74): operação vinda da própria base não é reaplicada.</li>
 *   <li><b>Idempotência</b> (RN-74): uma operação já aplicada é ignorada se chegar de novo,
 *       o que acontece no reprocessamento depois de uma falha.</li>
 *   <li><b>Conflito</b> (RN-75): se o registro já foi alterado no destino depois do instante da
 *       operação, prevalece a alteração mais recente e a operação descartada fica registrada para
 *       auditoria. Em empate de instante, vence a maior {@code operacaoId}, para que as duas bases
 *       cheguem à mesma decisão.</li>
 * </ol>
 *
 * <p>Nesta etapa o controle é mantido em memória, o que basta para os testes. Na Etapa 2 os
 * identificadores aplicados e os instantes passam a ser lidos de cada base (tabela no SQLite,
 * coleção no MongoDB), para sobreviver a reinícios.</p>
 */
public class ControleDeAplicacao {

    /** Resultado da decisão. */
    public enum Decisao { APLICAR, IGNORAR_MESMA_ORIGEM, IGNORAR_REPETIDA, DESCARTAR_MAIS_ANTIGA }

    /** Registro de uma operação descartada por conflito (RN-75). */
    public record Descarte(OperacaoSincronizacao descartada, Instant versaoNoDestino) {
    }

    private final Base destino;
    private final Set<String> aplicadas = new HashSet<>();
    private final Map<UUID, Ultima> ultimaPorRegistro = new HashMap<>();
    private final List<Descarte> descartes = new ArrayList<>();

    private record Ultima(Instant instante, String operacaoId) {
    }

    /** @param destino base em que este controle aplica as operações */
    public ControleDeAplicacao(Base destino) {
        this.destino = destino;
    }

    /**
     * Decide e, se for o caso, marca a operação como aplicada.
     *
     * @param op operação recebida
     * @return a decisão tomada
     */
    public Decisao decidir(OperacaoSincronizacao op) {
        if (!op.deveSerAplicadaEm(destino)) {
            return Decisao.IGNORAR_MESMA_ORIGEM;
        }
        if (aplicadas.contains(op.getOperacaoId())) {
            return Decisao.IGNORAR_REPETIDA;
        }
        Ultima atual = ultimaPorRegistro.get(op.getEntidadeUuid());
        if (atual != null && maisRecente(atual, op)) {
            descartes.add(new Descarte(op, atual.instante()));
            aplicadas.add(op.getOperacaoId());
            return Decisao.DESCARTAR_MAIS_ANTIGA;
        }
        aplicadas.add(op.getOperacaoId());
        ultimaPorRegistro.put(op.getEntidadeUuid(), new Ultima(op.getInstante(), op.getOperacaoId()));
        return Decisao.APLICAR;
    }

    /**
     * Informa uma alteração feita diretamente no destino, para que operações mais antigas do
     * mesmo registro sejam descartadas.
     *
     * @param entidadeUuid registro alterado
     * @param instante     instante da alteração local
     */
    public void registrarAlteracaoLocal(UUID entidadeUuid, Instant instante) {
        ultimaPorRegistro.merge(entidadeUuid, new Ultima(instante, ""),
                (antiga, nova) -> nova.instante().isAfter(antiga.instante()) ? nova : antiga);
    }

    /** @return operações descartadas por conflito, para auditoria (RN-75) */
    public List<Descarte> getDescartes() {
        return List.copyOf(descartes);
    }

    /**
     * @param entidadeUuid registro
     * @return instante da versão vigente no destino, se conhecido
     */
    public Optional<Instant> versaoVigente(UUID entidadeUuid) {
        return Optional.ofNullable(ultimaPorRegistro.get(entidadeUuid)).map(Ultima::instante);
    }

    /**
     * @param entidadeUuid registro
     * @return identificador da operação cuja versão está vigente no destino, se conhecida
     */
    public Optional<String> operacaoVigente(UUID entidadeUuid) {
        return Optional.ofNullable(ultimaPorRegistro.get(entidadeUuid)).map(Ultima::operacaoId);
    }

    private static boolean maisRecente(Ultima atual, OperacaoSincronizacao op) {
        int comparacao = atual.instante().compareTo(op.getInstante());
        if (comparacao != 0) {
            return comparacao > 0;
        }
        return atual.operacaoId().compareTo(op.getOperacaoId()) > 0;
    }
}
