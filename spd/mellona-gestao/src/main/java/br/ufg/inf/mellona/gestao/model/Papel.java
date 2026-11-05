package br.ufg.inf.mellona.gestao.model;

/** Papel de um usuário em um apiário (ERS, 13.3; personas Antônio e Carla). */
public enum Papel {
    /** Dono ou responsável pelas colmeias do apiário. */
    APICULTOR,
    /** Cadastra apiários, dispositivos, usuários e parâmetros. */
    ADMINISTRADOR,
    /** Técnico de cooperativa que acompanha vários apiários. */
    TECNICO
}
