package br.ufg.inf.mellona.gestao.model;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de colmeias (HU-04), com as regras RN-01, RN-02 e RN-04.
 */
public class CadastroColmeias {

    private final Repositorios repos;

    public CadastroColmeias(Repositorios repos) {
        this.repos = repos;
    }

    /**
     * Cadastra uma colmeia em um apiário ativo.
     *
     * @param apiario        apiário onde a colmeia fica
     * @param codigo         código único, como A1-07
     * @param dataInstalacao data de instalação, opcional
     * @return colmeia gravada
     * @throws RegraDeNegocioException RN-01 se o código já existir; RN-02 se o apiário estiver inativo
     */
    public Colmeia cadastrar(Apiario apiario, String codigo, LocalDate dataInstalacao) {
        exigirApiarioAtivo(apiario);
        String normalizado = Colmeia.normalizarCodigo(codigo);
        if (buscarPorCodigo(normalizado).isPresent()) {
            throw new RegraDeNegocioException("RN-01", "Já existe uma colmeia com o código " + normalizado
                    + ". Cada caixa precisa de um código próprio.");
        }
        return repos.colmeias.create(new Colmeia(normalizado, apiario, dataInstalacao));
    }

    /**
     * Muda a colmeia de apiário. Como ela tem uma única chave de apiário, deixa de pertencer
     * ao anterior no mesmo instante (RN-02).
     *
     * @param colmeia colmeia ativa
     * @param destino apiário ativo de destino
     * @return colmeia gravada
     */
    public Colmeia mover(Colmeia colmeia, Apiario destino) {
        exigirApiarioAtivo(destino);
        if (!colmeia.isAtiva()) {
            throw new RegraDeNegocioException("RN-02", "A " + colmeia + " está inativa e não pode ser movida.");
        }
        colmeia.setApiario(destino);
        return repos.colmeias.update(colmeia);
    }

    /**
     * Inativa a colmeia e encerra as associações de equipamentos ativas nela. Nada é apagado:
     * inspeções, medições e associações antigas continuam no histórico (RN-04).
     *
     * @param colmeia colmeia a inativar
     * @param agora   instante do encerramento das associações
     */
    public void inativar(Colmeia colmeia, Instant agora) {
        repos.colmeias.emTransacao(() -> {
            for (AssociacaoDispositivo associacao : repos.associacoes.loadWhere("colmeia_id", colmeia)) {
                if (associacao.isAtiva()) {
                    associacao.encerrar(agora);
                    repos.associacoes.update(associacao);
                }
            }
            colmeia.setAtiva(false);
            repos.colmeias.update(colmeia);
            return null;
        });
    }

    /**
     * @param codigo código da colmeia, em qualquer caixa
     * @return a colmeia, se existir
     */
    public Optional<Colmeia> buscarPorCodigo(String codigo) {
        return repos.colmeias.loadWhere("codigo", Colmeia.normalizarCodigo(codigo)).stream().findFirst();
    }

    /**
     * @param apiario apiário
     * @return colmeias ativas do apiário, por código
     */
    public List<Colmeia> colmeiasAtivasDe(Apiario apiario) {
        try {
            return repos.colmeias.getDao().queryBuilder().orderBy("codigo", true)
                    .where().eq("apiario_id", apiario).and().eq("ativa", true).query();
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao listar colmeias", e);
        }
    }

    private static void exigirApiarioAtivo(Apiario apiario) {
        Validacao.obrigatorio(apiario, "o apiário", "RN-02");
        if (!apiario.isAtivo()) {
            throw new RegraDeNegocioException("RN-02", "O " + apiario.getNome()
                    + " está inativo. Escolha um apiário ativo para a colmeia.");
        }
    }
}
