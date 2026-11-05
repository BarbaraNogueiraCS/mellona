package br.ufg.inf.mellona.gestao.model;

/**
 * Operação recusada por uma regra de negócio do ERS.
 *
 * <p>A mensagem é escrita para o usuário: diz o que impediu a operação e, quando possível,
 * como corrigir (ERS, RNF-26). O código da regra (por exemplo, {@code RN-01}) permite
 * rastrear a recusa até o requisito e é usado nos testes.</p>
 */
public class RegraDeNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String regra;

    /**
     * @param regra    identificador da regra no ERS, como {@code RN-03}
     * @param mensagem texto para o usuário
     */
    public RegraDeNegocioException(String regra, String mensagem) {
        super(mensagem);
        this.regra = regra;
    }

    /** @return identificador da regra no ERS */
    public String getRegra() {
        return regra;
    }
}
