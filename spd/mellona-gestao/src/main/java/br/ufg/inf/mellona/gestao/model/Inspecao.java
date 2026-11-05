package br.ufg.inf.mellona.gestao.model;

import java.time.Instant;
import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Registro do que se viu dentro de uma colmeia (ERS, 13.1; HU-19). Na interface, "Revisão".
 *
 * <p>RN-24: identifica colmeia, data e responsável. RN-27: o registro descreve o que foi
 * observado e não constitui diagnóstico sanitário.</p>
 */
@DatabaseTable(tableName = "inspecao")
public class Inspecao extends EntidadeBase {

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Colmeia colmeia;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Usuario responsavel;

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date data;

    @DatabaseField(dataType = DataType.ENUM_NAME)
    private SituacaoRainha rainha;

    /** Avaliação das crias (ovos, larvas e pupas), como "Boas". */
    @DatabaseField
    private String crias;

    @DatabaseField
    private String condicaoColonia;

    @DatabaseField
    private String observacoes;

    /** Construtor exigido pelo ORMLite. */
    Inspecao() {
    }

    /** Use {@link RegistroDeCampo#registrarInspecao}, que confere a colmeia e o acesso. */
    Inspecao(Colmeia colmeia, Usuario responsavel, Instant data) {
        this.colmeia = Validacao.obrigatorio(colmeia, "a colmeia revisada", "RN-24");
        this.responsavel = Validacao.obrigatorio(responsavel, "o responsável pela revisão", "RN-24");
        this.data = Date.from(Validacao.obrigatorio(data, "a data da revisão", "RN-24"));
    }

    public Colmeia getColmeia() {
        return colmeia;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public Instant getData() {
        return data.toInstant();
    }

    public SituacaoRainha getRainha() {
        return rainha;
    }

    public void setRainha(SituacaoRainha rainha) {
        this.rainha = rainha;
    }

    public String getCrias() {
        return crias;
    }

    public void setCrias(String crias) {
        this.crias = crias;
    }

    public String getCondicaoColonia() {
        return condicaoColonia;
    }

    public void setCondicaoColonia(String condicaoColonia) {
        this.condicaoColonia = condicaoColonia;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
