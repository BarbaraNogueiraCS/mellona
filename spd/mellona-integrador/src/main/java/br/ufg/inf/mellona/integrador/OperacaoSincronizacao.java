package br.ufg.inf.mellona.integrador;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Mensagem publicada no Redis a cada criação, alteração ou inativação de uma entidade
 * compartilhada (ERS, RN-74). É o contrato entre as aplicações e o integrador.
 *
 * <p>Corresponde à classe {@code CrudOperation} do tutorial "Integrador Redis I", com quatro
 * acréscimos exigidos pelo ERS:</p>
 * <ul>
 *   <li>{@code operacaoId}: identifica a operação, para aplicá-la uma única vez no destino (RN-74);</li>
 *   <li>{@code entidadeUuid}: o identificador universal do registro, igual nas duas bases (RN-73);</li>
 *   <li>{@code instante}: quando a alteração aconteceu na origem, para resolver conflitos (RN-75);</li>
 *   <li>{@code tipo} = {@link TipoOperacao#INATIVACAO} no lugar de DELETE: nada com histórico é
 *       apagado (RN-04, 11.10).</li>
 * </ul>
 *
 * <p>Exemplo do JSON publicado:</p>
 * <pre>{@code
 * {"schemaVersion":1,"operacaoId":"0f6d…","entidade":"Colmeia","entidadeUuid":"a3c1…",
 *  "tipo":"CRIACAO","origem":"GESTAO","instante":"2026-10-21T10:00:00Z",
 *  "dados":{"codigo":"A1-07","apiarioUuid":"7b2e…","ativa":true}}
 * }</pre>
 */
public final class OperacaoSincronizacao {

    /** Versão do formato, para evoluir o contrato sem quebrar quem já o usa. */
    public static final int VERSAO_DO_FORMATO = 1;

    private static final Gson GSON = new Gson();

    private int schemaVersion = VERSAO_DO_FORMATO;
    private String operacaoId;
    private String entidade;
    private String entidadeUuid;
    private TipoOperacao tipo;
    private Base origem;
    private String instante;
    private JsonObject dados;

    /** Usado pelo Gson. */
    private OperacaoSincronizacao() {
    }

    /**
     * Cria uma operação nova, com identificador próprio.
     *
     * @param entidade     nome da entidade compartilhada, como {@code Colmeia}
     * @param entidadeUuid identificador universal do registro
     * @param tipo         criação, alteração ou inativação
     * @param origem       base onde a operação aconteceu
     * @param instante     instante da alteração na origem
     * @param dados        estado do registro depois da operação, em JSON
     * @return operação validada
     * @throws IllegalArgumentException se faltar campo ou se a entidade não puder ser propagada a
     *                                  partir dessa origem
     */
    public static OperacaoSincronizacao nova(String entidade, UUID entidadeUuid, TipoOperacao tipo,
                                             Base origem, Instant instante, JsonObject dados) {
        OperacaoSincronizacao op = new OperacaoSincronizacao();
        op.operacaoId = UUID.randomUUID().toString();
        op.entidade = entidade;
        op.entidadeUuid = entidadeUuid == null ? null : entidadeUuid.toString();
        op.tipo = tipo;
        op.origem = origem;
        op.instante = instante == null ? null : instante.toString();
        op.dados = dados == null ? new JsonObject() : dados;
        op.validar();
        return op;
    }

    /** @return a operação em JSON, para {@code PUBLISH} no canal do Redis */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Lê e valida uma mensagem recebida do Redis.
     *
     * @param json texto recebido
     * @return operação validada
     * @throws IllegalArgumentException se o JSON for inválido ou incompleto
     */
    public static OperacaoSincronizacao fromJson(String json) {
        OperacaoSincronizacao op;
        try {
            op = GSON.fromJson(json, OperacaoSincronizacao.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Mensagem de sincronização não é um JSON válido.", e);
        }
        if (op == null) {
            throw new IllegalArgumentException("Mensagem de sincronização vazia.");
        }
        if (op.dados == null) {
            op.dados = new JsonObject();
        }
        op.validar();
        return op;
    }

    /**
     * RN-74: uma operação aplicada a partir de outra base não volta à base de origem.
     *
     * @param destino base que recebeu a mensagem
     * @return {@code true} se o destino deve aplicar a operação
     */
    public boolean deveSerAplicadaEm(Base destino) {
        return destino != origem;
    }

    private void validar() {
        if (schemaVersion != VERSAO_DO_FORMATO) {
            throw new IllegalArgumentException("Versão de formato não suportada: " + schemaVersion);
        }
        exigir(operacaoId, "operacaoId");
        exigir(entidade, "entidade");
        exigir(entidadeUuid, "entidadeUuid");
        exigir(tipo, "tipo");
        exigir(origem, "origem");
        exigir(instante, "instante");
        try {
            UUID.fromString(entidadeUuid);
            Instant.parse(instante);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new IllegalArgumentException("entidadeUuid ou instante em formato inválido.", e);
        }
        if (!RegrasDePropagacao.permitida(entidade, origem)) {
            throw new IllegalArgumentException("A entidade " + entidade + " não é propagada a partir da base " + origem
                    + " (ERS, RN-74).");
        }
    }

    private static void exigir(Object valor, String campo) {
        if (valor == null || (valor instanceof String s && s.isBlank())) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + campo);
        }
    }

    public int getSchemaVersion() { return schemaVersion; }
    public String getOperacaoId() { return operacaoId; }
    public String getEntidade() { return entidade; }
    public UUID getEntidadeUuid() { return UUID.fromString(entidadeUuid); }
    public TipoOperacao getTipo() { return tipo; }
    public Base getOrigem() { return origem; }
    public Instant getInstante() { return Instant.parse(instante); }
    /** @return cópia dos dados, para que o consumidor não altere a mensagem */
    public JsonObject getDados() { return dados.deepCopy(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof OperacaoSincronizacao outra && Objects.equals(operacaoId, outra.operacaoId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(operacaoId);
    }
}
