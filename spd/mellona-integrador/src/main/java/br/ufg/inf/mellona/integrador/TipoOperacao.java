package br.ufg.inf.mellona.integrador;

/**
 * Operações propagadas entre as bases. Não há exclusão física: registros com histórico são
 * inativados, e a inativação é propagada como alteração de estado (ERS, 11.10).
 */
public enum TipoOperacao {
    CRIACAO,
    ALTERACAO,
    INATIVACAO
}
