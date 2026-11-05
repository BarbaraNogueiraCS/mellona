package br.ufg.inf.mellona.gestao.controller;

import java.util.Optional;

import br.ufg.inf.mellona.gestao.model.Apiario;
import br.ufg.inf.mellona.gestao.model.CadastroApiarios;
import br.ufg.inf.mellona.gestao.model.CadastroColmeias;
import br.ufg.inf.mellona.gestao.model.PersistenciaException;
import br.ufg.inf.mellona.gestao.model.RegraDeNegocioException;
import br.ufg.inf.mellona.gestao.model.Repositorios;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Controlador da tela de apiários (padrão MVC): liga os componentes de {@code apiarios.fxml}
 * aos casos de uso de {@link CadastroApiarios}.
 *
 * <p>Os textos seguem o guia de estilo de IHC: botões com verbo e objeto, confirmação com a
 * consequência antes de inativar e mensagens de erro que dizem como corrigir.</p>
 */
public class ApiarioController {

    private final CadastroApiarios cadastro;
    private final CadastroColmeias colmeias;

    @FXML private TableView<Apiario> tabela;
    @FXML private TableColumn<Apiario, String> nomeCol;
    @FXML private TableColumn<Apiario, String> colmeiasCol;
    @FXML private TableColumn<Apiario, String> protecaoCol;
    @FXML private TableColumn<Apiario, String> fusoCol;
    @FXML private TextField nomeField;
    @FXML private TextField localField;
    @FXML private TextField fusoField;
    @FXML private Label mensagem;

    private Apiario selecionado;

    /** @param repos repositórios da base aberta pela aplicação */
    public ApiarioController(Repositorios repos) {
        this.cadastro = new CadastroApiarios(repos);
        this.colmeias = new CadastroColmeias(repos);
    }

    @FXML
    private void initialize() {
        nomeCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNome()));
        colmeiasCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                String.valueOf(colmeias.colmeiasAtivasDe(c.getValue()).size())));
        protecaoCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                cadastro.configuracaoDe(c.getValue()).isArmada() ? "Ligada" : "Desligada"));
        fusoCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFusoHorario()));
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, agora) -> mostrar(agora));
        recarregar();
        novo();
    }

    /** "Novo apiário": limpa o formulário para um cadastro. */
    @FXML
    private void novo() {
        tabela.getSelectionModel().clearSelection();
        selecionado = null;
        nomeField.clear();
        localField.clear();
        fusoField.setText(Apiario.FUSO_PADRAO);
        mensagem.setText("");
        nomeField.requestFocus();
    }

    /** "Salvar apiário": cria ou altera, conforme haja um apiário selecionado. */
    @FXML
    private void salvar() {
        executar(() -> {
            Apiario apiario;
            if (selecionado == null) {
                apiario = cadastro.criar(nomeField.getText(), localField.getText(), fusoField.getText());
                informar(apiario.getNome() + " cadastrado. A proteção começa desligada.");
            } else {
                apiario = selecionado;
                apiario.setNome(nomeField.getText());
                apiario.setDescricaoLocal(localField.getText());
                apiario.setFusoHorario(fusoField.getText());
                cadastro.salvar(apiario);
                informar("Alterações do " + apiario.getNome() + " salvas.");
            }
            recarregar();
            tabela.getSelectionModel().select(apiario);
        });
    }

    /** "Inativar apiário": pede confirmação com a consequência, como manda o guia de estilo. */
    @FXML
    private void inativar() {
        if (selecionado == null) {
            avisar("Selecione na lista o apiário que deseja inativar.");
            return;
        }
        ButtonType confirmar = new ButtonType("Inativar apiário", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "O apiário sai das listas, mas o histórico de visitas e medições é mantido.", cancelar, confirmar);
        alerta.setHeaderText("Inativar o " + selecionado.getNome() + "?");
        Optional<ButtonType> resposta = alerta.showAndWait();
        if (resposta.isPresent() && resposta.get() == confirmar) {
            executar(() -> {
                String nome = selecionado.getNome();
                cadastro.inativar(selecionado);
                recarregar();
                novo();
                informar(nome + " inativado.");
            });
        }
    }

    private void mostrar(Apiario apiario) {
        selecionado = apiario;
        if (apiario != null) {
            nomeField.setText(apiario.getNome());
            localField.setText(apiario.getDescricaoLocal() == null ? "" : apiario.getDescricaoLocal());
            fusoField.setText(apiario.getFusoHorario());
            mensagem.setText("");
        }
    }

    private void recarregar() {
        tabela.setItems(FXCollections.observableArrayList(cadastro.listarAtivos()));
    }

    private void executar(Runnable acao) {
        try {
            acao.run();
        } catch (RegraDeNegocioException e) {
            avisar(e.getMessage());
        } catch (PersistenciaException e) {
            avisar("Não foi possível gravar na base. " + e.getMessage());
        }
    }

    private void informar(String texto) {
        mensagem.getStyleClass().remove("erro");
        mensagem.setText(texto);
    }

    private void avisar(String texto) {
        if (!mensagem.getStyleClass().contains("erro")) {
            mensagem.getStyleClass().add("erro");
        }
        mensagem.setText(texto);
    }
}
