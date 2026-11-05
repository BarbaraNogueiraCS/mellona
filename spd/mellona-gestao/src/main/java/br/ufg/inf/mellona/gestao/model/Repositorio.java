package br.ufg.inf.mellona.gestao.model;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.table.TableUtils;

/**
 * Repositório genérico de uma entidade, sobre o DAO do ORMLite (padrão Repository).
 *
 * <p>Segue a classe {@code Repositorio<T, ID>} do tutorial JavaFX CRUD da disciplina, com três
 * diferenças: cada repositório guarda a própria base (sem campo estático compartilhado),
 * as falhas viram {@link PersistenciaException} em vez de serem só impressas, e as
 * entidades derivadas de {@link EntidadeBase} recebem os instantes de criação e alteração.</p>
 *
 * @param <T>  tipo da entidade
 * @param <ID> tipo da chave interna
 */
public class Repositorio<T, ID> {

    private final Database database;
    private final Class<T> classe;
    private final Dao<T, ID> dao;

    /**
     * Cria o DAO e a tabela da entidade, se ainda não existir.
     *
     * @param database base da gestão
     * @param classe   classe anotada com {@code @DatabaseTable}
     */
    public Repositorio(Database database, Class<T> classe) {
        this.database = database;
        this.classe = classe;
        try {
            this.dao = DaoManager.createDao(database.getConnection(), classe);
            TableUtils.createTableIfNotExists(database.getConnection(), classe);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível preparar a tabela de " + classe.getSimpleName(), e);
        }
    }

    /**
     * Grava um objeto novo.
     *
     * @param entidade objeto transiente
     * @return o mesmo objeto, agora com a chave interna preenchida
     */
    public T create(T entidade) {
        marcar(entidade);
        try {
            if (dao.create(entidade) != 1) {
                throw new SQLException("nenhuma linha gravada");
            }
            return entidade;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível gravar " + classe.getSimpleName(), e);
        }
    }

    /**
     * Grava as alterações de um objeto já persistido.
     *
     * @param entidade objeto carregado ou criado por este repositório
     * @return o mesmo objeto
     */
    public T update(T entidade) {
        marcar(entidade);
        try {
            dao.update(entidade);
            return entidade;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível alterar " + classe.getSimpleName(), e);
        }
    }

    /**
     * Exclui fisicamente. Os cadastros do Mellona não usam esta operação: registros com
     * histórico são inativados (ERS, RN-04 e 11.10). Ela fica para dados de teste e tabelas
     * sem histórico.
     *
     * @param entidade objeto persistido
     */
    public void delete(T entidade) {
        try {
            dao.delete(entidade);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível excluir " + classe.getSimpleName(), e);
        }
    }

    /**
     * @param id chave interna
     * @return o objeto, se existir
     */
    public Optional<T> loadFromId(ID id) {
        try {
            return Optional.ofNullable(dao.queryForId(id));
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao consultar " + classe.getSimpleName(), e);
        }
    }

    /**
     * Busca pelo identificador universal, o mesmo em todas as bases (RN-73).
     *
     * @param uuid identificador universal
     * @return o objeto, se existir
     */
    public Optional<T> loadFromUuid(UUID uuid) {
        return loadWhere("uuid", uuid).stream().findFirst();
    }

    /** @return todos os objetos da tabela */
    public List<T> loadAll() {
        try {
            return dao.queryForAll();
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao consultar " + classe.getSimpleName(), e);
        }
    }

    /**
     * Consulta por igualdade em uma coluna.
     *
     * @param coluna nome da coluna (para chaves estrangeiras, {@code <campo>_id})
     * @param valor  valor procurado; para chave estrangeira, o próprio objeto relacionado
     * @return objetos encontrados
     */
    public List<T> loadWhere(String coluna, Object valor) {
        try {
            return dao.queryForEq(coluna, valor);
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao consultar " + classe.getSimpleName(), e);
        }
    }

    /** @return quantidade de linhas da tabela */
    public long count() {
        try {
            return dao.countOf();
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao contar " + classe.getSimpleName(), e);
        }
    }

    /**
     * Executa várias gravações como uma unidade: ou todas são confirmadas, ou nenhuma.
     *
     * @param trabalho operações a executar
     * @param <R>      tipo do resultado
     * @return resultado do trabalho
     */
    public <R> R emTransacao(Callable<R> trabalho) {
        try {
            return TransactionManager.callInTransaction(database.getConnection(), trabalho);
        } catch (RegraDeNegocioException e) {
            throw e;
        } catch (SQLException e) {
            // o ORMLite embrulha exceções não SQL lançadas dentro da transação
            if (e.getCause() instanceof RegraDeNegocioException regra) {
                throw regra;
            }
            if (e.getCause() instanceof PersistenciaException persistencia) {
                throw persistencia;
            }
            throw new PersistenciaException("Transação desfeita em " + classe.getSimpleName(), e);
        }
    }

    /** @return DAO do ORMLite, para consultas que o repositório não oferece */
    public Dao<T, ID> getDao() {
        return dao;
    }

    private void marcar(T entidade) {
        if (entidade instanceof EntidadeBase base) {
            base.marcarAlteracao(Instant.now());
        }
    }
}
