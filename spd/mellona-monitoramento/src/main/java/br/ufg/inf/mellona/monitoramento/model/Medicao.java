package br.ufg.inf.mellona.monitoramento.model;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import br.ufg.inf.mellona.monitoramento.model.Dominios.Qualidade;

/**
 * Leitura de temperatura ou umidade de uma colmeia (ERS, 13.1: Medição).
 *
 * <p>O documento guarda o evento como a central do apiário o envia: os nomes dos campos são os do
 * contrato de eventos combinado com SSU (ERS, 13.4.1), em inglês, para que o JSON recebido e o
 * documento gravado sejam o mesmo objeto. O índice único em {@code eventId} faz o MongoDB recusar
 * a mesma leitura duas vezes quando a central reenvia o que guardou durante uma queda de
 * internet (RN-23, RN-67).</p>
 */
@Document(collection = "medicoes")
@CompoundIndex(name = "entidade_tempo", def = "{'entityId': 1, 'eventTime': -1}")
public class Medicao {

    /** PAR-04: idade máxima de uma medição apresentada como atual. */
    public static final Duration IDADE_MAXIMA = Duration.ofSeconds(180);

    @Id
    private String id;

    private int schemaVersion = 1;

    @Indexed(unique = true)
    private String eventId;

    /** {@code leitura.temperatura} ou {@code leitura.umidade}. */
    private String eventType;
    private String deviceId;
    /** Entidade observada, como {@code colmeia:A1-07}. */
    private String entityId;
    /** Instante de ocorrência na origem (RN-65). */
    private Instant eventTime;
    private String timeSource;
    private long sequence;
    private Double value;
    private String unit;
    private Qualidade quality;
    /** Atribuído no recebimento; não vem da origem. */
    private Instant receivedAt;

    /** Construtor usado pelo Spring Data e pelo Jackson. */
    protected Medicao() {
    }

    /**
     * Cria uma leitura já avaliada, para uso em testes e na ingestão.
     *
     * @param eventId   identificador único do evento
     * @param eventType {@code leitura.temperatura} ou {@code leitura.umidade}
     * @param deviceId  dispositivo de origem
     * @param entityId  entidade observada
     * @param eventTime instante de ocorrência
     * @param sequence  sequência por dispositivo
     * @param value     valor lido; {@code null} se o sensor não leu
     * @return leitura com unidade e qualidade definidas
     */
    public static Medicao leitura(String eventId, String eventType, String deviceId, String entityId,
                                  Instant eventTime, long sequence, Double value) {
        Medicao m = new Medicao();
        m.eventId = eventId;
        m.eventType = eventType;
        m.deviceId = deviceId;
        m.entityId = entityId;
        m.eventTime = eventTime;
        m.timeSource = "RTC";
        m.sequence = sequence;
        m.value = value;
        m.unit = m.unidadeEsperada();
        m.quality = m.avaliar();
        return m;
    }

    /**
     * Prepara uma leitura recebida pela API: confere os campos obrigatórios, registra o
     * instante de recebimento e reavalia a qualidade, que não é aceita da origem às cegas.
     *
     * @param agora instante do recebimento
     */
    public void receber(Instant agora) {
        if (eventId == null || eventId.isBlank() || deviceId == null || entityId == null || eventTime == null) {
            throw new IllegalArgumentException("Leitura sem eventId, deviceId, entityId ou eventTime.");
        }
        unidadeEsperada(); // recusa tipos de evento que não são leituras
        if (unit == null) {
            unit = unidadeEsperada();
        }
        receivedAt = agora;
        quality = avaliar();
    }

    /**
     * RN-07: leituras ausentes ou fora da faixa válida são inválidas e nunca aparecem como normais.
     * Faixas: temperatura de −10 °C a 60 °C (PAR-05); umidade de 0 % a 100 % (PAR-06).
     *
     * @return {@link Qualidade#VALIDO} ou {@link Qualidade#INVALIDO}
     */
    Qualidade avaliar() {
        if (value == null || value.isNaN()) {
            return Qualidade.INVALIDO;
        }
        boolean dentro = switch (eventType) {
            case "leitura.temperatura" -> value >= -10.0 && value <= 60.0;
            case "leitura.umidade" -> value >= 0.0 && value <= 100.0;
            default -> false;
        };
        return dentro ? Qualidade.VALIDO : Qualidade.INVALIDO;
    }

    /**
     * Qualidade para exibição: uma leitura válida com mais de 180 s (PAR-04) aparece como
     * desatualizada, para que um sensor mudo não pareça "tudo bem" (RN-22).
     *
     * @param agora instante da consulta
     * @return qualidade a apresentar
     */
    public Qualidade qualidadeEm(Instant agora) {
        if (quality == Qualidade.VALIDO && Duration.between(eventTime, agora).compareTo(IDADE_MAXIMA) > 0) {
            return Qualidade.DESATUALIZADO;
        }
        return quality;
    }

    private String unidadeEsperada() {
        if ("leitura.temperatura".equals(eventType)) {
            return "C";
        }
        if ("leitura.umidade".equals(eventType)) {
            return "%RH";
        }
        throw new IllegalArgumentException("Tipo de leitura desconhecido: " + eventType);
    }

    public String getId() { return id; }
    public int getSchemaVersion() { return schemaVersion; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getDeviceId() { return deviceId; }
    public String getEntityId() { return entityId; }
    public Instant getEventTime() { return eventTime; }
    public String getTimeSource() { return timeSource; }
    public long getSequence() { return sequence; }
    public Double getValue() { return value; }
    public String getUnit() { return unit; }
    public Qualidade getQuality() { return quality; }
    public Instant getReceivedAt() { return receivedAt; }
}
