package br.ufg.inf.mellona.gestao.model;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Área de criação com colmeias, sensor de presença e central (ERS, 13.1).
 *
 * <p>Relações:</p>
 * <ul>
 *   <li>1:1 com {@link ConfiguracaoSeguranca}, criada junto com o apiário;</li>
 *   <li>1:N com {@link Colmeia}, navegável por {@link #getColmeias()};</li>
 *   <li>N:N com {@link Usuario}, por {@link AcessoApiario};</li>
 *   <li>1:N com {@link Visita} e com {@link AssociacaoDispositivo}.</li>
 * </ul>
 *
 * <p>A descrição do local é informação sensível (RNF-16, risco R3): não aparece em
 * {@link #toString()} e não deve ser enviada nos eventos.</p>
 */
@DatabaseTable(tableName = "apiario")
public class Apiario extends EntidadeBase {

    /** Fuso usado quando nenhum é informado. */
    public static final String FUSO_PADRAO = "America/Sao_Paulo";

    @DatabaseField(canBeNull = false)
    private String nome;

    @DatabaseField
    private String descricaoLocal;

    @DatabaseField(canBeNull = false)
    private String fusoHorario = FUSO_PADRAO;

    @DatabaseField(canBeNull = false)
    private boolean ativo = true;

    @ForeignCollectionField(eager = false)
    private ForeignCollection<Colmeia> colmeias;

    /** Construtor exigido pelo ORMLite. */
    Apiario() {
    }

    /**
     * @param nome           nome pelo qual o apicultor reconhece o apiário, como "Apiário Serra"
     * @param descricaoLocal referência de localização, de acesso restrito
     */
    public Apiario(String nome, String descricaoLocal) {
        setNome(nome);
        setDescricaoLocal(descricaoLocal);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Validacao.obrigatorio(nome, "o nome do apiário");
    }

    public String getDescricaoLocal() {
        return descricaoLocal;
    }

    public void setDescricaoLocal(String descricaoLocal) {
        this.descricaoLocal = descricaoLocal == null || descricaoLocal.isBlank() ? null : descricaoLocal.strip();
    }

    public String getFusoHorario() {
        return fusoHorario;
    }

    /**
     * @param fusoHorario identificador IANA, como {@code America/Sao_Paulo}; define o período
     *                    do dia na central do apiário (RN-60)
     */
    public void setFusoHorario(String fusoHorario) {
        String valor = Validacao.obrigatorio(fusoHorario, "o fuso horário do apiário");
        try {
            ZoneId.of(valor);
        } catch (DateTimeException e) {
            throw new RegraDeNegocioException("RN-60", "O fuso \"" + valor + "\" não existe. Use um nome como America/Sao_Paulo.");
        }
        this.fusoHorario = valor;
    }

    public boolean isAtivo() {
        return ativo;
    }

    /** Usado por {@link CadastroApiarios}, que confere antes se não há colmeias ativas. */
    void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /**
     * Colmeias do apiário, ativas e inativas, lidas da base (relação 1:N).
     *
     * @return cópia da coleção; vazia se o apiário ainda não foi carregado da base
     */
    public List<Colmeia> getColmeias() {
        return colmeias == null ? List.of() : new ArrayList<>(colmeias);
    }

    @Override
    public String toString() {
        return nome;
    }
}
