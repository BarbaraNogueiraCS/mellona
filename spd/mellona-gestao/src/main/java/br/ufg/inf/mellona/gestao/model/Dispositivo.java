package br.ufg.inf.mellona.gestao.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Equipamento que mede, detecta ou sinaliza (ERS, 13.1). Na interface, "Equipamento".
 *
 * <p>O identificador é o mesmo enviado pelo equipamento no campo {@code deviceId} dos eventos
 * (ERS, 13.4.1), como {@code no-colmeia-A1-07}. A ligação com apiários e colmeias é a
 * relação N:N com histórico em {@link AssociacaoDispositivo}.</p>
 */
@DatabaseTable(tableName = "dispositivo")
public class Dispositivo extends EntidadeBase {

    @DatabaseField(canBeNull = false, unique = true)
    private String identificador;

    @DatabaseField(canBeNull = false, dataType = DataType.ENUM_NAME)
    private TipoDispositivo tipo;

    @DatabaseField(canBeNull = false)
    private boolean ativo = true;

    /** Construtor exigido pelo ORMLite. */
    Dispositivo() {
    }

    /**
     * @param identificador identificador único, igual ao {@code deviceId} dos eventos
     * @param tipo          tipo do equipamento
     */
    public Dispositivo(String identificador, TipoDispositivo tipo) {
        this.identificador = Validacao.obrigatorio(identificador, "o identificador do equipamento");
        this.tipo = Validacao.obrigatorio(tipo, "o tipo do equipamento", "RN-03");
    }

    public String getIdentificador() {
        return identificador;
    }

    public TipoDispositivo getTipo() {
        return tipo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    /** Usado por {@link CadastroDispositivos#inativar}. */
    void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return identificador;
    }
}
