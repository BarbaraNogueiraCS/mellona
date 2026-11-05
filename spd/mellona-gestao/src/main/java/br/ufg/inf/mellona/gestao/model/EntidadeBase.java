package br.ufg.inf.mellona.gestao.model;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

/**
 * Campos comuns a todas as entidades da gestão.
 *
 * <ul>
 *   <li>{@code id}: chave interna do SQLite, sequencial. Vale só nesta base.</li>
 *   <li>{@code uuid}: identificador universal gerado na criação, imutável e igual em todas as
 *       bases em que o registro existir (ERS, RN-73). É ele que o integrador usa para
 *       correlacionar o mesmo registro no SQLite e no MongoDB.</li>
 *   <li>{@code criadoEm} e {@code atualizadoEm}: instantes mantidos pelo {@link Repositorio};
 *       o segundo decide conflitos entre bases pela alteração mais recente (RN-75).</li>
 * </ul>
 */
public abstract class EntidadeBase {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, unique = true, dataType = DataType.UUID)
    private UUID uuid = UUID.randomUUID();

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date criadoEm;

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date atualizadoEm;

    /** Construtor exigido pelo ORMLite. */
    protected EntidadeBase() {
    }

    /** @return chave interna desta base; 0 enquanto o objeto não foi gravado */
    public int getId() {
        return id;
    }

    /** @return identificador universal do registro (RN-73); não tem setter */
    public UUID getUuid() {
        return uuid;
    }

    /** @return instante da criação, ou {@code null} antes da primeira gravação */
    public Instant getCriadoEm() {
        return criadoEm == null ? null : criadoEm.toInstant();
    }

    /** @return instante da última alteração, ou {@code null} antes da primeira gravação */
    public Instant getAtualizadoEm() {
        return atualizadoEm == null ? null : atualizadoEm.toInstant();
    }

    /** @return {@code true} se o objeto já foi gravado nesta base */
    public boolean isPersistida() {
        return id != 0;
    }

    /**
     * Registra uma alteração. Chamado pelo {@link Repositorio} antes de cada gravação.
     *
     * @param agora instante da gravação
     */
    void marcarAlteracao(Instant agora) {
        Date instante = Date.from(agora);
        if (criadoEm == null) {
            criadoEm = instante;
        }
        atualizadoEm = instante;
    }

    /** Duas entidades são iguais quando têm o mesmo identificador universal. */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (outro == null || getClass() != outro.getClass()) {
            return false;
        }
        return Objects.equals(uuid, ((EntidadeBase) outro).uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}
