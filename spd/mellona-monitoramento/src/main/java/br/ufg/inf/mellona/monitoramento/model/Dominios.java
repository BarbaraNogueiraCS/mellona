package br.ufg.inf.mellona.monitoramento.model;

/**
 * Domínios de valores do monitoramento (ERS, 13.3). Reunidos numa classe para facilitar a
 * leitura; cada um é um enum independente, gravado no MongoDB pelo nome.
 */
public final class Dominios {

    private Dominios() {
    }

    /** Tipos de ocorrência (alerta). */
    public enum TipoOcorrencia {
        MOVIMENTACAO_ANORMAL, TOMBAMENTO_POSSIVEL, PRESENCA, OCORRENCIA_SUSPEITA,
        SEM_COMUNICACAO, SEGURANCA_DEGRADADA, HORARIO_INVALIDO
    }

    /** RN-19 e RN-45. */
    public enum Prioridade { BAIXA, MEDIA, ALTA }

    /**
     * RN-21: estados do alerta. Na interface, "Estou ciente" leva a {@code RECONHECIDO} e
     * "Encerrar alerta" leva a {@code RESOLVIDO} (glossário do guia de estilo de IHC).
     */
    public enum EstadoAlerta { ATIVO, RECONHECIDO, RESOLVIDO }

    /** RN-60: período do dia usado na decisão de sinalização. */
    public enum PeriodoDia { DIURNO, NOTURNO }

    /** Recursos de sinalização: sirene e luz. */
    public enum Recurso { SOM, LUZ }

    /** RN-07, RN-66: qualidade de um evento. */
    public enum Qualidade { VALIDO, INVALIDO, DESATUALIZADO, ATRASADO }
}
