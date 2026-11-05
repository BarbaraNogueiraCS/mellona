package br.ufg.inf.mellona.gestao.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Registros feitos a partir de uma ida ao apiário: visitas (HU-33) e inspeções (HU-19).
 *
 * <p>Só registra quem tem acesso ativo ao apiário (RNF-16), o que usa a relação N:N
 * {@link AcessoApiario}.</p>
 */
public class RegistroDeCampo {

    private final Repositorios repos;
    private final CadastroAcessos acessos;

    public RegistroDeCampo(Repositorios repos) {
        this.repos = repos;
        this.acessos = new CadastroAcessos(repos);
    }

    /**
     * Registra uma visita com motivo classificado.
     *
     * @param apiario     apiário ativo
     * @param responsavel usuário com acesso ao apiário
     * @param motivo      categoria CM ou IB (RN-51)
     * @param data        data e hora da visita
     * @param descricao   descrição; obrigatória quando a categoria exige justificativa (CM-07)
     * @return visita gravada
     */
    public Visita registrarVisita(Apiario apiario, Usuario responsavel, CategoriaAtividade motivo, Instant data, String descricao) {
        Visita visita = new Visita(apiario, responsavel, motivo, data, descricao);
        if (!apiario.isAtivo()) {
            throw new RegraDeNegocioException("RN-52", "O " + apiario.getNome() + " está inativo.");
        }
        exigirAcesso(responsavel, apiario);
        if (motivo.isExigeJustificativa() && visita.getDescricao() == null) {
            throw new RegraDeNegocioException("RN-51", "O motivo " + motivo + " exige uma justificativa na descrição.");
        }
        return repos.visitas.create(visita);
    }

    /**
     * Registra a inspeção de uma colmeia ativa ("Registrar revisão" no protótipo).
     *
     * @param colmeia     colmeia revisada
     * @param responsavel usuário com acesso ao apiário da colmeia
     * @param data        data e hora
     * @param rainha      situação da rainha, opcional
     * @param crias       avaliação das crias, opcional
     * @param observacoes observações, opcional
     * @return inspeção gravada
     */
    public Inspecao registrarInspecao(Colmeia colmeia, Usuario responsavel, Instant data,
                                      SituacaoRainha rainha, String crias, String observacoes) {
        Inspecao inspecao = new Inspecao(colmeia, responsavel, data);
        if (!colmeia.isAtiva()) {
            throw new RegraDeNegocioException("RN-24", "A " + colmeia + " está inativa.");
        }
        exigirAcesso(responsavel, colmeia.getApiario());
        inspecao.setRainha(rainha);
        inspecao.setCrias(crias);
        inspecao.setObservacoes(observacoes);
        return repos.inspecoes.create(inspecao);
    }

    /**
     * @param apiario apiário
     * @return visitas do apiário, da mais recente para a mais antiga (RN-54)
     */
    public List<Visita> visitasDe(Apiario apiario) {
        return repos.visitas.loadWhere("apiario_id", apiario).stream()
                .sorted(Comparator.comparing(Visita::getData).reversed()).toList();
    }

    /**
     * @param colmeia colmeia
     * @return inspeções da colmeia, da mais recente para a mais antiga (relação 1:N)
     */
    public List<Inspecao> inspecoesDe(Colmeia colmeia) {
        return repos.inspecoes.loadWhere("colmeia_id", colmeia).stream()
                .sorted(Comparator.comparing(Inspecao::getData).reversed()).toList();
    }

    private void exigirAcesso(Usuario usuario, Apiario apiario) {
        if (acessos.acessoAtivo(usuario, apiario).isEmpty()) {
            throw new RegraDeNegocioException("RNF-16", usuario.getNome() + " não tem acesso ao " + apiario.getNome() + ".");
        }
    }
}
