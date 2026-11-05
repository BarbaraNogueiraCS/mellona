package br.ufg.inf.mellona.gestao.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de usuários e de seus acessos a apiários (HU-02, RNF-16).
 */
public class CadastroAcessos {

    private final Repositorios repos;

    public CadastroAcessos(Repositorios repos) {
        this.repos = repos;
    }

    /**
     * @param nome  nome completo
     * @param email e-mail único
     * @return usuário gravado
     */
    public Usuario cadastrarUsuario(String nome, String email) {
        Usuario usuario = new Usuario(nome, email);
        if (!repos.usuarios.loadWhere("email", usuario.getEmail()).isEmpty()) {
            throw new RegraDeNegocioException("HU-02", "Já existe um usuário com o e-mail " + usuario.getEmail() + ".");
        }
        return repos.usuarios.create(usuario);
    }

    /**
     * Dá ao usuário acesso a um apiário, com um papel.
     *
     * @param usuario usuário ativo
     * @param apiario apiário ativo
     * @param papel   papel no apiário
     * @param inicio  início do acesso
     * @return acesso gravado
     * @throws RegraDeNegocioException se já houver acesso ativo do usuário ao apiário
     */
    public AcessoApiario conceder(Usuario usuario, Apiario apiario, Papel papel, Instant inicio) {
        Validacao.obrigatorio(usuario, "o usuário", "HU-02");
        Validacao.obrigatorio(apiario, "o apiário", "HU-02");
        Validacao.obrigatorio(papel, "o papel", "HU-02");
        if (acessoAtivo(usuario, apiario).isPresent()) {
            throw new RegraDeNegocioException("HU-02", usuario.getNome() + " já tem acesso ao " + apiario.getNome()
                    + ". Para mudar o papel, revogue o acesso atual antes.");
        }
        return repos.acessos.create(new AcessoApiario(usuario, apiario, papel, inicio));
    }

    /**
     * Encerra o acesso; o registro permanece para auditoria (RNF-15).
     *
     * @param acesso acesso ativo
     * @param fim    instante do encerramento
     */
    public void revogar(AcessoApiario acesso, Instant fim) {
        acesso.encerrar(fim);
        repos.acessos.update(acesso);
    }

    /**
     * @param usuario usuário
     * @param apiario apiário
     * @return o acesso ativo do usuário ao apiário, se houver
     */
    public Optional<AcessoApiario> acessoAtivo(Usuario usuario, Apiario apiario) {
        return repos.acessos.loadWhere("usuario_id", usuario).stream()
                .filter(a -> a.isAtivo() && a.getApiario().getId() == apiario.getId())
                .findFirst();
    }

    /**
     * Um lado da relação N:N.
     *
     * @param usuario usuário
     * @return apiários aos quais o usuário tem acesso ativo
     */
    public List<Apiario> apiariosDe(Usuario usuario) {
        return repos.acessos.loadWhere("usuario_id", usuario).stream()
                .filter(AcessoApiario::isAtivo).map(AcessoApiario::getApiario).toList();
    }

    /**
     * O outro lado da relação N:N.
     *
     * @param apiario apiário
     * @return usuários com acesso ativo ao apiário
     */
    public List<Usuario> usuariosDe(Apiario apiario) {
        return repos.acessos.loadWhere("apiario_id", apiario).stream()
                .filter(AcessoApiario::isAtivo).map(AcessoApiario::getUsuario).toList();
    }
}
