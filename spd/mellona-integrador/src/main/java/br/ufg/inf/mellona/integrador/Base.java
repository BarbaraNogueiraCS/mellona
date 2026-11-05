package br.ufg.inf.mellona.integrador;

/** As duas bases sincronizadas pelo integrador. */
public enum Base {
    /** Base relacional (SQLite, ORMLite) da aplicação de gestão, I1. */
    GESTAO,
    /** Base de documentos (MongoDB, Spring Data) da API de monitoramento, I3. */
    MONITORAMENTO
}
