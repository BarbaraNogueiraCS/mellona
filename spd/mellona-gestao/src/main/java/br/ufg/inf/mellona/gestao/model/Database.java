package br.ufg.inf.mellona.gestao.model;

import java.sql.SQLException;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

/**
 * Conexão com a base relacional da gestão, um arquivo SQLite.
 *
 * <p>O SQLite não exige servidor: o driver JDBC cria e mantém o arquivo indicado.
 * Uma única conexão é compartilhada por todos os repositórios de uma mesma base,
 * o que evita gravações concorrentes no mesmo arquivo.</p>
 */
public class Database implements AutoCloseable {

    private final String url;
    private JdbcConnectionSource connection;

    /**
     * @param arquivo caminho do arquivo SQLite (por exemplo, {@code mellona-gestao.sqlite});
     *                é criado na primeira conexão, se não existir
     */
    public Database(String arquivo) {
        if (arquivo == null || arquivo.isBlank()) {
            throw new IllegalArgumentException("Informe o caminho do arquivo SQLite.");
        }
        this.url = "jdbc:sqlite:" + arquivo;
    }

    /**
     * Abre a conexão na primeira chamada e a reaproveita nas seguintes.
     *
     * @return fonte de conexão do ORMLite
     * @throws PersistenciaException se o arquivo não puder ser aberto
     */
    public synchronized ConnectionSource getConnection() {
        if (connection == null) {
            try {
                connection = new JdbcConnectionSource(url);
            } catch (SQLException e) {
                throw new PersistenciaException("Não foi possível abrir a base " + url, e);
            }
        }
        return connection;
    }

    /** Fecha a conexão, se estiver aberta. */
    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                throw new PersistenciaException("Falha ao fechar a base " + url, e);
            } finally {
                connection = null;
            }
        }
    }
}
