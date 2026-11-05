package br.ufg.inf.mellona.integrador;

import java.util.Map;
import java.util.Set;

/**
 * Quais entidades são compartilhadas e em que sentido são propagadas (ERS, RN-74).
 *
 * <p>Medições, detecções e acionamentos não aparecem aqui: ficam só na base de monitoramento e
 * são consultados pela API (ERS, 11.10).</p>
 */
public final class RegrasDePropagacao {

    /** Canal Redis Pub/Sub das operações (equivale ao {@code crud-channel} do tutorial). */
    public static final String CANAL = "mellona:crud";

    /**
     * Lista Redis que retém cada operação até o integrador processá-la. O Pub/Sub entrega só a
     * quem está ouvindo naquele instante; a lista cobre o integrador fora do ar (RES-03, RNF-22).
     */
    public static final String FILA = "mellona:crud:fila";

    /** Lista Redis de operações que falharam no destino e aguardam reprocessamento (RNF-22). */
    public static final String FILA_DE_REPROCESSAMENTO = "mellona:crud:pendentes";

    private static final Map<String, Set<Base>> ORIGENS_PERMITIDAS = Map.of(
            "Apiario", Set.of(Base.GESTAO),
            "Colmeia", Set.of(Base.GESTAO),
            "Dispositivo", Set.of(Base.GESTAO),
            "AssociacaoDispositivo", Set.of(Base.GESTAO),
            "ConfiguracaoSeguranca", Set.of(Base.GESTAO),
            "AcessoApiario", Set.of(Base.GESTAO),
            "Ocorrencia", Set.of(Base.MONITORAMENTO),
            "Visita", Set.of(Base.GESTAO, Base.MONITORAMENTO),
            "AutorizacaoManutencao", Set.of(Base.GESTAO, Base.MONITORAMENTO));

    private RegrasDePropagacao() {
    }

    /**
     * @param entidade nome da entidade
     * @param origem   base onde a operação aconteceu
     * @return {@code true} se a entidade é compartilhada e pode partir dessa base
     */
    public static boolean permitida(String entidade, Base origem) {
        return ORIGENS_PERMITIDAS.getOrDefault(entidade, Set.of()).contains(origem);
    }

    /** @return nomes das entidades compartilhadas */
    public static Set<String> entidadesCompartilhadas() {
        return ORIGENS_PERMITIDAS.keySet();
    }
}
