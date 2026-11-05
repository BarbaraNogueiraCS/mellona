package br.ufg.inf.mellona.gestao;

import java.io.IOException;

import br.ufg.inf.mellona.gestao.controller.ApiarioController;
import br.ufg.inf.mellona.gestao.model.DadosDeExemplo;
import br.ufg.inf.mellona.gestao.model.Database;
import br.ufg.inf.mellona.gestao.model.Repositorios;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Ponto de entrada da aplicação de gestão (I1).
 *
 * <p>Execute com {@code mvn -pl mellona-gestao javafx:run}. Duas variáveis de ambiente
 * ajustam a execução:</p>
 * <ul>
 *   <li>{@code MELLONA_DB}: caminho do arquivo SQLite; o padrão é {@code mellona-gestao.sqlite}
 *       na pasta do módulo;</li>
 *   <li>{@code MELLONA_EXEMPLO=1}: grava os dados de demonstração se a base estiver vazia.</li>
 * </ul>
 */
public class App extends Application {

    private Database database;

    @Override
    public void start(Stage stage) throws IOException {
        database = new Database(configuracao("MELLONA_DB", "mellona-gestao.sqlite"));
        Repositorios repos = new Repositorios(database);
        repos.garantirCategorias();
        if ("1".equals(configuracao("MELLONA_EXEMPLO", "0"))) {
            DadosDeExemplo.carregarSeVazia(repos);
        }

        FXMLLoader loader = new FXMLLoader(App.class.getResource("view/apiarios.fxml"));
        loader.setControllerFactory(tipo -> new ApiarioController(repos));
        Scene scene = new Scene(loader.load(), 1100, 640);
        scene.getStylesheets().add(App.class.getResource("view/mellona.css").toExternalForm());

        stage.setTitle("Mellona — Gestão de apiários");
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (database != null) {
            database.close();
        }
    }

    /** Lê uma variável de ambiente ou, na falta dela, a propriedade de sistema de mesmo nome. */
    private static String configuracao(String nome, String padrao) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) {
            valor = System.getProperty(nome, padrao);
        }
        return valor;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
