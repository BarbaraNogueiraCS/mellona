package br.ufg.inf.mellona.gestao.model;

/**
 * Falha técnica de acesso à base (arquivo indisponível, SQL rejeitado, violação de restrição).
 *
 * <p>É não verificada para não obrigar as camadas de visão e controle a tratar
 * {@link java.sql.SQLException}; a causa original fica preservada.</p>
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
