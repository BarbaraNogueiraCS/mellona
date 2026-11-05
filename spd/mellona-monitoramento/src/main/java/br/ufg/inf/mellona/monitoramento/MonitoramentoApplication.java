package br.ufg.inf.mellona.monitoramento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API de monitoramento do Mellona (I3). Recebe e consulta medições e ocorrências, e é o
 * "serviço remoto" da arquitetura de SSU: a central do apiário envia a ele os eventos
 * guardados, e o aplicativo de acompanhamento (protótipo de IHC) o consulta.
 *
 * <p>Execute com {@code mvn -pl mellona-monitoramento spring-boot:run}, com o MongoDB no ar.</p>
 */
@SpringBootApplication
public class MonitoramentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoramentoApplication.class, args);
    }
}
