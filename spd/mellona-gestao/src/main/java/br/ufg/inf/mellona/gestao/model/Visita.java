package br.ufg.inf.mellona.gestao.model;

import java.time.Instant;
import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Ida registrada ao apiário, com motivo classificado (ERS, 13.1; HU-33).
 *
 * <ul>
 *   <li>RN-52: identifica apiário, responsável, motivo e data (todos obrigatórios).</li>
 *   <li>RN-51: o motivo é uma {@link CategoriaAtividade} do grupo CM ou IB.</li>
 *   <li>RN-55: registrar a visita não desliga a sirene; o modo visita é uma autorização própria,
 *       ligada à visita (entidade {@code AutorizacaoManutencao}, prevista para a Etapa 2).</li>
 * </ul>
 */
@DatabaseTable(tableName = "visita")
public class Visita extends EntidadeBase {

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Apiario apiario;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Usuario responsavel;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private CategoriaAtividade motivo;

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date data;

    @DatabaseField
    private String descricao;

    /** Construtor exigido pelo ORMLite. */
    Visita() {
    }

    /** Use {@link RegistroDeCampo#registrarVisita}, que confere acesso e justificativa. */
    Visita(Apiario apiario, Usuario responsavel, CategoriaAtividade motivo, Instant data, String descricao) {
        this.apiario = Validacao.obrigatorio(apiario, "o apiário da visita", "RN-52");
        this.responsavel = Validacao.obrigatorio(responsavel, "o responsável pela visita", "RN-52");
        this.motivo = Validacao.obrigatorio(motivo, "o motivo da visita", "RN-52");
        this.data = Date.from(Validacao.obrigatorio(data, "a data da visita", "RN-52"));
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
    }

    public Apiario getApiario() {
        return apiario;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public CategoriaAtividade getMotivo() {
        return motivo;
    }

    public Instant getData() {
        return data.toInstant();
    }

    public String getDescricao() {
        return descricao;
    }
}
