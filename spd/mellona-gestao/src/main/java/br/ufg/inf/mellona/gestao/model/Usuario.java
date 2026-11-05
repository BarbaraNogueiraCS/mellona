package br.ufg.inf.mellona.gestao.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Pessoa com acesso ao sistema (ERS, 13.1). O papel não fica aqui: ele depende do apiário e é
 * registrado em {@link AcessoApiario}, a associação N:N entre usuários e apiários.
 */
@DatabaseTable(tableName = "usuario")
public class Usuario extends EntidadeBase {

    @DatabaseField(canBeNull = false)
    private String nome;

    @DatabaseField(canBeNull = false, unique = true)
    private String email;

    @DatabaseField(canBeNull = false)
    private boolean ativo = true;

    /** Construtor exigido pelo ORMLite. */
    Usuario() {
    }

    /**
     * @param nome  nome completo
     * @param email e-mail de acesso, único no sistema
     */
    public Usuario(String nome, String email) {
        setNome(nome);
        setEmail(email);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Validacao.obrigatorio(nome, "o nome do usuário");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String valor = Validacao.obrigatorio(email, "o e-mail do usuário").toLowerCase();
        if (!valor.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new RegraDeNegocioException("RNF-26", "O e-mail \"" + email + "\" não é válido. Confira o endereço digitado.");
        }
        this.email = valor;
    }

    public boolean isAtivo() {
        return ativo;
    }

    /** Inativa o usuário sem apagar seu histórico de visitas e inspeções (RN-04). */
    public void inativar() {
        this.ativo = false;
    }

    @Override
    public String toString() {
        return nome;
    }
}
