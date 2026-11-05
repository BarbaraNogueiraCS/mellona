package br.ufg.inf.mellona.gestao.model;

/** Verificações de preenchimento usadas pelas entidades. */
final class Validacao {

    private Validacao() {
    }

    /**
     * @param valor texto informado
     * @param campo nome do campo na mensagem, com artigo ("o nome do apiário")
     * @return o texto sem espaços nas pontas
     * @throws RegraDeNegocioException se o texto estiver vazio
     */
    static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException("RNF-26", "Falta informar " + campo + ".");
        }
        return valor.strip();
    }

    /**
     * @param valor objeto informado
     * @param campo nome do campo na mensagem, com artigo
     * @param regra regra do ERS que exige o campo
     * @param <T>   tipo do objeto
     * @return o próprio objeto
     * @throws RegraDeNegocioException se o objeto for nulo
     */
    static <T> T obrigatorio(T valor, String campo, String regra) {
        if (valor == null) {
            throw new RegraDeNegocioException(regra, "Falta informar " + campo + ".");
        }
        return valor;
    }

    /**
     * @param valor  número informado
     * @param minimo menor valor aceito
     * @param maximo maior valor aceito
     * @param campo  nome do parâmetro na mensagem
     * @param regra  parâmetro do ERS que define a faixa
     * @return o próprio número
     * @throws RegraDeNegocioException se estiver fora da faixa
     */
    static int faixa(int valor, int minimo, int maximo, String campo, String regra) {
        if (valor < minimo || valor > maximo) {
            throw new RegraDeNegocioException(regra,
                    campo + " deve ficar entre " + minimo + " e " + maximo + "; foi informado " + valor + ".");
        }
        return valor;
    }
}
