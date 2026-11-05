package br.ufg.inf.mellona.gestao.model;

import java.sql.SQLException;
import java.util.List;

/**
 * Casos de uso de apiários (HU-03) e de sua configuração de segurança (HU-37).
 */
public class CadastroApiarios {

    private final Repositorios repos;

    public CadastroApiarios(Repositorios repos) {
        this.repos = repos;
    }

    /**
     * Cria o apiário e, na mesma transação, sua configuração de segurança com os valores
     * iniciais do ERS. Assim a relação 1:1 nunca fica pela metade.
     *
     * @param nome           nome do apiário
     * @param descricaoLocal referência de localização, de acesso restrito
     * @return apiário gravado
     */
    public Apiario criar(String nome, String descricaoLocal) {
        return criar(nome, descricaoLocal, Apiario.FUSO_PADRAO);
    }

    /**
     * Como {@link #criar(String, String)}, com o fuso horário do apiário.
     *
     * @param nome           nome do apiário
     * @param descricaoLocal referência de localização, de acesso restrito
     * @param fusoHorario    identificador IANA, como America/Cuiaba
     * @return apiário gravado
     */
    public Apiario criar(String nome, String descricaoLocal, String fusoHorario) {
        Apiario apiario = new Apiario(nome, descricaoLocal);
        apiario.setFusoHorario(fusoHorario);
        return repos.apiarios.emTransacao(() -> {
            repos.apiarios.create(apiario);
            repos.configuracoes.create(new ConfiguracaoSeguranca(apiario));
            return apiario;
        });
    }

    /**
     * @param apiario apiário gravado
     * @return sua configuração de segurança (sempre existe para apiários criados por {@link #criar})
     */
    public ConfiguracaoSeguranca configuracaoDe(Apiario apiario) {
        return repos.configuracoes.loadWhere("apiario_id", apiario).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Apiário sem configuração de segurança: " + apiario.getUuid()));
    }

    /**
     * Grava parâmetros alterados e incrementa a versão enviada à central (comando.configurar).
     *
     * @param configuracao configuração alterada pelos setters, que já validam as faixas
     * @return a configuração gravada
     */
    public ConfiguracaoSeguranca salvarConfiguracao(ConfiguracaoSeguranca configuracao) {
        configuracao.incrementarVersao();
        return repos.configuracoes.update(configuracao);
    }

    /**
     * Grava nome, local e fuso alterados.
     *
     * @param apiario apiário alterado
     * @return o apiário gravado
     */
    public Apiario salvar(Apiario apiario) {
        return repos.apiarios.update(apiario);
    }

    /**
     * Inativa o apiário, preservando histórico (RN-04). É recusado enquanto houver colmeias
     * ativas nele, porque uma colmeia só pode pertencer a um apiário ativo (RN-02).
     *
     * @param apiario apiário a inativar
     */
    public void inativar(Apiario apiario) {
        long ativas = new CadastroColmeias(repos).colmeiasAtivasDe(apiario).size();
        if (ativas > 0) {
            throw new RegraDeNegocioException("RN-02", "O " + apiario.getNome() + " ainda tem " + ativas
                    + " colmeia(s) ativa(s). Mova ou inative as colmeias antes de inativar o apiário.");
        }
        apiario.setAtivo(false);
        repos.apiarios.update(apiario);
    }

    /** @return apiários ativos, em ordem alfabética */
    public List<Apiario> listarAtivos() {
        try {
            return repos.apiarios.getDao().queryBuilder().orderBy("nome", true)
                    .where().eq("ativo", true).query();
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao listar apiários", e);
        }
    }
}
