package br.ufg.inf.mellona.gestao.model;

/**
 * Reúne os repositórios de todas as entidades de uma base da gestão.
 *
 * <p>Equivale à classe {@code Repositorios} do tutorial JavaFX CRUD, mas é instanciada por base
 * em vez de usar constantes estáticas, o que permite aos testes usar um arquivo SQLite novo
 * em cada caso. Criar um {@code Repositorios} cria as tabelas que faltarem.</p>
 */
public class Repositorios {

    private final Database database;

    public final Repositorio<Usuario, Integer> usuarios;
    public final Repositorio<Apiario, Integer> apiarios;
    public final Repositorio<ConfiguracaoSeguranca, Integer> configuracoes;
    public final Repositorio<AcessoApiario, Integer> acessos;
    public final Repositorio<Colmeia, Integer> colmeias;
    public final Repositorio<Dispositivo, Integer> dispositivos;
    public final Repositorio<AssociacaoDispositivo, Integer> associacoes;
    public final Repositorio<CategoriaAtividade, Integer> categorias;
    public final Repositorio<Visita, Integer> visitas;
    public final Repositorio<Inspecao, Integer> inspecoes;

    /** @param database base da gestão */
    public Repositorios(Database database) {
        this.database = database;
        usuarios = new Repositorio<>(database, Usuario.class);
        apiarios = new Repositorio<>(database, Apiario.class);
        configuracoes = new Repositorio<>(database, ConfiguracaoSeguranca.class);
        acessos = new Repositorio<>(database, AcessoApiario.class);
        colmeias = new Repositorio<>(database, Colmeia.class);
        dispositivos = new Repositorio<>(database, Dispositivo.class);
        associacoes = new Repositorio<>(database, AssociacaoDispositivo.class);
        categorias = new Repositorio<>(database, CategoriaAtividade.class);
        visitas = new Repositorio<>(database, Visita.class);
        inspecoes = new Repositorio<>(database, Inspecao.class);
    }

    public Database getDatabase() {
        return database;
    }

    /** Grava as 14 categorias do Apêndice A, se a tabela estiver vazia. */
    public void garantirCategorias() {
        if (categorias.count() == 0) {
            categorias.emTransacao(() -> {
                CategoriaAtividade.padrao().forEach(categorias::create);
                return null;
            });
        }
    }

    /**
     * @param codigo código como {@code IB-01}
     * @return a categoria
     * @throws RegraDeNegocioException se o código não existir
     */
    public CategoriaAtividade categoria(String codigo) {
        return categorias.loadWhere("codigo", codigo).stream().findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("RN-51", "A categoria " + codigo + " não existe no Apêndice A."));
    }
}
