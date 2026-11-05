package br.ufg.inf.mellona.gestao.model;

import java.time.Instant;
import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Vínculo histórico de um dispositivo a um apiário e, para o nó da colmeia, a uma colmeia
 * (ERS, 13.1 e 13.2).
 *
 * <p>É a tabela associativa da relação N:N entre {@link Dispositivo} e {@link Colmeia}: ao longo
 * do tempo, um equipamento passa por várias colmeias e uma colmeia recebe vários equipamentos.
 * Os atributos {@code inicio} e {@code fim} guardam o período; enquanto {@code fim} é nulo,
 * a associação está ativa. Associações encerradas nunca são apagadas (RN-04), e as medições
 * antigas continuam atribuíveis à colmeia certa.</p>
 */
@DatabaseTable(tableName = "associacao_dispositivo")
public class AssociacaoDispositivo extends EntidadeBase {

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Dispositivo dispositivo;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Apiario apiario;

    /** Nulo para sensor de presença e central, que ficam na área do apiário. */
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Colmeia colmeia;

    @DatabaseField(canBeNull = false, dataType = DataType.DATE_LONG)
    private Date inicio;

    @DatabaseField(dataType = DataType.DATE_LONG)
    private Date fim;

    /** Construtor exigido pelo ORMLite. */
    AssociacaoDispositivo() {
    }

    /** Use {@link CadastroDispositivos#associar}, que confere RN-03. */
    AssociacaoDispositivo(Dispositivo dispositivo, Apiario apiario, Colmeia colmeia, Instant inicio) {
        this.dispositivo = dispositivo;
        this.apiario = apiario;
        this.colmeia = colmeia;
        this.inicio = Date.from(inicio);
    }

    public Dispositivo getDispositivo() {
        return dispositivo;
    }

    public Apiario getApiario() {
        return apiario;
    }

    /** @return colmeia associada, ou {@code null} para equipamentos da área do apiário */
    public Colmeia getColmeia() {
        return colmeia;
    }

    public Instant getInicio() {
        return inicio.toInstant();
    }

    /** @return fim do vínculo, ou {@code null} se ainda está ativo */
    public Instant getFim() {
        return fim == null ? null : fim.toInstant();
    }

    /** @return {@code true} enquanto o vínculo não foi encerrado */
    public boolean isAtiva() {
        return fim == null;
    }

    /** Usado por {@link CadastroDispositivos#encerrarAssociacao}. */
    void encerrar(Instant instante) {
        if (instante.isBefore(getInicio())) {
            throw new RegraDeNegocioException("RN-03", "O fim da associação não pode ser anterior ao início.");
        }
        this.fim = Date.from(instante);
    }
}
