package br.ufg.inf.mellona.gestao.model;

/**
 * Resultado da procura pela rainha numa inspeção. As três opções são as do seletor da tela
 * "Registrar revisão" no protótipo de IHC (guia de estilo, 2.5).
 */
public enum SituacaoRainha {
    /** A rainha foi vista. Na interface: "Vista". */
    VISTA,
    /** Não foi vista, mas há ovos ou larvas novas. Na interface: "Sinais de rainha". */
    SINAIS,
    /** Nem a rainha nem sinais dela. Na interface: "Não encontrada". */
    NAO_ENCONTRADA
}
