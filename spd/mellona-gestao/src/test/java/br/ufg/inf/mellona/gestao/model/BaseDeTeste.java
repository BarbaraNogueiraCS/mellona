package br.ufg.inf.mellona.gestao.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prepara, para cada teste, um arquivo SQLite novo numa pasta temporária, com as tabelas criadas
 * e as categorias do Apêndice A gravadas. Os testes não dependem uns dos outros nem de uma base
 * pré-existente.
 */
abstract class BaseDeTeste {

    /** Instante fixo, para que os testes não dependam do relógio. */
    static final Instant T0 = Instant.parse("2026-10-21T10:00:00Z");

    @TempDir
    Path pasta;

    Database database;
    Repositorios repos;
    CadastroApiarios apiarios;
    CadastroColmeias colmeias;
    CadastroDispositivos dispositivos;
    CadastroAcessos acessos;
    RegistroDeCampo campo;

    @BeforeEach
    void abrirBase() {
        database = new Database(pasta.resolve("mellona-teste.sqlite").toString());
        repos = new Repositorios(database);
        repos.garantirCategorias();
        apiarios = new CadastroApiarios(repos);
        colmeias = new CadastroColmeias(repos);
        dispositivos = new CadastroDispositivos(repos);
        acessos = new CadastroAcessos(repos);
        campo = new RegistroDeCampo(repos);
    }

    @AfterEach
    void fecharBase() {
        database.close();
    }

    /** Confere que a operação é recusada pela regra indicada. */
    static RegraDeNegocioException recusadaPor(String regra, Executable operacao) {
        RegraDeNegocioException e = assertThrows(RegraDeNegocioException.class, operacao);
        assertEquals(regra, e.getRegra(), () -> "regra esperada " + regra + ", mensagem: " + e.getMessage());
        return e;
    }
}
