package br.ufg.inf.mellona.gestao.model;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Parâmetros de segurança de um apiário e o estado da proteção (ERS, 13.3, HU-37, PAR-10 a PAR-14).
 *
 * <p>É a relação 1:1 do modelo: a chave estrangeira {@code apiario_id} é única, então a base
 * recusa uma segunda configuração para o mesmo apiário. A configuração é aplicada pela central
 * do apiário, que decide a sinalização sem internet (SSU, Atividade 01); por isso cada
 * alteração incrementa a {@link #getVersao() versão}, enviada no comando {@code comando.configurar}.</p>
 */
@DatabaseTable(tableName = "configuracao_seguranca")
public class ConfiguracaoSeguranca extends EntidadeBase {

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false, unique = true)
    private Apiario apiario;

    /** PAR-14: início do período diurno, no horário local do apiário. */
    @DatabaseField(canBeNull = false)
    private String inicioDiurno = "06:00";

    /** PAR-14: início do período noturno. */
    @DatabaseField(canBeNull = false)
    private String inicioNoturno = "18:00";

    /** PAR-10: intervalo de correlação entre presença e colmeia mexida, em segundos. */
    @DatabaseField(canBeNull = false)
    private int intervaloCorrelacaoS = 60;

    /** PAR-11: duração da sinalização, em segundos. */
    @DatabaseField(canBeNull = false)
    private int duracaoSinalizacaoS = 30;

    /** PAR-12: intervalo de proteção contra repetição, em segundos. */
    @DatabaseField(canBeNull = false)
    private int intervaloProtecaoS = 120;

    /** PAR-13: duração padrão do modo visita, em minutos. */
    @DatabaseField(canBeNull = false)
    private int duracaoAutorizacaoMin = 60;

    /** RN-70: proteção ligada ou desligada. Nasce desligada, até os equipamentos serem instalados. */
    @DatabaseField(canBeNull = false)
    private boolean armada = false;

    @DatabaseField(canBeNull = false)
    private int versao = 1;

    /** Construtor exigido pelo ORMLite. */
    ConfiguracaoSeguranca() {
    }

    /**
     * Configuração com os valores iniciais do ERS (seção 12).
     *
     * @param apiario apiário configurado
     */
    public ConfiguracaoSeguranca(Apiario apiario) {
        this.apiario = Validacao.obrigatorio(apiario, "o apiário da configuração", "RN-60");
    }

    public Apiario getApiario() {
        return apiario;
    }

    public LocalTime getInicioDiurno() {
        return LocalTime.parse(inicioDiurno);
    }

    public LocalTime getInicioNoturno() {
        return LocalTime.parse(inicioNoturno);
    }

    /**
     * Define os dois períodos do dia (PAR-14: qualquer par de horários válidos e diferentes).
     *
     * @param inicioDiurno  horário no formato HH:mm
     * @param inicioNoturno horário no formato HH:mm
     */
    public void definirPeriodos(String inicioDiurno, String inicioNoturno) {
        LocalTime dia = horario(inicioDiurno, "o início do período diurno");
        LocalTime noite = horario(inicioNoturno, "o início do período noturno");
        if (dia.equals(noite)) {
            throw new RegraDeNegocioException("PAR-14", "O dia e a noite precisam começar em horários diferentes.");
        }
        this.inicioDiurno = dia.toString();
        this.inicioNoturno = noite.toString();
    }

    public int getIntervaloCorrelacaoS() {
        return intervaloCorrelacaoS;
    }

    public void setIntervaloCorrelacaoS(int segundos) {
        this.intervaloCorrelacaoS = Validacao.faixa(segundos, 10, 300, "O intervalo de correlação (s)", "PAR-10");
    }

    public int getDuracaoSinalizacaoS() {
        return duracaoSinalizacaoS;
    }

    public void setDuracaoSinalizacaoS(int segundos) {
        this.duracaoSinalizacaoS = Validacao.faixa(segundos, 10, 120, "A duração da sinalização (s)", "PAR-11");
    }

    public int getIntervaloProtecaoS() {
        return intervaloProtecaoS;
    }

    public void setIntervaloProtecaoS(int segundos) {
        this.intervaloProtecaoS = Validacao.faixa(segundos, 30, 600, "O intervalo de proteção (s)", "PAR-12");
    }

    public int getDuracaoAutorizacaoMin() {
        return duracaoAutorizacaoMin;
    }

    public void setDuracaoAutorizacaoMin(int minutos) {
        this.duracaoAutorizacaoMin = Validacao.faixa(minutos, 10, 240, "A duração do modo visita (min)", "PAR-13");
    }

    /** @return {@code true} se a proteção está ligada (RN-70) */
    public boolean isArmada() {
        return armada;
    }

    public void setArmada(boolean armada) {
        this.armada = armada;
    }

    /** @return versão da configuração, enviada à central do apiário */
    public int getVersao() {
        return versao;
    }

    /** Chamado por {@link CadastroApiarios} a cada alteração gravada. */
    void incrementarVersao() {
        versao++;
    }

    private static LocalTime horario(String valor, String campo) {
        String texto = Validacao.obrigatorio(valor, campo);
        try {
            return LocalTime.parse(texto);
        } catch (DateTimeParseException e) {
            throw new RegraDeNegocioException("PAR-14", "Informe " + campo + " no formato HH:mm, como 06:00.");
        }
    }
}
