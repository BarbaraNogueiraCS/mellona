package br.ufg.inf.mellona.gestao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Operações básicas do repositório genérico e identificador universal (RN-73). */
class RepositorioTest extends BaseDeTeste {

    @Test
    @DisplayName("create grava, preenche a chave interna e os instantes de criação e alteração")
    void createPreencheChaveEInstantes() {
        Usuario antonio = new Usuario("Antônio Souza", "antonio@mellona.app");
        assertFalse(antonio.isPersistida());

        repos.usuarios.create(antonio);

        assertTrue(antonio.getId() > 0);
        assertNotNull(antonio.getCriadoEm());
        assertEquals(antonio.getCriadoEm(), antonio.getAtualizadoEm());
    }

    @Test
    @DisplayName("RN-73: o identificador universal nasce com o objeto e volta igual da base")
    void uuidNasceComObjetoESobreviveAGravacao() {
        Apiario serra = apiarios.criar("Apiário Serra", "Estrada da Serra, km 12");
        UUID uuid = serra.getUuid();
        assertNotNull(uuid);

        Apiario lido = repos.apiarios.loadFromId(serra.getId()).orElseThrow();
        assertEquals(uuid, lido.getUuid());
        assertEquals(serra, lido, "entidades com o mesmo uuid são iguais");
        assertEquals(serra.getId(), repos.apiarios.loadFromUuid(uuid).orElseThrow().getId());
    }

    @Test
    @DisplayName("RN-73: cada registro tem um uuid diferente")
    void uuidsSaoDistintos() {
        Apiario a = apiarios.criar("Apiário Serra", null);
        Apiario b = apiarios.criar("Apiário Barreiro", null);
        assertNotEquals(a.getUuid(), b.getUuid());
    }

    @Test
    @DisplayName("update grava a alteração, mantém o uuid e avança o instante de alteração")
    void updateMantemUuid() throws InterruptedException {
        Apiario serra = apiarios.criar("Apiario Serra", null);
        UUID uuid = serra.getUuid();
        Thread.sleep(5);

        serra.setNome("Apiário Serra");
        apiarios.salvar(serra);

        Apiario lido = repos.apiarios.loadFromUuid(uuid).orElseThrow();
        assertEquals("Apiário Serra", lido.getNome());
        assertTrue(lido.getAtualizadoEm().isAfter(lido.getCriadoEm()));
    }

    @Test
    @DisplayName("consultas por chave ou uuid inexistentes devolvem vazio, sem exceção")
    void consultaInexistenteDevolveVazio() {
        assertTrue(repos.apiarios.loadFromId(999).isEmpty());
        assertTrue(repos.apiarios.loadFromUuid(UUID.randomUUID()).isEmpty());
    }
}
