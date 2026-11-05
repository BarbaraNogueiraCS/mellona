package br.ufg.inf.mellona.gestao.model;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de equipamentos e de suas associações (HU-05), com a regra RN-03:
 * dispositivos não podem ter associações ativas conflitantes.
 *
 * <p>São conflitos, nesta versão:</p>
 * <ol>
 *   <li>o equipamento já ter uma associação ativa (precisa ser encerrada antes);</li>
 *   <li>a colmeia já ter outro sensor de colmeia ativo;</li>
 *   <li>o apiário já ter outra central ativa.</li>
 * </ol>
 */
public class CadastroDispositivos {

    private final Repositorios repos;

    public CadastroDispositivos(Repositorios repos) {
        this.repos = repos;
    }

    /**
     * @param identificador identificador único (deviceId dos eventos)
     * @param tipo          tipo do equipamento
     * @return equipamento gravado
     */
    public Dispositivo cadastrar(String identificador, TipoDispositivo tipo) {
        Dispositivo dispositivo = new Dispositivo(identificador, tipo);
        if (!repos.dispositivos.loadWhere("identificador", dispositivo.getIdentificador()).isEmpty()) {
            throw new RegraDeNegocioException("RN-03", "Já existe um equipamento com o identificador "
                    + dispositivo.getIdentificador() + ".");
        }
        return repos.dispositivos.create(dispositivo);
    }

    /**
     * Associa o equipamento a um apiário e, se for sensor de colmeia, a uma colmeia desse apiário.
     *
     * @param dispositivo equipamento ativo e sem associação ativa
     * @param apiario     apiário ativo
     * @param colmeia     colmeia ativa do apiário, obrigatória só para {@link TipoDispositivo#NO_COLMEIA}
     * @param inicio      instante de início do vínculo
     * @return associação gravada
     * @throws RegraDeNegocioException RN-03 em caso de conflito
     */
    public AssociacaoDispositivo associar(Dispositivo dispositivo, Apiario apiario, Colmeia colmeia, Instant inicio) {
        Validacao.obrigatorio(dispositivo, "o equipamento", "RN-03");
        Validacao.obrigatorio(apiario, "o apiário", "RN-03");
        Validacao.obrigatorio(inicio, "o início da associação", "RN-03");
        if (!dispositivo.isAtivo()) {
            throw new RegraDeNegocioException("RN-03", "O equipamento " + dispositivo + " está inativo.");
        }
        if (!apiario.isAtivo()) {
            throw new RegraDeNegocioException("RN-02", "O " + apiario.getNome() + " está inativo.");
        }
        conferirLocal(dispositivo, apiario, colmeia);

        associacaoAtiva(dispositivo).ifPresent(atual -> {
            throw new RegraDeNegocioException("RN-03", "O equipamento " + dispositivo + " já está associado ao "
                    + atual.getApiario().getNome() + ". Encerre essa associação antes de criar outra.");
        });
        if (dispositivo.getTipo() == TipoDispositivo.NO_COLMEIA && !ativasNa("colmeia_id", colmeia, TipoDispositivo.NO_COLMEIA).isEmpty()) {
            throw new RegraDeNegocioException("RN-03", "A " + colmeia + " já tem um sensor de colmeia ativo.");
        }
        if (dispositivo.getTipo() == TipoDispositivo.SEGURANCA_LOCAL && !ativasNa("apiario_id", apiario, TipoDispositivo.SEGURANCA_LOCAL).isEmpty()) {
            throw new RegraDeNegocioException("RN-03", "O " + apiario.getNome() + " já tem uma central ativa.");
        }
        return repos.associacoes.create(new AssociacaoDispositivo(dispositivo, apiario, colmeia, inicio));
    }

    /**
     * Encerra a associação ativa do equipamento, se houver. O registro fica no histórico (RN-04).
     *
     * @param dispositivo equipamento
     * @param fim         instante do encerramento
     */
    public void encerrarAssociacao(Dispositivo dispositivo, Instant fim) {
        associacaoAtiva(dispositivo).ifPresent(associacao -> {
            associacao.encerrar(fim);
            repos.associacoes.update(associacao);
        });
    }

    /**
     * Inativa o equipamento e encerra sua associação ativa (RN-04).
     *
     * @param dispositivo equipamento
     * @param agora       instante
     */
    public void inativar(Dispositivo dispositivo, Instant agora) {
        repos.dispositivos.emTransacao(() -> {
            encerrarAssociacao(dispositivo, agora);
            dispositivo.setAtivo(false);
            repos.dispositivos.update(dispositivo);
            return null;
        });
    }

    /**
     * @param dispositivo equipamento
     * @return a associação ativa, se houver
     */
    public Optional<AssociacaoDispositivo> associacaoAtiva(Dispositivo dispositivo) {
        return repos.associacoes.loadWhere("dispositivo_id", dispositivo).stream()
                .filter(AssociacaoDispositivo::isAtiva).findFirst();
    }

    /**
     * Um lado da relação N:N: as colmeias pelas quais o equipamento passou.
     *
     * @param dispositivo equipamento
     * @return associações, da mais antiga para a mais recente
     */
    public List<AssociacaoDispositivo> historico(Dispositivo dispositivo) {
        return repos.associacoes.loadWhere("dispositivo_id", dispositivo).stream()
                .sorted(Comparator.comparing(AssociacaoDispositivo::getInicio)).toList();
    }

    /**
     * O outro lado da relação N:N: os equipamentos ativos em uma colmeia.
     *
     * @param colmeia colmeia
     * @return equipamentos com associação ativa na colmeia
     */
    public List<Dispositivo> dispositivosAtivosNa(Colmeia colmeia) {
        return repos.associacoes.loadWhere("colmeia_id", colmeia).stream()
                .filter(AssociacaoDispositivo::isAtiva).map(AssociacaoDispositivo::getDispositivo).toList();
    }

    private void conferirLocal(Dispositivo dispositivo, Apiario apiario, Colmeia colmeia) {
        if (dispositivo.getTipo().ficaNaColmeia()) {
            if (colmeia == null) {
                throw new RegraDeNegocioException("RN-03", "Escolha a colmeia onde o sensor " + dispositivo + " está instalado.");
            }
            if (!colmeia.isAtiva()) {
                throw new RegraDeNegocioException("RN-03", "A " + colmeia + " está inativa.");
            }
            if (colmeia.getApiario().getId() != apiario.getId()) {
                throw new RegraDeNegocioException("RN-03", "A " + colmeia + " fica no " + colmeia.getApiario().getNome()
                        + ", e não no " + apiario.getNome() + ".");
            }
        } else if (colmeia != null) {
            throw new RegraDeNegocioException("RN-03", "O equipamento " + dispositivo
                    + " fica na área do apiário e não é associado a uma colmeia.");
        }
    }

    private List<AssociacaoDispositivo> ativasNa(String coluna, Object alvo, TipoDispositivo tipo) {
        try {
            return repos.associacoes.getDao().queryBuilder().where().eq(coluna, alvo).and().isNull("fim").query()
                    .stream().filter(a -> a.getDispositivo().getTipo() == tipo).toList();
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao consultar associações", e);
        }
    }
}
