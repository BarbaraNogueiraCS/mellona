package br.ufg.inf.mellona.gestao.model;

import java.time.LocalDate;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Caixa identificada individualmente (ERS, 13.1).
 *
 * <ul>
 *   <li>RN-01: o código, como {@code A1-07}, é único; a base tem restrição UNIQUE.</li>
 *   <li>RN-02: a colmeia pertence a um só apiário por vez; a chave estrangeira é obrigatória e
 *       {@link CadastroColmeias} só aceita apiários ativos.</li>
 * </ul>
 */
@DatabaseTable(tableName = "colmeia")
public class Colmeia extends EntidadeBase {

    @DatabaseField(canBeNull = false, unique = true)
    private String codigo;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Apiario apiario;

    /** Data no formato ISO (aaaa-mm-dd), que o SQLite ordena corretamente como texto. */
    @DatabaseField
    private String dataInstalacao;

    @DatabaseField(canBeNull = false)
    private boolean ativa = true;

    /** Construtor exigido pelo ORMLite. */
    Colmeia() {
    }

    /**
     * Use {@link CadastroColmeias#cadastrar} para gravar, que confere RN-01 e RN-02.
     *
     * @param codigo         identificador impresso na caixa
     * @param apiario        apiário onde a colmeia está
     * @param dataInstalacao data de instalação, opcional
     */
    public Colmeia(String codigo, Apiario apiario, LocalDate dataInstalacao) {
        this.codigo = normalizarCodigo(codigo);
        this.apiario = Validacao.obrigatorio(apiario, "o apiário da colmeia", "RN-02");
        this.dataInstalacao = dataInstalacao == null ? null : dataInstalacao.toString();
    }

    /**
     * Padroniza o código para comparação: sem espaços nas pontas e em maiúsculas.
     *
     * @param codigo código digitado
     * @return código padronizado
     */
    public static String normalizarCodigo(String codigo) {
        return Validacao.obrigatorio(codigo, "o código da colmeia").toUpperCase();
    }

    public String getCodigo() {
        return codigo;
    }

    public Apiario getApiario() {
        return apiario;
    }

    /** Usado por {@link CadastroColmeias#mover}. */
    void setApiario(Apiario apiario) {
        this.apiario = apiario;
    }

    public LocalDate getDataInstalacao() {
        return dataInstalacao == null ? null : LocalDate.parse(dataInstalacao);
    }

    public boolean isAtiva() {
        return ativa;
    }

    /** Usado por {@link CadastroColmeias#inativar}. */
    void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    @Override
    public String toString() {
        return "Colmeia " + codigo;
    }
}
