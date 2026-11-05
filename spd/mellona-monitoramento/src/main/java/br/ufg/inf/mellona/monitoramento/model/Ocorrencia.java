package br.ufg.inf.mellona.monitoramento.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import br.ufg.inf.mellona.monitoramento.model.Dominios.EstadoAlerta;
import br.ufg.inf.mellona.monitoramento.model.Dominios.PeriodoDia;
import br.ufg.inf.mellona.monitoramento.model.Dominios.Prioridade;
import br.ufg.inf.mellona.monitoramento.model.Dominios.TipoOcorrencia;

/**
 * Alerta gerado pelo monitoramento (ERS, 13.1: Ocorrência). Na interface, "Alerta".
 *
 * <p>Documento da coleção {@code ocorrencias}. Apiário, colmeia e dispositivo são referenciados
 * pelo identificador universal (RN-73), e não por chave interna, porque esses cadastros nascem
 * na base relacional da gestão e chegam aqui pelo integrador. A ocorrência é propagada de volta
 * à gestão (RN-74), onde pode dar origem a uma manutenção (HU-30).</p>
 *
 * <p>RN-18: uma ocorrência suspeita não é apresentada como furto confirmado; o
 * {@link #getMotivoDecisao() motivo da decisão} explica o que foi observado (RNF-21).</p>
 */
@Document(collection = "ocorrencias")
@CompoundIndex(name = "apiario_estado_instante", def = "{'apiarioUuid': 1, 'estado': 1, 'instante': -1}")
public class Ocorrencia {

    @Id
    private String id;

    @Indexed(unique = true)
    private String uuid;

    private String apiarioUuid;
    private String colmeiaUuid;
    private String dispositivoId;

    private TipoOcorrencia tipo;
    private Prioridade prioridade;
    private EstadoAlerta estado;
    private Instant instante;
    private PeriodoDia periodoDia;
    private String motivoDecisao;

    /** {@code relatedEventIds} do evento {@code ocorrencia.suspeita}: as detecções correlacionadas. */
    private List<String> eventosRelacionados = new ArrayList<>();

    /** Relação 1:N modelada como documentos embutidos. */
    private List<Acionamento> acionamentos = new ArrayList<>();

    private String reconhecidoPor;
    private Instant reconhecidoEm;
    private Instant resolvidoEm;
    private Instant atualizadoEm;

    /** Construtor usado pelo Spring Data na leitura e pelo Jackson no corpo das requisições. */
    protected Ocorrencia() {
    }

    /**
     * Cria uma ocorrência ativa.
     *
     * @param apiarioUuid  apiário onde ocorreu
     * @param tipo         tipo da ocorrência
     * @param prioridade   prioridade
     * @param instante     instante de ocorrência na origem (RN-65)
     * @param motivo       explicação da decisão, como "presença e colmeia mexida em 18 s"
     * @return ocorrência no estado {@link EstadoAlerta#ATIVO}
     */
    public static Ocorrencia nova(String apiarioUuid, TipoOcorrencia tipo, Prioridade prioridade,
                                  Instant instante, String motivo) {
        Ocorrencia o = new Ocorrencia();
        o.uuid = UUID.randomUUID().toString();
        o.apiarioUuid = obrigatorio(apiarioUuid, "apiarioUuid");
        o.tipo = obrigatorio(tipo, "tipo");
        o.prioridade = obrigatorio(prioridade, "prioridade");
        o.instante = obrigatorio(instante, "instante");
        o.motivoDecisao = motivo;
        o.estado = EstadoAlerta.ATIVO;
        o.atualizadoEm = instante;
        return o;
    }

    /**
     * Completa uma ocorrência recebida no corpo de uma requisição: gera o uuid, se não veio,
     * e garante o estado inicial.
     *
     * @param agora instante do recebimento
     */
    public void prepararNova(Instant agora) {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        obrigatorio(apiarioUuid, "apiarioUuid");
        obrigatorio(tipo, "tipo");
        obrigatorio(prioridade, "prioridade");
        obrigatorio(instante, "instante");
        estado = EstadoAlerta.ATIVO;
        atualizadoEm = agora;
    }

    /**
     * "Estou ciente": a pessoa viu o alerta (RN-21). Só vale para alertas ativos.
     *
     * @param usuarioUuid quem reconheceu
     * @param agora       instante do reconhecimento
     * @throws IllegalStateException se o alerta não estiver ativo
     */
    public void reconhecer(String usuarioUuid, Instant agora) {
        if (estado != EstadoAlerta.ATIVO) {
            throw new IllegalStateException("Só um alerta ativo pode ser reconhecido; este está " + estado + ".");
        }
        reconhecidoPor = obrigatorio(usuarioUuid, "usuario");
        reconhecidoEm = agora;
        estado = EstadoAlerta.RECONHECIDO;
        atualizadoEm = agora;
    }

    /**
     * "Encerrar alerta": o problema foi tratado (RN-21). Resolvido é estado final.
     *
     * @param agora instante do encerramento
     * @throws IllegalStateException se o alerta já estiver resolvido
     */
    public void resolver(Instant agora) {
        if (estado == EstadoAlerta.RESOLVIDO) {
            throw new IllegalStateException("O alerta já está resolvido.");
        }
        resolvidoEm = agora;
        estado = EstadoAlerta.RESOLVIDO;
        atualizadoEm = agora;
    }

    /**
     * Registra o uso de sirene ou luz nesta ocorrência.
     *
     * @param acionamento recurso e período
     */
    public void registrarAcionamento(Acionamento acionamento) {
        acionamentos.add(acionamento);
    }

    private static <T> T obrigatorio(T valor, String campo) {
        if (valor == null || (valor instanceof String s && s.isBlank())) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + campo);
        }
        return valor;
    }

    public String getId() { return id; }
    public String getUuid() { return uuid; }
    public String getApiarioUuid() { return apiarioUuid; }
    public String getColmeiaUuid() { return colmeiaUuid; }
    public void setColmeiaUuid(String colmeiaUuid) { this.colmeiaUuid = colmeiaUuid; }
    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }
    public TipoOcorrencia getTipo() { return tipo; }
    public Prioridade getPrioridade() { return prioridade; }
    public EstadoAlerta getEstado() { return estado; }
    public Instant getInstante() { return instante; }
    public PeriodoDia getPeriodoDia() { return periodoDia; }
    public void setPeriodoDia(PeriodoDia periodoDia) { this.periodoDia = periodoDia; }
    public String getMotivoDecisao() { return motivoDecisao; }
    public List<String> getEventosRelacionados() { return eventosRelacionados; }
    public List<Acionamento> getAcionamentos() { return acionamentos; }
    public String getReconhecidoPor() { return reconhecidoPor; }
    public Instant getReconhecidoEm() { return reconhecidoEm; }
    public Instant getResolvidoEm() { return resolvidoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
