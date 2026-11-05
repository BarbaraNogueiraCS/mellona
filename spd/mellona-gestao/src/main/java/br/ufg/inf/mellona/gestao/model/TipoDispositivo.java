package br.ufg.inf.mellona.gestao.model;

/**
 * Tipos de equipamento do apiário (ERS, 13.3). Os nomes na interface seguem o glossário
 * do guia de estilo de IHC.
 */
public enum TipoDispositivo {
    /** Nó da colmeia: temperatura, umidade, inclinação e movimento. Na interface: "Sensor da colmeia". */
    NO_COLMEIA,
    /** Sensor de presença na área do apiário. */
    SENSOR_PRESENCA,
    /** Componente local de segurança, com sirene, luz e teclado. Na interface: "Central do apiário". */
    SEGURANCA_LOCAL;

    /** @return {@code true} se o equipamento é instalado em uma colmeia, e não na área do apiário */
    public boolean ficaNaColmeia() {
        return this == NO_COLMEIA;
    }
}
