package br.ufg.inf.mellona.gestao.model;

import java.util.List;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Categoria de atividade do Apêndice A do ERS: CM-01 a CM-07 (manutenção física) e
 * IB-01 a IB-07 (manejo e intervenções biológicas). É dado de referência: as 14 categorias
 * são gravadas por {@link Repositorios#garantirCategorias()} e não são alteradas pelo usuário.
 */
@DatabaseTable(tableName = "categoria_atividade")
public class CategoriaAtividade extends EntidadeBase {

    @DatabaseField(canBeNull = false, unique = true)
    private String codigo;

    @DatabaseField(canBeNull = false, dataType = DataType.ENUM_NAME)
    private GrupoAtividade grupo;

    @DatabaseField(canBeNull = false)
    private String nome;

    /** CM-07, "Outros", só é aceita com justificativa (Apêndice A). */
    @DatabaseField(canBeNull = false)
    private boolean exigeJustificativa;

    /** Construtor exigido pelo ORMLite. */
    CategoriaAtividade() {
    }

    CategoriaAtividade(String codigo, GrupoAtividade grupo, String nome, boolean exigeJustificativa) {
        this.codigo = codigo;
        this.grupo = grupo;
        this.nome = nome;
        this.exigeJustificativa = exigeJustificativa;
    }

    /** @return as 14 categorias do Apêndice A, na ordem do ERS */
    public static List<CategoriaAtividade> padrao() {
        return List.of(
                new CategoriaAtividade("CM-01", GrupoAtividade.CM, "Estrutura da caixa", false),
                new CategoriaAtividade("CM-02", GrupoAtividade.CM, "Cavaletes e suportes", false),
                new CategoriaAtividade("CM-03", GrupoAtividade.CM, "Quadros e favos", false),
                new CategoriaAtividade("CM-04", GrupoAtividade.CM, "Higiene e conservação", false),
                new CategoriaAtividade("CM-05", GrupoAtividade.CM, "Proteção contra pragas", false),
                new CategoriaAtividade("CM-06", GrupoAtividade.CM, "Equipamentos de monitoramento", false),
                new CategoriaAtividade("CM-07", GrupoAtividade.CM, "Outros", true),
                new CategoriaAtividade("IB-01", GrupoAtividade.IB, "Manejo da rainha", false),
                new CategoriaAtividade("IB-02", GrupoAtividade.IB, "Manejo populacional e das crias", false),
                new CategoriaAtividade("IB-03", GrupoAtividade.IB, "Reprodução e enxameação", false),
                new CategoriaAtividade("IB-04", GrupoAtividade.IB, "Manejo sanitário", false),
                new CategoriaAtividade("IB-05", GrupoAtividade.IB, "Manejo nutricional", false),
                new CategoriaAtividade("IB-06", GrupoAtividade.IB, "Manejo comportamental", false),
                new CategoriaAtividade("IB-07", GrupoAtividade.IB, "Manejo de adaptação ambiental", false));
    }

    public String getCodigo() {
        return codigo;
    }

    public GrupoAtividade getGrupo() {
        return grupo;
    }

    public String getNome() {
        return nome;
    }

    public boolean isExigeJustificativa() {
        return exigeJustificativa;
    }

    /** O código sempre vem com o nome na interface (guia de estilo, 2.6). */
    @Override
    public String toString() {
        return codigo + " — " + nome;
    }
}
