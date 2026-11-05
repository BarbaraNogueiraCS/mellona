package br.ufg.inf.mellona.gestao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Relação N:N com atributos entre Usuário e Apiário (HU-02, RNF-16). */
class AcessoApiarioTest extends BaseDeTeste {

    @Test
    @DisplayName("N:N — Carla acompanha dois apiários, e cada apiário tem mais de um usuário")
    void navegacaoNosDoisSentidos() {
        Usuario antonio = acessos.cadastrarUsuario("Antônio Souza", "antonio@mellona.app");
        Usuario carla = acessos.cadastrarUsuario("Carla Mendes", "carla@coop.org");
        Apiario serra = apiarios.criar("Apiário Serra", null);
        Apiario barreiro = apiarios.criar("Apiário Barreiro", null);

        acessos.conceder(antonio, serra, Papel.APICULTOR, T0);
        acessos.conceder(antonio, barreiro, Papel.APICULTOR, T0);
        acessos.conceder(carla, serra, Papel.TECNICO, T0);
        acessos.conceder(carla, barreiro, Papel.TECNICO, T0);

        Set<String> apiariosDaCarla = acessos.apiariosDe(carla).stream().map(Apiario::getNome).collect(Collectors.toSet());
        assertEquals(Set.of("Apiário Serra", "Apiário Barreiro"), apiariosDaCarla);
        Set<String> usuariosDaSerra = acessos.usuariosDe(serra).stream().map(Usuario::getNome).collect(Collectors.toSet());
        assertEquals(Set.of("Antônio Souza", "Carla Mendes"), usuariosDaSerra);
        assertEquals(Papel.TECNICO, acessos.acessoAtivo(carla, serra).orElseThrow().getPapel());
    }

    @Test
    @DisplayName("HU-02 — não há dois acessos ativos do mesmo usuário ao mesmo apiário")
    void semAcessoDuplicado() {
        Usuario carla = acessos.cadastrarUsuario("Carla Mendes", "carla@coop.org");
        Apiario serra = apiarios.criar("Apiário Serra", null);
        acessos.conceder(carla, serra, Papel.TECNICO, T0);

        recusadaPor("HU-02", () -> acessos.conceder(carla, serra, Papel.ADMINISTRADOR, T0));
    }

    @Test
    @DisplayName("revogar encerra o acesso sem apagá-lo; um novo acesso pode ser concedido depois")
    void revogarPreservaHistorico() {
        Usuario carla = acessos.cadastrarUsuario("Carla Mendes", "carla@coop.org");
        Apiario serra = apiarios.criar("Apiário Serra", null);
        AcessoApiario primeiro = acessos.conceder(carla, serra, Papel.TECNICO, T0);

        acessos.revogar(primeiro, T0.plusSeconds(86_400));
        assertTrue(acessos.apiariosDe(carla).isEmpty());

        acessos.conceder(carla, serra, Papel.ADMINISTRADOR, T0.plusSeconds(172_800));
        assertEquals(List.of(serra), acessos.apiariosDe(carla));
        assertEquals(2, repos.acessos.count(), "o acesso revogado continua registrado");
    }

    @Test
    @DisplayName("e-mail de usuário é único e válido")
    void emailUnicoEValido() {
        acessos.cadastrarUsuario("Antônio Souza", "antonio@mellona.app");
        recusadaPor("HU-02", () -> acessos.cadastrarUsuario("Outro Antônio", "ANTONIO@mellona.app"));
        recusadaPor("RNF-26", () -> acessos.cadastrarUsuario("Sem Arroba", "antonio.mellona.app"));
    }
}
