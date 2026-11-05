package br.ufg.inf.mellona.gestao.model;

import java.time.Instant;

/**
 * Dados de demonstração, baseados nas personas da PF1 de IHC: Antônio, apicultor com cerca de
 * 40 colmeias em dois apiários, e Carla, técnica de cooperativa que acompanha os dois.
 * Usados para apresentar a aplicação; os testes criam os próprios dados.
 */
public final class DadosDeExemplo {

    private DadosDeExemplo() {
    }

    /**
     * Grava os dados de exemplo se a base ainda não tiver apiários.
     *
     * @param repos repositórios da base aberta
     * @return {@code true} se os dados foram gravados agora
     */
    public static boolean carregarSeVazia(Repositorios repos) {
        if (repos.apiarios.count() > 0) {
            return false;
        }
        repos.garantirCategorias();
        CadastroApiarios apiarios = new CadastroApiarios(repos);
        CadastroColmeias colmeias = new CadastroColmeias(repos);
        CadastroAcessos acessos = new CadastroAcessos(repos);
        Instant agora = Instant.now();

        Apiario serra = apiarios.criar("Apiário Serra", "Estrada da Serra, km 12");
        Apiario barreiro = apiarios.criar("Apiário Barreiro", "Sítio Barreiro");
        for (int i = 1; i <= 22; i++) {
            colmeias.cadastrar(serra, String.format("A1-%02d", i), null);
        }
        for (int i = 1; i <= 18; i++) {
            colmeias.cadastrar(barreiro, String.format("B2-%02d", i), null);
        }
        ConfiguracaoSeguranca protecaoSerra = apiarios.configuracaoDe(serra);
        protecaoSerra.setArmada(true);
        apiarios.salvarConfiguracao(protecaoSerra);

        Usuario antonio = acessos.cadastrarUsuario("Antônio Souza", "antonio@mellona.app");
        Usuario carla = acessos.cadastrarUsuario("Carla Mendes", "carla@coop.org");
        acessos.conceder(antonio, serra, Papel.APICULTOR, agora);
        acessos.conceder(antonio, barreiro, Papel.APICULTOR, agora);
        acessos.conceder(carla, serra, Papel.TECNICO, agora);
        acessos.conceder(carla, barreiro, Papel.TECNICO, agora);
        return true;
    }
}
