package br.ufg.inf.mellona.gestao.model;

import java.time.Instant;
import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Vínculo de um usuário a um apiário, com papel e período (ERS, 13.1).
 *
 * <p>É a tabela associativa da relação N:N entre {@link Usuario} e {@link Apiario}: Carla, técnica
 * de cooperativa, acompanha vários apiários, e um apiário tem apicultor, administrador e técnico.
 * O acesso decide quem vê os dados do apiário (RNF-16) e quem pode registrar visitas.</p>
 */
@DatabaseTable(tableName = "acesso_apiario")
public class AcessoApiario extends EntidadeBase {

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Usuario usuario;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Apiario apiario;

    @DatabaseField(canBeNull = false, dataType = DataType.ENUM_NAME)
    private Papel papel;

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date inicio;

    @DatabaseField(dataType = DataType.DATE_LONG)
    private Date fim;

    /** Construtor exigido pelo ORMLite. */
    AcessoApiario() {
    }

    /** Use {@link CadastroAcessos#conceder}, que impede dois acessos ativos iguais. */
    AcessoApiario(Usuario usuario, Apiario apiario, Papel papel, Instant inicio) {
        this.usuario = usuario;
        this.apiario = apiario;
        this.papel = papel;
        this.inicio = Date.from(inicio);
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Apiario getApiario() {
        return apiario;
    }

    public Papel getPapel() {
        return papel;
    }

    public Instant getInicio() {
        return inicio.toInstant();
    }

    /** @return fim do acesso, ou {@code null} se ainda vale */
    public Instant getFim() {
        return fim == null ? null : fim.toInstant();
    }

    /** @return {@code true} enquanto o acesso não foi revogado */
    public boolean isAtivo() {
        return fim == null;
    }

    /** Usado por {@link CadastroAcessos#revogar}. */
    void encerrar(Instant instante) {
        this.fim = Date.from(instante);
    }
}
